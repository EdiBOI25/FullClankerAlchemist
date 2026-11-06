package fullclanker.ai.controller;

import fullclanker.ai.dto.GeneratedItem;
import fullclanker.ai.exception.ItemGenerationException;
import fullclanker.ai.service.ChatClientItemGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(GenerateController.class)
class GenerateControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private ChatClientItemGenerator generator;

	@Test
	void validPairReturnsTheItem() {
		when(generator.generate("laser", "sword"))
				.thenReturn(new GeneratedItem("Laser Blade", "⚔️", List.of("energy", "weapon")));

		var response = assertThat(post("""
				{"a":"laser","b":"sword"}
				"""))
				.hasStatusOk()
				.bodyJson();

		response.extractingPath("$.name").isEqualTo("Laser Blade");
		response.extractingPath("$.emoji").isEqualTo("⚔️");
		response.extractingPath("$.tags").asArray().containsExactly("energy", "weapon");
	}

	@Test
	void namesAtTheLimitAreAccepted() {
		String a = "a".repeat(50);
		String b = "b".repeat(50);
		when(generator.generate(a, b)).thenReturn(new GeneratedItem("Long", "📏", List.of("long")));

		assertThat(post("""
				{"a":"%s","b":"%s"}
				""".formatted(a, b)))
				.hasStatusOk();
	}

	@Test
	void generatorFailureReturnsBadGatewayWithOurMessageOnly() {
		when(generator.generate("laser", "sword")).thenThrow(new ItemGenerationException(
				"AI call failed for laser + sword", new RuntimeException("provider secret details")));

		var result = assertThat(post("""
				{"a":"laser","b":"sword"}
				"""))
				.hasStatus(502)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON);

		result.bodyJson().extractingPath("$.detail").isEqualTo("AI call failed for laser + sword");
		result.bodyText().doesNotContain("provider secret details");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("badRequests")
	void badRequestReturnsBadRequestProblem(String rule, String body) {
		assertThat(post(body))
				.hasStatus(400)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		verifyNoInteractions(generator);
	}

	static Stream<Arguments> badRequests() {
		return Stream.of(
				Arguments.of("missing b", """
						{"a":"laser"}
						"""),
				Arguments.of("null a", """
						{"a":null,"b":"sword"}
						"""),
				Arguments.of("blank a", """
						{"a":" ","b":"sword"}
						"""),
				Arguments.of("object instead of text", """
						{"a":{},"b":"sword"}
						"""),
				Arguments.of("a over 50 chars", """
						{"a":"%s","b":"sword"}
						""".formatted("a".repeat(51))),
				Arguments.of("b over 50 chars", """
						{"a":"laser","b":"%s"}
						""".formatted("b".repeat(51))),
				Arguments.of("invalid JSON", "{\"a\":"));
	}

	private MockMvcTester.MockMvcRequestBuilder post(String body) {
		return mvc.post().uri("/generate").contentType(MediaType.APPLICATION_JSON).content(body);
	}

}
