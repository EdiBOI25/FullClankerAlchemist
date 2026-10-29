package lab.site.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiProviderCheckTest {

	private final AiProviderCheck check = new AiProviderCheck();

	private static MockEnvironment env(String provider) {
		return new MockEnvironment().withProperty("spring.ai.model.chat", provider);
	}

	@Test
	void isRegistered() {
		assertThat(SpringFactoriesLoader.forDefaultResourceLocation()
				.load(EnvironmentPostProcessor.class, null, (type, name, failure) -> {}))
				.hasAtLeastOneElementOfType(AiProviderCheck.class);
	}

	@Test
	void geminiWithItsKeyStarts() {
		MockEnvironment env = env("google-genai")
				.withProperty("spring.ai.google.genai.api-key", "g");

		assertThatCode(() -> check.postProcessEnvironment(env, null)).doesNotThrowAnyException();
	}

	@Test
	void anthropicWithItsKeyStarts() {
		MockEnvironment env = env("anthropic")
				.withProperty("spring.ai.anthropic.api-key", "a");

		assertThatCode(() -> check.postProcessEnvironment(env, null)).doesNotThrowAnyException();
	}

	@Test
	void geminiWithOnlyAnthropicKeyFailsWithSwitchHint() {
		MockEnvironment env = env("google-genai")
				.withProperty("spring.ai.google.genai.api-key", "")
				.withProperty("spring.ai.anthropic.api-key", "a");

		assertThatThrownBy(() -> check.postProcessEnvironment(env, null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("GEMINI_API_KEY is empty")
				.hasMessageContaining("use AI_PROVIDER=anthropic");
	}

	@Test
	void anthropicWithoutKeyFails() {
		MockEnvironment env = env("anthropic");

		assertThatThrownBy(() -> check.postProcessEnvironment(env, null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("ANTHROPIC_API_KEY is empty")
				.hasMessageNotContaining("to switch");
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "gemini", "anthropic "})
	void unknownProviderFails(String provider) {
		MockEnvironment env = env(provider)
				.withProperty("spring.ai.google.genai.api-key", "g")
				.withProperty("spring.ai.anthropic.api-key", "a");

		assertThatThrownBy(() -> check.postProcessEnvironment(env, null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("AI_PROVIDER is '" + provider + "'")
				.hasMessageEndingWith("use google-genai or anthropic");
	}
}
