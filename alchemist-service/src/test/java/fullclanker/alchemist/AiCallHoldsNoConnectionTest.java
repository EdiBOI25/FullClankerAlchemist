package fullclanker.alchemist;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** Combines a new pair against a stub ai-service that counts the DB connections in use while it answers. */
@SpringBootTest
@AutoConfigureMockMvc
class AiCallHoldsNoConnectionTest {

	private static final AtomicInteger activeDuringAiCall = new AtomicInteger(-1);
	private static final HttpServer server = startServer();
	private static HikariPoolMXBean pool;

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private DataSource dataSource;

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

	@BeforeEach
	void findPool() throws SQLException {
		pool = dataSource.unwrap(HikariDataSource.class).getHikariPoolMXBean();
	}

	@Test
	void combineHoldsNoDbConnectionDuringTheAiCall() {
		assertThat(mvc.post().uri("/combine")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"a\": \"fire\", \"b\": \"water\"}"))
				.hasStatusOk();
		assertThat(activeDuringAiCall.get()).isZero();
	}

	private static HttpServer startServer() {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
			server.createContext("/generate", AiCallHoldsNoConnectionTest::answer);
			return server;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void answer(HttpExchange exchange) throws IOException {
		activeDuringAiCall.set(pool.getActiveConnections());
		byte[] body = "{\"name\": \"Steam\", \"emoji\": \"💨\", \"tags\": [\"gas\"]}".getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(200, body.length);
		try (OutputStream output = exchange.getResponseBody()) {
			output.write(body);
		}
	}

}
