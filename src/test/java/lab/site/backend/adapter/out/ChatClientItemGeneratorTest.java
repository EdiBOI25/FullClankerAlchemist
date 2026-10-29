package lab.site.backend.adapter.out;

import lab.site.backend.domain.dto.GeneratedItem;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatClientItemGeneratorTest {

	private final ChatModel model = mock(ChatModel.class);
	private final ChatClientItemGenerator generator = new ChatClientItemGenerator(ChatClient.builder(model));

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

	private void reply(String json) {
		when(model.getOptions()).thenReturn(ChatOptions.builder().build());
		when(model.call(any(Prompt.class)))
				.thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(json)))));
	}

}
