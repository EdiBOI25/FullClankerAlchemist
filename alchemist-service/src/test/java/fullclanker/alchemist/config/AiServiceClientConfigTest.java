package fullclanker.alchemist.config;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiServiceClientConfigTest {

	private final HttpServer server = startServer();

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	@Test
	void callsGoToTheConfiguredUrl() {
		server.createContext("/generate", exchange -> answer(exchange, "ok"));

		String body = client(HttpClientSettings.defaults(), Duration.ofSeconds(5))
				.get().uri("/generate").retrieve().body(String.class);

		assertThat(body).isEqualTo("ok");
	}

	@ParameterizedTest
	@ValueSource(strings = {"http://localhost:2501", "http://ai-service:2501", "https://ai.example.com", "HTTP://localhost:2501",
			"http://127.0.0.1:2501/", "http://[::1]:2501", "http://ai-service:2501/api"})
	void validUrlIsAccepted(String url) {
		assertThat(clientFor(url)).isNotNull();
	}

	@ParameterizedTest
	@ValueSource(strings = {"localhost:2501", "http://localhost:2501 ", "ftp://ai-service", "http://", "http:localhost:2501",
			"http://my_host:2501", "http://:2501", "http://user@:2501", "/generate", ""})
	void invalidUrlStopsStartup(String url) {
		assertThatThrownBy(() -> clientFor(url))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("AI_SERVICE_URL (ai-service.url) must look like http://localhost:2501, got '" + url + "'");
	}

	@Test
	void slowAnswerTimesOutEvenWhenTheGlobalReadTimeoutIsLonger() {
		server.createContext("/generate", exchange -> {
			sleep(Duration.ofSeconds(2));
			answer(exchange, "too late");
		});
		HttpClientSettings global = HttpClientSettings.defaults().withReadTimeout(Duration.ofSeconds(10));

		assertThatThrownBy(() -> client(global, Duration.ofMillis(200)).get().uri("/generate").retrieve().body(String.class))
				.isInstanceOf(ResourceAccessException.class)
				.hasRootCauseInstanceOf(HttpTimeoutException.class);
	}

	@Test
	void keepsTheGlobalClientSettings() {
		server.createContext("/generate", exchange -> {
			exchange.getResponseHeaders().add("Location", "/elsewhere");
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
		});
		server.createContext("/elsewhere", exchange -> answer(exchange, "followed"));
		HttpClientSettings global = HttpClientSettings.defaults().withRedirects(HttpRedirects.DONT_FOLLOW);

		var response = client(global, Duration.ofSeconds(5)).get().uri("/generate").retrieve().toEntity(String.class);

		assertThat(response.getStatusCode().value()).isEqualTo(302);
	}

	private static RestClient clientFor(String url) {
		return new AiServiceClientConfig().aiServiceClient(RestClient.builder(), ClientHttpRequestFactoryBuilder.jdk(),
				HttpClientSettings.defaults(), url, Duration.ofSeconds(5));
	}

	private RestClient client(HttpClientSettings globalSettings, Duration timeout) {
		String url = "http://localhost:" + server.getAddress().getPort();
		return new AiServiceClientConfig().aiServiceClient(RestClient.builder(), ClientHttpRequestFactoryBuilder.jdk(),
				globalSettings, url, timeout);
	}

	private static HttpServer startServer() {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
			server.start();
			return server;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void answer(HttpExchange exchange, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(200, bytes.length);
		try (OutputStream out = exchange.getResponseBody()) {
			out.write(bytes);
		}
	}

	private static void sleep(Duration duration) {
		try {
			Thread.sleep(duration);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}
