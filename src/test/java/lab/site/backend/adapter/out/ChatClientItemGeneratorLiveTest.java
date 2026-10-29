package lab.site.backend.adapter.out;

import lab.site.backend.domain.dto.GeneratedItem;
import lab.site.backend.domain.port.out.ItemGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.ai.google.genai.api-key=${GEMINI_API_KEY}")
@EnabledIf("liveTestsEnabled")
class ChatClientItemGeneratorLiveTest {

	@Autowired
	private ItemGenerator generator;

	@Test
	void realGeminiReturnsNameTagsAndEmoji() {
		GeneratedItem item = generator.generate("fire", "water");

		assertThat(item.name()).isNotBlank();
		assertThat(item.tags()).isNotEmpty();
		assertThat(item.emoji()).isNotBlank();
	}

	/** True when LIVE_TESTS=true and GEMINI_API_KEY are set, in the OS environment or in ./.env. */
	static boolean liveTestsEnabled() throws IOException {
		Properties dotEnv = new Properties();
		Path file = Path.of(".env");
		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				dotEnv.load(reader);
			}
		}
		Function<String, String> env = name -> System.getenv().getOrDefault(name, dotEnv.getProperty(name));
		return "true".equalsIgnoreCase(env.apply("LIVE_TESTS")) && StringUtils.hasText(env.apply("GEMINI_API_KEY"));
	}

}
