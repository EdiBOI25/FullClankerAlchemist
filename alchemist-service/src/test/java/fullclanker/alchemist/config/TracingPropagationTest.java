package fullclanker.alchemist.config;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TracingPropagationTest {

	private static final AtomicReference<String> traceParent = new AtomicReference<>();
	private static final HttpServer server = startServer();

	@Autowired
	private RestClient.Builder restClientBuilder;

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
	void restClientCarriesTraceHeaderToAiService() {
		restClientBuilder.clone()
				.baseUrl("http://localhost:" + server.getAddress().getPort())
				.build()
				.get()
				.uri("/generate")
				.retrieve()
				.toBodilessEntity();

		assertThat(traceParent.get()).isNotBlank();
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
		byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(200, body.length);
		try (OutputStream output = exchange.getResponseBody()) {
			output.write(body);
		}
	}

}
