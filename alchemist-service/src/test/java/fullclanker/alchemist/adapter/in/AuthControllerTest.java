package fullclanker.alchemist.adapter.in;

import fullclanker.alchemist.domain.model.Role;
import fullclanker.alchemist.domain.model.User;
import fullclanker.alchemist.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
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

	private HttpRequest.Builder request(String path) {
		return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
	}

	private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
		return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> signUp(String json) throws Exception {
		return send(request("/auth/signup")
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.POST(HttpRequest.BodyPublishers.ofString(json)));
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

}
