package fullclanker.ai.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import fullclanker.ai.dto.GeneratedItem;
import fullclanker.ai.exception.ItemGenerationException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;
import java.time.Duration;

@Service
public class ChatClientItemGenerator {

	private static final Logger log = LoggerFactory.getLogger(ChatClientItemGenerator.class);

	private static final Resource SYSTEM_PROMPT = new ClassPathResource("prompts/item-generator-system.st");
	private static final Resource USER_PROMPT = new ClassPathResource("prompts/item-generator-user.st");

	private final ChatClient chat;
	private final Validator validator;

	public ChatClientItemGenerator(ChatClient.Builder builder, Validator validator) {
		this.chat = builder.build();
		this.validator = validator;
	}

	public GeneratedItem generate(String nameA, String nameB) {
		long started = System.nanoTime();
		GeneratedItem generated;
		try {
			generated = attempt(nameA, nameB);
		} catch (ItemGenerationException first) {
			log.warn("AI result generation failed for {} + {}; retrying", nameA, nameB, first);
			try {
				generated = attempt(nameA, nameB);
			} catch (ItemGenerationException second) {
				second.addSuppressed(first);
				throw second;
			}
		}
		log.info("Generated item for {} + {} -> {} (duration={} ms)", nameA, nameB, generated.name(),
				Duration.ofNanos(System.nanoTime() - started).toMillis());
		return generated;
	}

	private GeneratedItem attempt(String nameA, String nameB) {
		GeneratedItem item;
		try {
			item = chat.prompt()
					.system(SYSTEM_PROMPT)
					.user(u -> u.text(USER_PROMPT).param("a", nameA).param("b", nameB))
					.call()
					.entity(GeneratedItem.class, ChatClient.EntityParamSpec::useProviderStructuredOutput);
		} catch (RuntimeException e) {
			throw new ItemGenerationException("AI call failed for %s + %s".formatted(nameA, nameB), e);
		}
		if (item == null) {
			throw new ItemGenerationException("AI returned no item for %s + %s".formatted(nameA, nameB));
		}
		GeneratedItem normalized = normalize(item);
		Set<ConstraintViolation<GeneratedItem>> violations = validator.validate(normalized);
		if (!violations.isEmpty()) {
			throw new ItemGenerationException("AI returned an invalid item for %s + %s".formatted(nameA, nameB),
					new ConstraintViolationException(violations));
		}
		return normalized;
	}

	private GeneratedItem normalize(GeneratedItem item) {
		String name = item.name() == null ? null : item.name().trim();
		var tags = item.tags() == null ? null : item.tags().stream()
				.map(tag -> tag == null ? null : tag.trim().toLowerCase(Locale.ROOT))
				.toList();
		return new GeneratedItem(name, item.emoji(), tags);
	}

}
