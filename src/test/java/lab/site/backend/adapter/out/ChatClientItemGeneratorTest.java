package lab.site.backend.adapter.out;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lab.site.backend.domain.dto.GeneratedItem;
import lab.site.backend.domain.exception.ItemGenerationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatClientItemGeneratorTest {

	private final ChatModel model = mock(ChatModel.class);
	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
	private final ChatClientItemGenerator generator = new ChatClientItemGenerator(ChatClient.builder(model), validator);

	@Test
	void mapsTheAiResponse() {
		reply("""
				{"name": "Steam", "emoji": "💨", "tags": ["gas", "hot"]}
				""");

		GeneratedItem item = generator.generate("fire", "water");

		assertThat(item).isEqualTo(new GeneratedItem("Steam", "💨", List.of("gas", "hot")));
	}

	@Test
	void systemPromptHoldsTheRulesAndUserPromptHoldsTheItems() {
		reply("""
				{"name": "Steam", "emoji": "💨", "tags": ["gas"]}
				""");

		generator.generate("fire", "water");

		ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
		verify(model).call(prompt.capture());
		assertThat(prompt.getValue().getSystemMessage().getText()).contains("fantasy", "sci-fi");
		assertThat(prompt.getValue().getUserMessage().getText()).contains("fire", "water");
	}

	@Test
	void providerFailureBecomesItemGenerationException() {
		RuntimeException failure = new RuntimeException("503 from provider");
		when(model.getOptions()).thenReturn(ChatOptions.builder().build());
		when(model.call(any(Prompt.class))).thenThrow(failure);

		assertThatThrownBy(() -> generator.generate("fire", "water"))
				.isInstanceOf(ItemGenerationException.class)
				.hasCause(failure);
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "not json", "null"})
	void unusableReplyBecomesItemGenerationException(String reply) {
		reply(reply);

		assertThatThrownBy(() -> generator.generate("fire", "water"))
				.isInstanceOf(ItemGenerationException.class);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidItems")
	void invalidItemBecomesItemGenerationException(String rule, String json, String property) {
		reply(json);

		assertThatThrownBy(() -> generator.generate("fire", "water"))
				.isInstanceOf(ItemGenerationException.class)
				.hasMessageContaining("invalid item")
				.cause()
				.isInstanceOf(ConstraintViolationException.class)
				.hasMessageContaining(property);
	}

	@Test
	void itemAtTheLimitsIsAccepted() {
		String tag = "a".repeat(30);
		reply(item("a".repeat(50), "💨".repeat(8), quoted(tag, tag, tag, tag, tag)));

		GeneratedItem item = generator.generate("fire", "water");

		assertThat(item.name()).hasSize(50);
		assertThat(item.emoji()).hasSize(16);
		assertThat(item.tags()).hasSize(5);
	}

	@Test
	void normalizedTagsAreValidatedAndRetried() {
		reply(item("Steam", "💨", quoted("İ".repeat(16))), item("Steam", "💨", quoted(" Gas ")));

		GeneratedItem item = generator.generate("fire", "water");

		assertThat(item.tags()).containsExactly("gas");
		verify(model, times(2)).call(any(Prompt.class));
	}

	@Test
	void invalidResultIsRetriedOnce() {
		reply(item(" ", "💨", quoted("gas")), item("Steam", "💨", quoted("gas")));

		GeneratedItem item = generator.generate("fire", "water");

		assertThat(item.name()).isEqualTo("Steam");
		verify(model, times(2)).call(any(Prompt.class));
	}

	@Test
	void providerFailureIsRetriedOnce() {
		when(model.getOptions()).thenReturn(ChatOptions.builder().build());
		when(model.call(any(Prompt.class)))
				.thenThrow(new RuntimeException("503 from provider"))
				.thenReturn(response(item("Steam", "💨", quoted("gas"))));

		GeneratedItem item = generator.generate("fire", "water");

		assertThat(item.name()).isEqualTo("Steam");
		verify(model, times(2)).call(any(Prompt.class));
	}

	@Test
	void secondFailureIsThrownWithTheFirstAttached() {
		reply(item(" ", "💨", quoted("gas")), "not json");

		assertThatThrownBy(() -> generator.generate("fire", "water"))
				.isInstanceOf(ItemGenerationException.class)
				.hasMessageContaining("AI call failed")
				.satisfies(second -> assertThat(second.getSuppressed())
						.singleElement()
						.satisfies(first -> assertThat(first).hasMessageContaining("invalid item")));
		verify(model, times(2)).call(any(Prompt.class));
	}

	@Test
	void invalidResultTwiceIsThrownWithBothReasons() {
		reply(item(" ", "💨", quoted("gas")), item("Steam", " ", quoted("gas")));

		assertThatThrownBy(() -> generator.generate("fire", "water"))
				.isInstanceOf(ItemGenerationException.class)
				.hasMessageContaining("invalid item")
				.satisfies(second -> {
					assertThat(second.getCause()).hasMessageContaining("emoji");
					assertThat(second.getSuppressed())
							.singleElement()
							.satisfies(first -> assertThat(first.getCause()).hasMessageContaining("name"));
				});
		verify(model, times(2)).call(any(Prompt.class));
	}

	static Stream<Arguments> invalidItems() {
		return Stream.of(
				Arguments.of("blank name", item(" ", "💨", quoted("gas")), "name"),
				Arguments.of("name over 50 chars", item("a".repeat(51), "💨", quoted("gas")), "name"),
				Arguments.of("blank emoji", item("Steam", " ", quoted("gas")), "emoji"),
				Arguments.of("emoji over 16 chars", item("Steam", "💨".repeat(8) + "a", quoted("gas")), "emoji"),
				Arguments.of("missing tags", """
						{"name": "Steam", "emoji": "💨"}
						""", "tags"),
				Arguments.of("no tags", item("Steam", "💨", ""), "tags"),
				Arguments.of("more than 5 tags", item("Steam", "💨", quoted("a", "b", "c", "d", "e", "f")), "tags"),
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

	private void reply(String... jsons) {
		when(model.getOptions()).thenReturn(ChatOptions.builder().build());
		OngoingStubbing<ChatResponse> call = when(model.call(any(Prompt.class)));
		for (String json : jsons) {
			call = call.thenReturn(response(json));
		}
	}

	private static ChatResponse response(String json) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(json))));
	}

}
