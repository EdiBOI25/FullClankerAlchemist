package fullclanker.ai.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
public class GeminiClientConfig {

	/** Gemini client with a request timeout and no SDK retries; the starter has no properties for them, so its own client backs off. */
	@Bean
	@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "google-genai")
	public Client geminiClient(@Value("${spring.ai.google.genai.api-key}") String apiKey,
			@Value("${ai.timeout}") Duration timeout) {
		return Client.builder()
				.apiKey(apiKey)
				.httpOptions(HttpOptions.builder()
						.timeout(Math.toIntExact(timeout.toMillis()))
						.retryOptions(HttpRetryOptions.builder().attempts(1).httpStatusCodes(List.of()).build())
						.build())
				.build();
	}

}
