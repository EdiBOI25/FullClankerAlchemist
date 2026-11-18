package fullclanker.alchemist.adapter.in;

import fullclanker.alchemist.domain.dto.Credentials;
import fullclanker.alchemist.domain.model.Role;
import fullclanker.alchemist.domain.model.User;
import fullclanker.alchemist.domain.port.in.CreateUser;
import fullclanker.alchemist.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.json.JsonContent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockingDetails;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthControllerTest {

	private static final String PASSWORD = "password1";
	private static final String USERNAME_RULE = "username: must be 3 to 30 characters: a-z, A-Z, 0-9, _ or -";
	private static final String PASSWORD_RULE = "password: must be at least 8 characters and at most 72 bytes in UTF-8";

	private static final HttpClient client = HttpClient.newHttpClient();

	@TempDir
	static Path temporaryDirectory;

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url",
				() -> "jdbc:sqlite:" + temporaryDirectory.resolve("auth.db") + "?foreign_keys=true");
	}

	@LocalServerPort
	private int port;

	@MockitoSpyBean
	private UserRepository users;

	@Autowired
	private CreateUser createUser;

	@Autowired
	private LoggingSystem logging;

	private HttpRequest.Builder request(String path) {
		return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
	}

	private static HttpRequest.Builder withBasic(HttpRequest.Builder request, String username, String password) {
		return request.header(HttpHeaders.AUTHORIZATION,
				"Basic " + HttpHeaders.encodeBasicAuth(username, password, StandardCharsets.UTF_8));
	}

	private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
		return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}

	private static HttpRequest.Builder withCookie(HttpRequest.Builder request, String cookie) {
		return request.header(HttpHeaders.COOKIE, cookie);
	}

	private HttpResponse<String> postJson(HttpRequest.Builder request, String json) throws Exception {
		return send(request
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.POST(HttpRequest.BodyPublishers.ofString(json)));
	}

	private HttpResponse<String> signUp(String json) throws Exception {
		return postJson(request("/auth/signup"), json);
	}

	private HttpResponse<String> logIn(String json) throws Exception {
		return postJson(request("/auth/login"), json);
	}

	private void givenUser(String username, String password) throws Exception {
		assertThat(signUp(credentials(username, password)).statusCode()).isEqualTo(201);
	}

	/** Checks the response sets one HttpOnly, SameSite=Strict session cookie; returns it as a Cookie header value. */
	private static String assertSessionCookie(HttpResponse<String> response) {
		List<String> cookies = response.headers().allValues(HttpHeaders.SET_COOKIE);
		assertThat(cookies).singleElement().asString()
				.startsWith("JSESSIONID=")
				.contains("HttpOnly")
				.contains("SameSite=Strict");
		return cookies.getFirst().split(";", 2)[0];
	}

	private static String credentials(String username, String password) {
		return """
				{"username":"%s","password":"%s"}
				""".formatted(username, password);
	}

	private static void assertProblem(HttpResponse<String> response, int status, String detail) {
		assertThat(response.statusCode()).isEqualTo(status);
		assertThat(response.headers().firstValue(HttpHeaders.CONTENT_TYPE)).hasValue(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		assertThat(new JsonContent(response.body())).extractingPath("$.detail").isEqualTo(detail);
		assertThat(response.headers().allValues(HttpHeaders.SET_COOKIE)).isEmpty();
	}

	@Test
	void signUpCreatesAUserAndReturnsIt() throws Exception {
		HttpResponse<String> response = signUp(credentials("Alice", PASSWORD));

		assertThat(response.statusCode()).isEqualTo(201);
		assertThat(new JsonContent(response.body())).isStrictlyEqualTo("""
				{"username":"Alice","role":"USER"}
				""");
		assertThat(users.findByUsernameIgnoreCase("alice").orElseThrow())
				.extracting(User::getUsername, User::getRole)
				.containsExactly("Alice", Role.USER);
	}

	@Test
	void signUpDoesNotLogIn() throws Exception {
		HttpResponse<String> response = signUp(credentials("dora", PASSWORD));

		assertThat(response.statusCode()).isEqualTo(201);
		assertThat(response.headers().allValues(HttpHeaders.SET_COOKIE)).isEmpty();
	}

	@Test
	void roleInTheBodyIsIgnored() throws Exception {
		HttpResponse<String> response = signUp("""
				{"username":"mallory","password":"%s","role":"ADMIN"}
				""".formatted(PASSWORD));

		assertThat(new JsonContent(response.body())).extractingPath("$.role").isEqualTo("USER");
		assertThat(users.findByUsernameIgnoreCase("mallory").orElseThrow().getRole()).isEqualTo(Role.USER);
	}

	static Stream<Arguments> boundaryCredentials() {
		return Stream.of(
				Arguments.of("abc", "12345678"),
				Arguments.of("z".repeat(30), PASSWORD),
				Arguments.of("Max_ascii-1", "a".repeat(72)),
				Arguments.of("max_bytes", "é".repeat(36)));
	}

	@ParameterizedTest
	@MethodSource("boundaryCredentials")
	void valuesAtTheLimitsAreAccepted(String username, String password) throws Exception {
		assertThat(signUp(credentials(username, password)).statusCode()).isEqualTo(201);
	}

	static Stream<Arguments> invalidBodies() {
		return Stream.of(
				Arguments.of(credentials("ab", PASSWORD), USERNAME_RULE),
				Arguments.of(credentials("a".repeat(31), PASSWORD), USERNAME_RULE),
				Arguments.of(credentials("bad name", PASSWORD), USERNAME_RULE),
				Arguments.of(credentials("bossé", PASSWORD), USERNAME_RULE),
				Arguments.of(credentials("erin", "1234567"), PASSWORD_RULE),
				Arguments.of(credentials("erin", "a".repeat(73)), PASSWORD_RULE),
				Arguments.of(credentials("erin", "é".repeat(37)), PASSWORD_RULE),
				Arguments.of("""
						{"username":"erin"}
						""", "password: must not be null"),
				Arguments.of("""
						{"password":"%s"}
						""".formatted(PASSWORD), "username: must not be null"),
				Arguments.of("{}", "password: must not be null; username: must not be null"));
	}

	@ParameterizedTest
	@MethodSource("invalidBodies")
	void invalidSignUpNamesTheFieldAndCreatesNoUser(String json, String detail) throws Exception {
		long before = users.count();

		assertProblem(signUp(json), 400, detail);
		assertThat(users.count()).isEqualTo(before);
	}

	@Test
	void malformedJsonIsBadRequest() throws Exception {
		assertProblem(signUp("{"), 400, "Request body must be valid JSON");
	}

	@Test
	void takenNameInDifferentCaseIsConflict() throws Exception {
		assertThat(signUp(credentials("bob", PASSWORD)).statusCode()).isEqualTo(201);

		assertProblem(signUp(credentials("BOB", "password2")), 409, "Username already taken: BOB");
		assertThat(users.findByUsernameIgnoreCase("bob").orElseThrow().getUsername()).isEqualTo("bob");
	}

	/** Each name lookup waits up to 500 ms for the other, so without the transaction both miss and both insert. */
	@Test
	void concurrentSignUpsOfOneNameInDifferentCaseCreateOneUser() throws Exception {
		CountDownLatch bothLookedUp = new CountDownLatch(2);
		Answer<?> realLookup = mockingDetails(users).getMockCreationSettings().getDefaultAnswer();
		doAnswer(invocation -> {
			Object found = realLookup.answer(invocation);
			bothLookedUp.countDown();
			bothLookedUp.await(500, MILLISECONDS);
			return found;
		}).when(users).findByUsernameIgnoreCase(anyString());
		List<Integer> statuses = new ArrayList<>();

		try (ExecutorService threads = Executors.newFixedThreadPool(2)) {
			List<Future<Integer>> signUps = Stream.of("carol", "CAROL")
					.map(name -> threads.submit(() -> signUp(credentials(name, PASSWORD)).statusCode()))
					.toList();
			for (Future<Integer> signUp : signUps) {
				statuses.add(signUp.get(10, SECONDS));
			}
		}

		assertThat(statuses).containsExactlyInAnyOrder(201, 409);
		assertThat(users.findAll()).filteredOn(user -> user.getUsername().equalsIgnoreCase("carol")).hasSize(1);
	}

	@Test
	void plainTextSignUpIsUnsupportedWithoutLogin() throws Exception {
		HttpResponse<String> response = send(request("/auth/signup")
				.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
				.POST(HttpRequest.BodyPublishers.ofString(credentials("frank", PASSWORD))));

		assertThat(response.statusCode()).isEqualTo(415);
		assertThat(response.headers().allValues(HttpHeaders.SET_COOKIE)).isEmpty();
		assertThat(users.findByUsernameIgnoreCase("frank")).isEmpty();
	}

	@Test
	void onlyPostSignUpIsPublic() throws Exception {
		assertThat(send(request("/auth/signup")).statusCode()).isEqualTo(401);
	}

	@Test
	void passwordIsNotInAnyResponse() throws Exception {
		String password = "Secret-pass-42";
		List<HttpResponse<String>> responses = List.of(
				signUp(credentials("grace", password)),
				signUp(credentials("GRACE", password)),
				signUp(credentials("bad name", password)),
				signUp(credentials("grace2", password + "x".repeat(60))));

		assertThat(responses).extracting(HttpResponse::statusCode).containsExactly(201, 409, 400, 400);
		assertThat(responses).allSatisfy(response -> assertThat(response.body()).doesNotContain(password));
	}

	@Test
	void meWithBasicReturnsTheUserAsStored() throws Exception {
		assertThat(signUp(credentials("Henry", PASSWORD)).statusCode()).isEqualTo(201);

		HttpResponse<String> response = send(withBasic(request("/auth/me"), "hENRY", PASSWORD));

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(new JsonContent(response.body())).isStrictlyEqualTo("""
				{"username":"Henry","role":"USER"}
				""");
		assertThat(response.headers().allValues(HttpHeaders.SET_COOKIE)).isEmpty();
	}

	@Test
	void meShowsTheAdminRole() throws Exception {
		createUser.create(new Credentials("Root_Admin", PASSWORD), Role.ADMIN);

		HttpResponse<String> response = send(withBasic(request("/auth/me"), "Root_Admin", PASSWORD));

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(new JsonContent(response.body())).isStrictlyEqualTo("""
				{"username":"Root_Admin","role":"ADMIN"}
				""");
	}

	@Test
	void meWithoutLoginIsUnauthorized() throws Exception {
		assertProblem(send(request("/auth/me")), 401, "Invalid or missing credentials");
	}

	@Test
	void logInReturnsTheUserAndSetsAStrictHttpOnlySessionCookie() throws Exception {
		givenUser("Jack", PASSWORD);

		HttpResponse<String> response = logIn(credentials("Jack", PASSWORD));

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(new JsonContent(response.body())).isStrictlyEqualTo("""
				{"username":"Jack","role":"USER"}
				""");
		assertSessionCookie(response);
	}

	@Test
	void sessionCookieAloneLogsInLaterRequests() throws Exception {
		givenUser("kate", PASSWORD);
		String cookie = assertSessionCookie(logIn(credentials("kate", PASSWORD)));

		HttpResponse<String> me = send(withCookie(request("/auth/me"), cookie));

		assertThat(me.statusCode()).isEqualTo(200);
		assertThat(new JsonContent(me.body())).extractingPath("$.username").isEqualTo("kate");
		assertThat(send(withCookie(request("/items"), cookie)).statusCode()).isEqualTo(200);
	}

	@Test
	void logInIgnoresTheUsernameCase() throws Exception {
		givenUser("Liam", PASSWORD);

		HttpResponse<String> response = logIn(credentials("LIAM", PASSWORD));

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(new JsonContent(response.body())).extractingPath("$.username").isEqualTo("Liam");
	}

	@Test
	void wrongPasswordAndUnknownUserGetTheSameLogInResponse() throws Exception {
		givenUser("mia", PASSWORD);

		HttpResponse<String> wrongPassword = logIn(credentials("mia", "wrongpass1"));
		HttpResponse<String> unknownUser = logIn(credentials("nobody", PASSWORD));

		assertProblem(wrongPassword, 401, "Invalid or missing credentials");
		assertProblem(unknownUser, 401, "Invalid or missing credentials");
		assertThat(unknownUser.body()).isEqualTo(wrongPassword.body());
	}

	@Test
	void logInWithAnEarlierSessionGetsANewSessionId() throws Exception {
		givenUser("noah", PASSWORD);
		givenUser("olga", PASSWORD);
		String first = assertSessionCookie(logIn(credentials("noah", PASSWORD)));

		HttpResponse<String> second = postJson(withCookie(request("/auth/login"), first), credentials("olga", PASSWORD));

		assertThat(second.statusCode()).isEqualTo(200);
		String renewed = assertSessionCookie(second);
		assertThat(renewed).isNotEqualTo(first);
		assertThat(new JsonContent(send(withCookie(request("/auth/me"), renewed)).body()))
				.extractingPath("$.username").isEqualTo("olga");
		assertProblem(send(withCookie(request("/auth/me"), first)), 401, "Invalid or missing credentials");
	}

	@Test
	void logInWithAMadeUpSessionCookieGetsAFreshId() throws Exception {
		givenUser("sara", PASSWORD);
		String madeUp = "JSESSIONID=MADEUP0000000000000000000000000000";

		HttpResponse<String> response = postJson(withCookie(request("/auth/login"), madeUp), credentials("sara", PASSWORD));

		assertThat(response.statusCode()).isEqualTo(200);
		String cookie = assertSessionCookie(response);
		assertThat(cookie).isNotEqualTo(madeUp);
		assertThat(send(withCookie(request("/auth/me"), cookie)).statusCode()).isEqualTo(200);
		assertProblem(send(withCookie(request("/auth/me"), madeUp)), 401, "Invalid or missing credentials");
	}

	@Test
	void failedLogInKeepsTheEarlierSession() throws Exception {
		givenUser("tom", PASSWORD);
		String cookie = assertSessionCookie(logIn(credentials("tom", PASSWORD)));

		HttpResponse<String> failed = postJson(withCookie(request("/auth/login"), cookie), credentials("tom", "wrongpass1"));

		assertProblem(failed, 401, "Invalid or missing credentials");
		assertThat(new JsonContent(send(withCookie(request("/auth/me"), cookie)).body()))
				.extractingPath("$.username").isEqualTo("tom");
	}

	static Stream<RuntimeException> lookupFailures() {
		return Stream.of(
				new DataAccessResourceFailureException("database is locked"),
				new InvalidDataAccessApiUsageException("bad query", new IllegalArgumentException("internal detail")));
	}

	@ParameterizedTest
	@MethodSource("lookupFailures")
	void failingUserLookupIsServerErrorNotBadCredentials(RuntimeException failure) throws Exception {
		doThrow(failure).when(users).findByUsernameIgnoreCase(anyString());

		assertProblem(logIn(credentials("uma", PASSWORD)), 500, "Could not check credentials, try again later");
		assertProblem(send(withBasic(request("/auth/me"), "uma", PASSWORD)), 500,
				"Could not check credentials, try again later");
	}

	static Stream<Arguments> invalidLogIns() {
		return Stream.of(
				Arguments.of(credentials("ab", PASSWORD), USERNAME_RULE),
				Arguments.of(credentials("paul", "short"), PASSWORD_RULE),
				Arguments.of("{}", "password: must not be null; username: must not be null"));
	}

	@ParameterizedTest
	@MethodSource("invalidLogIns")
	void logInBreakingTheRulesIsBadRequest(String json, String detail) throws Exception {
		assertProblem(logIn(json), 400, detail);
	}

	@Test
	void plainTextLogInIsUnsupported() throws Exception {
		HttpResponse<String> response = send(request("/auth/login")
				.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
				.POST(HttpRequest.BodyPublishers.ofString(credentials("quinn", PASSWORD))));

		assertThat(response.statusCode()).isEqualTo(415);
		assertThat(response.headers().allValues(HttpHeaders.SET_COOKIE)).isEmpty();
	}

	@Test
	void onlyPostLogInIsPublic() throws Exception {
		assertThat(send(request("/auth/login")).statusCode()).isEqualTo(401);
	}

	@Test
	void passwordIsNotInAnyLogInResponse() throws Exception {
		String password = "Login-secret-77";
		givenUser("rose", password);

		List<HttpResponse<String>> responses = List.of(
				logIn(credentials("rose", password)),
				logIn(credentials("rose", password + "x")),
				logIn(credentials("bad name", password)),
				logIn(credentials("rose", password + "x".repeat(60))));

		assertThat(responses).extracting(HttpResponse::statusCode).containsExactly(200, 401, 400, 400);
		assertThat(responses).allSatisfy(response -> assertThat(response.body()).doesNotContain(password));
	}

	/** Raises every logger to TRACE (the pinned ones stay at INFO) only around these requests. */
	@Test
	@ExtendWith(OutputCaptureExtension.class)
	void passwordNeverReachesTheLogsEvenAtTrace(CapturedOutput output) throws Exception {
		String password = "LogSecret9191";
		LogLevel rootLevel = logging.getLoggerConfiguration(LoggingSystem.ROOT_LOGGER_NAME).getEffectiveLevel();
		List<HttpResponse<String>> responses;
		logging.setLogLevel(LoggingSystem.ROOT_LOGGER_NAME, LogLevel.TRACE);
		try {
			givenUser("zoe", password);
			responses = List.of(
					signUp(credentials("ZOE", password)),
					signUp(credentials("bad name", password)),
					signUp(credentials("zoe2", password + "x".repeat(60))),
					signUp("{\"username\":\"zoe3\",\"password\":" + password + "}"),
					send(request("/auth/signup")
							.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
							.POST(HttpRequest.BodyPublishers.ofString(credentials("zoe4", password)))),
					logIn(credentials("zoe", password)),
					logIn(credentials("zoe", password + "x")),
					send(withBasic(request("/auth/me"), "zoe", password)),
					send(withBasic(request("/auth/me"), "zoe", password + "y")));
		}
		finally {
			logging.setLogLevel(LoggingSystem.ROOT_LOGGER_NAME, rootLevel);
		}

		assertThat(responses).extracting(HttpResponse::statusCode)
				.containsExactly(409, 400, 400, 400, 415, 200, 401, 200, 401);
		assertThat(output.getAll()).contains(" TRACE ").doesNotContain(password,
				HttpHeaders.encodeBasicAuth("zoe", password, StandardCharsets.UTF_8),
				HttpHeaders.encodeBasicAuth("zoe", password + "y", StandardCharsets.UTF_8));
	}

	@Test
	void logOutEndsTheSession() throws Exception {
		givenUser("vera", PASSWORD);
		String cookie = assertSessionCookie(logIn(credentials("vera", PASSWORD)));

		HttpResponse<String> response = send(withCookie(request("/auth/logout"), cookie)
				.POST(HttpRequest.BodyPublishers.noBody()));

		assertThat(response.statusCode()).isEqualTo(204);
		assertThat(response.body()).isEmpty();
		assertThat(response.headers().allValues(HttpHeaders.SET_COOKIE)).isEmpty();
		assertProblem(send(withCookie(request("/auth/me"), cookie)), 401, "Invalid or missing credentials");
	}

	@ParameterizedTest
	@ValueSource(strings = {"GET", "PUT", "DELETE"})
	void otherMethodsOnLogOutDoNotLogOut(String method) throws Exception {
		String username = "walt_" + method.toLowerCase();
		givenUser(username, PASSWORD);
		String cookie = assertSessionCookie(logIn(credentials(username, PASSWORD)));

		HttpResponse<String> response = send(withCookie(request("/auth/logout"), cookie)
				.method(method, HttpRequest.BodyPublishers.noBody()));

		assertThat(response.statusCode()).isNotEqualTo(204);
		assertThat(send(withCookie(request("/auth/me"), cookie)).statusCode()).isEqualTo(200);
	}

	@Test
	void logInThroughAnHttpsProxyGetsASecureCookieAndHsts() throws Exception {
		givenUser("xena", PASSWORD);

		HttpResponse<String> response = postJson(request("/auth/login").header("X-Forwarded-Proto", "https"),
				credentials("xena", PASSWORD));

		assertThat(response.statusCode()).isEqualTo(200);
		assertSessionCookie(response);
		assertThat(response.headers().firstValue(HttpHeaders.SET_COOKIE).orElseThrow()).contains("; Secure");
		assertThat(response.headers().firstValue("Strict-Transport-Security").orElseThrow()).startsWith("max-age=");
	}

	@Test
	void plainHttpLogInGetsNeitherSecureCookieNorHsts() throws Exception {
		givenUser("yuri", PASSWORD);

		HttpResponse<String> response = logIn(credentials("yuri", PASSWORD));

		assertThat(response.statusCode()).isEqualTo(200);
		assertSessionCookie(response);
		assertThat(response.headers().firstValue(HttpHeaders.SET_COOKIE).orElseThrow()).doesNotContain("Secure");
		assertThat(response.headers().firstValue("Strict-Transport-Security")).isEmpty();
	}

	@Test
	void anonymousBrowserLogOutGetsNoContentNotARedirect() throws Exception {
		HttpResponse<String> response = send(request("/auth/logout")
				.header(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE)
				.POST(HttpRequest.BodyPublishers.noBody()));

		assertThat(response.statusCode()).isEqualTo(204);
		assertThat(response.headers().firstValue(HttpHeaders.LOCATION)).isEmpty();
	}

}
