package fullclanker.alchemist.domain.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CredentialsTest {

	private static final String VALID_USERNAME = "alice";
	private static final String VALID_PASSWORD = "password";

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	private List<String> invalidFields(String username, String password) {
		return validator.validate(new Credentials(username, password)).stream()
				.map(ConstraintViolation::getPropertyPath)
				.map(Object::toString)
				.toList();
	}

	@Test
	void validCredentialsPass() {
		assertThat(invalidFields(VALID_USERNAME, VALID_PASSWORD)).isEmpty();
	}

	@Test
	void missingValuesAreRejected() {
		assertThat(invalidFields(null, VALID_PASSWORD)).containsExactly("username");
		assertThat(invalidFields(VALID_USERNAME, null)).containsExactly("password");
	}

	@ParameterizedTest
	@ValueSource(strings = {"abc", "A-z_0-9", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
	void validUsernamePasses(String username) {
		assertThat(invalidFields(username, VALID_PASSWORD)).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "ab", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "a b", "aé1", "a.b", "abc\n"})
	void invalidUsernameIsRejected(String username) {
		assertThat(invalidFields(username, VALID_PASSWORD)).containsExactly("username");
	}

	@Test
	void passwordOf8CharactersPasses() {
		assertThat(invalidFields(VALID_USERNAME, "a".repeat(8))).isEmpty();
	}

	@Test
	void passwordOf7CharactersIsRejected() {
		assertThat(invalidFields(VALID_USERNAME, "a".repeat(7))).containsExactly("password");
	}

	@Test
	void passwordOf72AsciiCharactersPasses() {
		assertThat(invalidFields(VALID_USERNAME, "a".repeat(72))).isEmpty();
	}

	@Test
	void passwordOf73AsciiCharactersIsRejected() {
		assertThat(invalidFields(VALID_USERNAME, "a".repeat(73))).containsExactly("password");
	}

	@Test
	void passwordOf72BytesWithAccentsPasses() {
		assertThat(invalidFields(VALID_USERNAME, "é".repeat(36))).isEmpty();
	}

	@Test
	void passwordOf74BytesWithAccentsIsRejected() {
		assertThat(invalidFields(VALID_USERNAME, "é".repeat(37))).containsExactly("password");
	}

	@Test
	void fourEmojiCountAsFourCharactersAndAreRejected() {
		assertThat(invalidFields(VALID_USERNAME, "😀".repeat(4))).containsExactly("password");
	}

	@Test
	void eighteenEmojiFillExactly72BytesAndPass() {
		assertThat(invalidFields(VALID_USERNAME, "😀".repeat(18))).isEmpty();
	}

	@Test
	void nineteenEmojiExceed72BytesAndAreRejected() {
		assertThat(invalidFields(VALID_USERNAME, "😀".repeat(19))).containsExactly("password");
	}

	@Test
	void toStringHidesThePassword() {
		assertThat(new Credentials(VALID_USERNAME, "hunter22secret").toString())
				.contains(VALID_USERNAME)
				.doesNotContain("hunter22secret");
	}

	@Test
	void bothInvalidFieldsAreReported() {
		assertThat(invalidFields("ab", "short")).containsExactlyInAnyOrder("username", "password");
	}

}
