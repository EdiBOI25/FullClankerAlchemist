package fullclanker.alchemist.config;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class TracingPropagationTest {

	private static final AtomicReference<String> traceParent = new AtomicReference<>();
	private static final HttpServer server = startServer();

	@Autowired
	private MockMvcTester mvc;

	@BeforeAll
	static void start() {
		server.start();
	}

	@AfterAll
	static void stop() {
		server.stop(0);
	}

	@DynamicPropertySource
	static void properties(DynamicPropertyRegistry registry) {
		registry.add("ai-service.url", () -> "http://localhost:" + server.getAddress().getPort());
	}

	@Test
	void combineContinuesTheIncomingTraceToAiService() {
		String traceId = "4bf92f3577b34da6a3ce929d0e0e4736";

		assertThat(mvc.post().uri("/combine")
				.header("traceparent", "00-" + traceId + "-00f067aa0ba902b7-01")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"a\": \"fire\", \"b\": \"water\"}"))
				.hasStatusOk();
		assertThat(traceParent.get()).startsWith("00-" + traceId + "-");
	}

	private static HttpServer startServer() {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
			server.createContext("/generate", TracingPropagationTest::answer);
			return server;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void answer(HttpExchange exchange) throws IOException {
		traceParent.set(exchange.getRequestHeaders().getFirst("traceparent"));
		byte[] body = "{\"name\": \"Steam\", \"emoji\": \"💨\", \"tags\": [\"gas\"]}".getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(200, body.length);
		try (OutputStream output = exchange.getResponseBody()) {
			output.write(body);
		}
	}

}
