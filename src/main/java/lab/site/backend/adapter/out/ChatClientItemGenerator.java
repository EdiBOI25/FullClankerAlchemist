package lab.site.backend.adapter.out;

import lab.site.backend.domain.dto.GeneratedItem;
import lab.site.backend.domain.port.out.ItemGenerator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class ChatClientItemGenerator implements ItemGenerator {

	private final ChatClient chat;
	private final Resource systemPrompt;
	private final Resource userPrompt;

	public ChatClientItemGenerator(ChatClient.Builder builder,
			@Value("classpath:prompts/item-generator-system.st") Resource systemPrompt,
			@Value("classpath:prompts/item-generator-user.st") Resource userPrompt) {
		this.chat = builder.build();
		this.systemPrompt = systemPrompt;
		this.userPrompt = userPrompt;
	}

	@Override
	public GeneratedItem generate(String nameA, String nameB) {
		return chat.prompt()
				.system(systemPrompt)
				.user(u -> u.text(userPrompt).param("a", nameA).param("b", nameB))
				.call()
				.entity(GeneratedItem.class);
	}

}
