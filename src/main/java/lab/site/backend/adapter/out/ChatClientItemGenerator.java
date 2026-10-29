package lab.site.backend.adapter.out;

import lab.site.backend.domain.dto.GeneratedItem;
import lab.site.backend.domain.port.out.ItemGenerator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class ChatClientItemGenerator implements ItemGenerator {

	private static final Resource SYSTEM_PROMPT = new ClassPathResource("prompts/item-generator-system.st");
	private static final Resource USER_PROMPT = new ClassPathResource("prompts/item-generator-user.st");

	private final ChatClient chat;

	public ChatClientItemGenerator(ChatClient.Builder builder) {
		this.chat = builder.build();
	}

	@Override
	public GeneratedItem generate(String nameA, String nameB) {
		return chat.prompt()
				.system(SYSTEM_PROMPT)
				.user(u -> u.text(USER_PROMPT).param("a", nameA).param("b", nameB))
				.call()
				.entity(GeneratedItem.class, ChatClient.EntityParamSpec::useProviderStructuredOutput);
	}

}
