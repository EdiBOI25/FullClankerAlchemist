package fullclanker.alchemist.application;

import fullclanker.alchemist.domain.dto.Credentials;
import fullclanker.alchemist.domain.exception.UsernameTakenException;
import fullclanker.alchemist.domain.model.Role;
import fullclanker.alchemist.domain.model.User;
import fullclanker.alchemist.domain.port.in.CreateUser;
import fullclanker.alchemist.domain.port.out.UserRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserServiceTest {

	private static final String PASSWORD = "password1";

	@Autowired
	private CreateUser createUser;

	@Autowired
	private UserRepository users;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void storedPasswordIsABcryptHashOfTheRawOne() {
		createUser.create(new Credentials("Alice", PASSWORD), Role.USER);

		User alice = users.findByUsernameIgnoreCase("alice").orElseThrow();
		assertThat(alice.getUsername()).isEqualTo("Alice");
		assertThat(alice.getPassword()).startsWith("$2a$10$").doesNotContain(PASSWORD);
		assertThat(passwordEncoder.matches(PASSWORD, alice.getPassword())).isTrue();
		assertThat(passwordEncoder.matches("password2", alice.getPassword())).isFalse();
	}

	@ParameterizedTest
	@EnumSource(Role.class)
	void roleIsStored(Role role) {
		createUser.create(new Credentials("alice", PASSWORD), role);

		assertThat(users.findByUsernameIgnoreCase("alice").orElseThrow().getRole()).isEqualTo(role);
	}

	@Test
	void takenNameInDifferentCaseIsRejected() {
		createUser.create(new Credentials("alice", PASSWORD), Role.USER);
		String firstHash = users.findByUsernameIgnoreCase("alice").orElseThrow().getPassword();

		assertThatThrownBy(() -> createUser.create(new Credentials("ALICE", "password2"), Role.ADMIN))
				.isInstanceOf(UsernameTakenException.class)
				.hasMessage("Username already taken: ALICE");
		assertThat(users.findAll()).singleElement()
				.extracting(User::getUsername, User::getPassword, User::getRole)
				.containsExactly("alice", firstHash, Role.USER);
	}

	@Test
	void invalidUsernameIsRejected() {
		assertThatThrownBy(() -> createUser.create(new Credentials("ab", PASSWORD), Role.USER))
				.isInstanceOf(ConstraintViolationException.class)
				.hasMessageContaining("username");
		assertThat(users.findAll()).isEmpty();
	}

	@Test
	void tooLongPasswordIsRejectedBeforeHashing() {
		assertThatThrownBy(() -> createUser.create(new Credentials("alice", "a".repeat(73)), Role.USER))
				.isInstanceOf(ConstraintViolationException.class)
				.hasMessageContaining("password");
		assertThat(users.findAll()).isEmpty();
	}

	@Test
	void missingArgumentsAreRejected() {
		assertThatThrownBy(() -> createUser.create(null, Role.USER))
				.isInstanceOf(ConstraintViolationException.class);
		assertThatThrownBy(() -> createUser.create(new Credentials("alice", PASSWORD), null))
				.isInstanceOf(ConstraintViolationException.class);
		assertThat(users.findAll()).isEmpty();
	}

}
