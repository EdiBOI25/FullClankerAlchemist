package lab.site.backend.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/** Stops startup when AI_PROVIDER is unknown or the selected provider has no API key. */
public class AiProviderCheck implements EnvironmentPostProcessor {

	private record Provider(String name, String keyProperty, String keyVariable) {}

	private static final List<Provider> PROVIDERS = List.of(
			new Provider("google-genai", "spring.ai.google.genai.api-key", "GEMINI_API_KEY"),
			new Provider("anthropic", "spring.ai.anthropic.api-key", "ANTHROPIC_API_KEY"));

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
		String selected = env.getProperty("spring.ai.model.chat", "");
		Provider provider = PROVIDERS.stream()
				.filter(p -> p.name().equals(selected))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("AI_PROVIDER is '" + selected + "'; use "
						+ PROVIDERS.stream().map(Provider::name).collect(Collectors.joining(" or "))));
		if (StringUtils.hasText(env.getProperty(provider.keyProperty()))) {
			return;
		}
		String message = "AI_PROVIDER is " + selected + " but " + provider.keyVariable() + " is empty; set it in .env";
		for (Provider other : PROVIDERS) {
			if (other != provider && StringUtils.hasText(env.getProperty(other.keyProperty()))) {
				message += "; " + other.keyVariable() + " is set, use AI_PROVIDER=" + other.name() + " to switch";
			}
		}
		throw new IllegalStateException(message);
	}
}
