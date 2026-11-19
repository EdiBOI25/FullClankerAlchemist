package fullclanker.alchemist.config;

import fullclanker.alchemist.domain.dto.Credentials;
import fullclanker.alchemist.domain.model.Role;
import fullclanker.alchemist.domain.model.User;
import fullclanker.alchemist.domain.port.in.CreateUser;
import fullclanker.alchemist.domain.port.out.UserRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AdminSeederTest {

	private static final String PASSWORD = "password1";
	private static final String NEW_PASSWORD = "password2";

	@Autowired
	private CreateUser createUser;

	@Autowired
	private UserRepository users;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private Validator validator;

	@AfterEach
	void clearUsers() {
		users.deleteAll();
	}

	private void seed(String username, String password) {
		new AdminSeeder(createUser, users, passwordEncoder, validator, username, password).run();
	}

	private User user(String username) {
		return users.findByUsernameIgnoreCase(username).orElseThrow();
	}

	@Test
	void bothEmptyCreatesNoAdmin() {
		seed("", "");

		assertThat(users.findAll()).isEmpty();
	}

	@Test
	void onlyUsernameFailsNamingThePassword() {
		assertThatThrownBy(() -> seed("boss", ""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageStartingWith("ADMIN_PASSWORD is empty");
		assertThat(users.findAll()).isEmpty();
	}

	@Test
	void onlyPasswordFailsNamingTheUsername() {
		assertThatThrownBy(() -> seed("", PASSWORD))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageStartingWith("ADMIN_USERNAME is empty");
		assertThat(users.findAll()).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {" ", "ab", "bo ss", "boss!", "bossé"})
	void invalidUsernameFailsNamingIt(String username) {
		assertThatThrownBy(() -> seed(username, PASSWORD))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("ADMIN_USERNAME must be 3 to 30 characters: a-z, A-Z, 0-9, _ or -");
		assertThat(users.findAll()).isEmpty();
	}

	static Stream<String> invalidPasswords() {
		return Stream.of("   ", "a".repeat(7), "a".repeat(73), "é".repeat(37));
	}

	@ParameterizedTest
	@MethodSource("invalidPasswords")
	void invalidPasswordFailsNamingItWithoutShowingIt(String password) {
		assertThatThrownBy(() -> seed("boss", password))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageStartingWith("ADMIN_PASSWORD must be")
				.hasMessageNotContaining(password);
		assertThat(users.findAll()).isEmpty();
	}

	@Test
	void bothInvalidValuesAreNamed() {
		assertThatThrownBy(() -> seed("ab", "short"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("ADMIN_USERNAME")
				.hasMessageContaining("ADMIN_PASSWORD");
	}

	@Test
	void freeNameCreatesTheAdmin() {
		seed("boss", PASSWORD);

		User boss = user("boss");
		assertThat(boss.getRole()).isEqualTo(Role.ADMIN);
		assertThat(passwordEncoder.matches(PASSWORD, boss.getPassword())).isTrue();
	}

	@Test
	void unchangedPasswordKeepsTheHash() {
		seed("boss", PASSWORD);
		String hash = user("boss").getPassword();

		seed("boss", PASSWORD);

		assertThat(users.findAll()).singleElement()
				.extracting(User::getPassword)
				.isEqualTo(hash);
	}

	@Test
	void changedPasswordIsRehashed() {
		seed("boss", PASSWORD);

		seed("boss", NEW_PASSWORD);

		String hash = user("boss").getPassword();
		assertThat(passwordEncoder.matches(NEW_PASSWORD, hash)).isTrue();
		assertThat(passwordEncoder.matches(PASSWORD, hash)).isFalse();
	}

	@Test
	void existingAdminInDifferentCaseIsUpdatedNotDuplicated() {
		seed("Boss", PASSWORD);

		seed("boss", NEW_PASSWORD);

		assertThat(users.findAll()).singleElement().satisfies(admin -> {
			assertThat(admin.getUsername()).isEqualTo("Boss");
			assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
			assertThat(passwordEncoder.matches(NEW_PASSWORD, admin.getPassword())).isTrue();
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {"bob", "BOB"})
	void normalUserWithTheNameIsLeftUnchanged(String adminName) {
		createUser.create(new Credentials("bob", PASSWORD), Role.USER);
		String hash = user("bob").getPassword();

		seed(adminName, NEW_PASSWORD);

		assertThat(users.findAll()).singleElement()
				.extracting(User::getUsername, User::getPassword, User::getRole)
				.containsExactly("bob", hash, Role.USER);
	}

}
