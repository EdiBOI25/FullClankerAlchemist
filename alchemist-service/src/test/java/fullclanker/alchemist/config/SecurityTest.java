package fullclanker.alchemist.config;

import fullclanker.alchemist.TestPlayer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonContent;

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

	private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
		return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}

	private int status(HttpRequest.Builder request) throws Exception {
		return send(request).statusCode();
	}

	private static void assertProblem(HttpResponse<String> response, int status, String detail) {
		assertThat(response.statusCode()).isEqualTo(status);
		assertThat(response.headers().firstValue(HttpHeaders.CONTENT_TYPE)).hasValue(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		assertThat(new JsonContent(response.body())).extractingPath("$.detail").isEqualTo(detail);
	}

	private static void assertUnauthorized(HttpResponse<String> response) {
		assertProblem(response, 401, "Invalid or missing credentials");
		assertThat(response.headers().firstValue(HttpHeaders.WWW_AUTHENTICATE)).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {"/items", "/recipes"})
	void readingWithoutCredentialsIsUnauthorized(String path) throws Exception {
		assertUnauthorized(send(request(path)));
	}

	@Test
	void browserWithoutCredentialsGetsTheProblemToo() throws Exception {
		assertUnauthorized(send(request("/items").header(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE)));
	}

	@Test
	void combiningWithoutCredentialsIsUnauthorized() throws Exception {
		assertUnauthorized(send(request("/combine")
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("{\"a\":\"fire\",\"b\":\"water\"}"))));
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
	void wrongPasswordAndUnknownUserGetTheSameResponse() throws Exception {
		HttpResponse<String> wrongPassword = send(request("/items", TestPlayer.USERNAME, "wrongpass1"));
		HttpResponse<String> unknownUser = send(request("/items", "nobody", TestPlayer.PASSWORD));

		assertUnauthorized(wrongPassword);
		assertUnauthorized(unknownUser);
		assertThat(unknownUser.body()).isEqualTo(wrongPassword.body());
	}

	@Test
	void unauthorizedSetsNoCookie() throws Exception {
		assertThat(send(request("/items")).headers().allValues(HttpHeaders.SET_COOKIE)).isEmpty();
	}

	@Test
	void basicLoginSetsNoCookie() throws Exception {
		assertThat(send(request("/items", TestPlayer.USERNAME, TestPlayer.PASSWORD)).headers()
				.allValues(HttpHeaders.SET_COOKIE)).isEmpty();
	}

	@Test
	void unknownPathIsNotFoundWhenLoggedIn() throws Exception {
		assertThat(status(request("/nothing-here", TestPlayer.USERNAME, TestPlayer.PASSWORD))).isEqualTo(404);
	}

	@Test
	void defaultLoginPageIsOff() throws Exception {
		assertThat(status(request("/login", TestPlayer.USERNAME, TestPlayer.PASSWORD))).isEqualTo(404);
	}

	@Test
	void defaultLogoutIsOff() throws Exception {
		assertThat(status(request("/logout", TestPlayer.USERNAME, TestPlayer.PASSWORD)
				.POST(HttpRequest.BodyPublishers.noBody())))
				.isEqualTo(404);
	}

	@Test
	void plainTextCombineIsUnsupportedWhenLoggedIn() throws Exception {
		assertThat(status(request("/combine", TestPlayer.USERNAME, TestPlayer.PASSWORD)
				.header(HttpHeaders.CONTENT_TYPE, "text/plain")
				.POST(HttpRequest.BodyPublishers.ofString("fire water"))))
				.isEqualTo(415);
	}

	@Test
	void healthIsPublic() throws Exception {
		assertThat(status(request("/actuator/health"))).isEqualTo(200);
	}

	@Test
	void errorOnAPublicEndpointKeepsItsStatus() throws Exception {
		assertThat(status(request("/actuator/health").POST(HttpRequest.BodyPublishers.noBody()))).isEqualTo(405);
	}

	@ParameterizedTest
	@ValueSource(strings = {"/actuator/metrics", "/actuator/info"})
	void adminEndpointsNeedLogin(String path) throws Exception {
		assertUnauthorized(send(request(path)));
	}

	@ParameterizedTest
	@ValueSource(strings = {"/actuator/metrics", "/actuator/info"})
	void adminEndpointsAreForbiddenForUsers(String path) throws Exception {
		assertProblem(send(request(path, TestPlayer.USERNAME, TestPlayer.PASSWORD)), 403, "Access denied");
	}

	@ParameterizedTest
	@ValueSource(strings = {"/actuator/metrics", "/actuator/info"})
	void adminEndpointsAreOpenForTheAdmin(String path) throws Exception {
		assertThat(status(request(path, ADMIN, ADMIN_PASSWORD))).isEqualTo(200);
	}

}
