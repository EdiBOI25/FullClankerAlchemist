package fullclanker.alchemist.config;

import fullclanker.alchemist.TestPlayer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"alchemist.admin.username=" + SecurityTest.ADMIN,
		"alchemist.admin.password=" + SecurityTest.ADMIN_PASSWORD})
@Import(TestPlayer.class)
class SecurityTest {

	static final String ADMIN = "boss";
	static final String ADMIN_PASSWORD = "bosspass1";

	private static final HttpClient client = HttpClient.newHttpClient();

	@LocalServerPort
	private int port;

	private HttpRequest.Builder request(String path) {
		return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
	}

	private HttpRequest.Builder request(String path, String username, String password) {
		return request(path).header(HttpHeaders.AUTHORIZATION,
				"Basic " + HttpHeaders.encodeBasicAuth(username, password, StandardCharsets.UTF_8));
	}

	private int status(HttpRequest.Builder request) throws Exception {
		return client.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
	}

	@ParameterizedTest
	@ValueSource(strings = {"/items", "/recipes"})
	void readingWithoutCredentialsIsUnauthorized(String path) throws Exception {
		assertThat(status(request(path))).isEqualTo(401);
	}

	@Test
	void combiningWithoutCredentialsIsUnauthorized() throws Exception {
		assertThat(status(request("/combine")
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("{\"a\":\"fire\",\"b\":\"water\"}"))))
				.isEqualTo(401);
	}

	@ParameterizedTest
	@ValueSource(strings = {"/items", "/recipes"})
	void validBasicCredentialsAreAccepted(String path) throws Exception {
		assertThat(status(request(path, TestPlayer.USERNAME, TestPlayer.PASSWORD))).isEqualTo(200);
	}

	@Test
	void usernameIgnoresCase() throws Exception {
		assertThat(status(request("/items", TestPlayer.USERNAME.toUpperCase(), TestPlayer.PASSWORD))).isEqualTo(200);
	}

	@Test
	void wrongPasswordIsUnauthorized() throws Exception {
		assertThat(status(request("/items", TestPlayer.USERNAME, "wrongpass1"))).isEqualTo(401);
	}

	@Test
	void unknownUserIsUnauthorized() throws Exception {
		assertThat(status(request("/items", "nobody", TestPlayer.PASSWORD))).isEqualTo(401);
	}

	@Test
	void healthIsPublic() throws Exception {
		assertThat(status(request("/actuator/health"))).isEqualTo(200);
	}

	@ParameterizedTest
	@ValueSource(strings = {"/actuator/metrics", "/actuator/info"})
	void adminEndpointsNeedLogin(String path) throws Exception {
		assertThat(status(request(path))).isEqualTo(401);
	}

	@ParameterizedTest
	@ValueSource(strings = {"/actuator/metrics", "/actuator/info"})
	void adminEndpointsAreForbiddenForUsers(String path) throws Exception {
		assertThat(status(request(path, TestPlayer.USERNAME, TestPlayer.PASSWORD))).isEqualTo(403);
	}

	@ParameterizedTest
	@ValueSource(strings = {"/actuator/metrics", "/actuator/info"})
	void adminEndpointsAreOpenForTheAdmin(String path) throws Exception {
		assertThat(status(request(path, ADMIN, ADMIN_PASSWORD))).isEqualTo(200);
	}

}
