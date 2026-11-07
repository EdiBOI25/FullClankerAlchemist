package fullclanker.alchemist.adapter.out;

import fullclanker.alchemist.domain.dto.GeneratedItem;
import fullclanker.alchemist.domain.exception.AiServiceUnavailableException;
import fullclanker.alchemist.domain.exception.ItemGenerationException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpItemGeneratorTest {

	private final RestClient.Builder builder = RestClient.builder().baseUrl("http://ai-service");
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
	private final HttpItemGenerator generator = new HttpItemGenerator(builder.build(), validator);

	@AfterEach
	void allExpectedRequestsWereMade() {
		server.verify();
	}

	@Test
	void sendsThePairAndMapsTheReply() {
		server.expect(requestTo("http://ai-service/generate"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().json("""
						{"a": "fire", "b": "water"}
						""", JsonCompareMode.STRICT))
				.andRespond(withSuccess(item("Steam", "💨", quoted("gas", "hot")), MediaType.APPLICATION_JSON));

		GeneratedItem item = generator.generate("fire", "water");

		assertThat(item).isEqualTo(new GeneratedItem("Steam", "💨", List.of("gas", "hot")));
	}

	@Test
	void badGatewayBecomesItemGenerationExceptionWithOurOwnMessage() {
		server.expect(requestTo("http://ai-service/generate"))
				.andRespond(withStatus(HttpStatus.BAD_GATEWAY)
						.contentType(MediaType.APPLICATION_PROBLEM_JSON)
						.body("""
								{"status": 502, "detail": "provider secret detail"}
								"""));

		assertThatThrownBy(() -> generator.generate("fire", "water"))
				.isInstanceOf(ItemGenerationException.class)
				.hasMessage("AI call failed for fire + water")
				.hasCauseInstanceOf(RestClientResponseException.class);
	}

	@ParameterizedTest
	@EnumSource(value = HttpStatus.class, names = {"BAD_REQUEST", "NOT_FOUND", "INTERNAL_SERVER_ERROR", "SERVICE_UNAVAILABLE"})
	void otherErrorStatusBecomesItemGenerationException(HttpStatus status) {
		server.expect(requestTo("http://ai-service/generate")).andRespond(withStatus(status));

		assertThatThrownBy(() -> generator.generate("fire", "water"))
				.isInstanceOf(ItemGenerationException.class)
				.hasMessage("AI call failed for fire + water");
	}

	@Test
	void emptyReplyBecomesItemGenerationException() {
		server.expect(requestTo("http://ai-service/generate")).andRespond(withSuccess());

		assertThatThrownBy(() -> generator.generate("fire", "water"))
				.isInstanceOf(ItemGenerationException.class)
				.hasMessage("AI returned no item for fire + water");
	}

	@Test
	void nullJsonBecomesItemGenerationException() {
		server.expect(requestTo("http://ai-service/generate"))
				.andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> generator.generate("fire", "water"))
				.isInstanceOf(ItemGenerationException.class)
				.hasMessage("AI returned no item for fire + water");
	}

	@Test
	void readTimeoutBecomesItemGenerationExceptionAfterOneRequest() {
		server.expect(ExpectedCount.once(), requestTo("http://ai-service/generate"))
				.andRespond(withException(new HttpTimeoutException("Request cancelled")));

		assertThatThrownBy(() -> generator.generate("fire", "water"))
				.isInstanceOf(ItemGenerationException.class)
				.hasMessage("AI call failed for fire + water")
				.hasRootCauseInstanceOf(HttpTimeoutException.class);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("unreachableFailures")
	void unreachableThenSuccessRetriesOnce(String reason, IOException failure) {
		server.expect(requestTo("http://ai-service/generate")).andRespond(withException(failure));
		server.expect(requestTo("http://ai-service/generate"))
				.andRespond(withSuccess(item("Steam", "💨", quoted("gas")), MediaType.APPLICATION_JSON));

		GeneratedItem item = generator.generate("fire", "water");

		assertThat(item).isEqualTo(new GeneratedItem("Steam", "💨", List.of("gas")));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("unreachableFailures")
	void unreachableTwiceBecomesAiServiceUnavailableExceptionAfterTwoRequests(String reason, IOException failure) {
		server.expect(ExpectedCount.twice(), requestTo("http://ai-service/generate")).andRespond(withException(failure));

		assertThatThrownBy(() -> generator.generate("fire", "water"))
				.isInstanceOf(AiServiceUnavailableException.class)
				.hasMessage("AI service is unavailable for fire + water, try again later")
				.satisfies(second -> assertThat(second.getSuppressed())
						.singleElement()
						.isInstanceOf(AiServiceUnavailableException.class))
				.rootCause()
				.isSameAs(failure);
	}

	@Test
	void unreachableThenBadGatewayBecomesItemGenerationException() {
		server.expect(requestTo("http://ai-service/generate"))
				.andRespond(withException(new ConnectException("Connection refused")));
		server.expect(requestTo("http://ai-service/generate")).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

		assertThatThrownBy(() -> generator.generate("fire", "water"))
				.isInstanceOf(ItemGenerationException.class)
				.hasMessage("AI call failed for fire + water");
	}

	static Stream<Arguments> unreachableFailures() {
		return Stream.of(
				Arguments.of("connection refused", new ConnectException("Connection refused")),
				Arguments.of("unknown host", new UnknownHostException("ai-service")),
				Arguments.of("connect timeout", new HttpConnectTimeoutException("HTTP connect timed out")));
	}

	@ParameterizedTest
	@ValueSource(strings = {"not json", "[]", "{\"name\": [\"Steam\"], \"emoji\": \"💨\", \"tags\": [\"gas\"]}"})
	void unreadableReplyBecomesItemGenerationException(String reply) {
		server.expect(requestTo("http://ai-service/generate"))
				.andRespond(withSuccess(reply, MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> generator.generate("fire", "water"))
				.isInstanceOf(ItemGenerationException.class)
				.hasMessage("AI call failed for fire + water");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidItems")
	void invalidItemBecomesItemGenerationException(String rule, String json, String property) {
		server.expect(requestTo("http://ai-service/generate"))
				.andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> generator.generate("fire", "water"))
				.isInstanceOf(ItemGenerationException.class)
				.hasMessage("AI returned an invalid item for fire + water")
				.cause()
				.isInstanceOf(ConstraintViolationException.class)
				.hasMessageContaining(property);
	}

	@Test
	void itemAtTheLimitsIsAccepted() {
		String tag = "a".repeat(30);
		server.expect(requestTo("http://ai-service/generate"))
				.andRespond(withSuccess(item("a".repeat(50), "💨".repeat(8), quoted(tag, tag, tag, tag, tag)),
						MediaType.APPLICATION_JSON));

		GeneratedItem item = generator.generate("fire", "water");

		assertThat(item.name()).hasSize(50);
		assertThat(item.emoji()).hasSize(16);
		assertThat(item.tags()).hasSize(5);
	}

	static Stream<Arguments> invalidItems() {
		return Stream.of(
				Arguments.of("missing name", """
						{"emoji": "💨", "tags": ["gas"]}
						""", "name"),
				Arguments.of("blank name", item(" ", "💨", quoted("gas")), "name"),
				Arguments.of("name over 50 chars", item("a".repeat(51), "💨", quoted("gas")), "name"),
				Arguments.of("missing emoji", """
						{"name": "Steam", "tags": ["gas"]}
						""", "emoji"),
				Arguments.of("blank emoji", item("Steam", " ", quoted("gas")), "emoji"),
				Arguments.of("emoji over 16 chars", item("Steam", "💨".repeat(8) + "a", quoted("gas")), "emoji"),
				Arguments.of("missing tags", """
						{"name": "Steam", "emoji": "💨"}
						""", "tags"),
				Arguments.of("no tags", item("Steam", "💨", ""), "tags"),
				Arguments.of("more than 5 tags", item("Steam", "💨", quoted("a", "b", "c", "d", "e", "f")), "tags"),
				Arguments.of("null tag", item("Steam", "💨", "null"), "tags"),
				Arguments.of("blank tag", item("Steam", "💨", quoted(" ")), "tags"),
				Arguments.of("tag over 30 chars", item("Steam", "💨", quoted("a".repeat(31))), "tags"));
	}

	private static String item(String name, String emoji, String tags) {
		return """
				{"name": "%s", "emoji": "%s", "tags": [%s]}
				""".formatted(name, emoji, tags);
	}

	private static String quoted(String... tags) {
		return Stream.of(tags).map(tag -> "\"" + tag + "\"").collect(Collectors.joining(", "));
	}

}
