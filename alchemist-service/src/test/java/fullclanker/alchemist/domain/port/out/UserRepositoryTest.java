package fullclanker.alchemist.domain.port.out;

import fullclanker.alchemist.domain.model.Role;
import fullclanker.alchemist.domain.model.User;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class UserRepositoryTest {

	private static final String HASH = "h".repeat(60);
	private static final String INSERT = "insert into user (username, password, role) values (?, ?, ?)";

	@Autowired
	private UserRepository users;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void usernameIsUniqueIgnoringCase() {
		users.saveAndFlush(new User("alice", HASH, Role.USER));

		assertThatThrownBy(() -> users.saveAndFlush(new User("Alice", HASH, Role.USER)))
				.isInstanceOf(DataAccessException.class)
				.hasMessageContaining("UNIQUE constraint failed: user.username");
	}

	@Test
	void userIsFoundIgnoringCaseAndKeepsTheTypedName() {
		users.saveAndFlush(new User("Alice", HASH, Role.ADMIN));

		assertThat(users.findByUsernameIgnoreCase("aLICE")).get()
				.extracting(User::getUsername, User::getPassword, User::getRole)
				.containsExactly("Alice", HASH, Role.ADMIN);
		assertThat(users.findByUsernameIgnoreCase("bob")).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {"abc", "A-z_0-9", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
	void validUsernameIsSaved(String username) {
		assertThatCode(() -> users.saveAndFlush(new User(username, HASH, Role.USER)))
				.doesNotThrowAnyException();
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "ab", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "a b", "aé1", "a.b", "abc\n"})
	void invalidUsernameIsRejectedBeforeInsert(String username) {
		assertThatThrownBy(() -> users.saveAndFlush(new User(username, HASH, Role.USER)))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "ab", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "a b", "aé1", "a.b", "abc\n"})
	void invalidUsernameInsertedWithSqlFails(String username) {
		assertThatThrownBy(() -> jdbc.update(INSERT, username, HASH, "USER"))
				.isInstanceOf(DataAccessException.class)
				.hasMessageContaining("CHECK constraint failed");
	}

	@Test
	void missingFieldIsRejectedBeforeInsert() {
		assertThatThrownBy(() -> users.saveAndFlush(new User(null, HASH, Role.USER)))
				.isInstanceOf(ConstraintViolationException.class);
		assertThatThrownBy(() -> users.saveAndFlush(new User("alice", null, Role.USER)))
				.isInstanceOf(ConstraintViolationException.class);
		assertThatThrownBy(() -> users.saveAndFlush(new User("alice", HASH, null)))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@ParameterizedTest
	@ValueSource(ints = {8, 59, 61})
	void passwordThatIsNotAHashIsRejectedBeforeInsert(int length) {
		assertThatThrownBy(() -> users.saveAndFlush(new User("alice", "h".repeat(length), Role.USER)))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@ParameterizedTest
	@ValueSource(ints = {8, 59, 61})
	void passwordThatIsNotAHashInsertedWithSqlFails(int length) {
		assertThatThrownBy(() -> jdbc.update(INSERT, "alice", "h".repeat(length), "USER"))
				.isInstanceOf(DataAccessException.class)
				.hasMessageContaining("CHECK constraint failed");
	}

	@Test
	void unknownRoleInsertedWithSqlFails() {
		assertThatThrownBy(() -> jdbc.update(INSERT, "alice", HASH, "ROOT"))
				.isInstanceOf(DataAccessException.class)
				.hasMessageContaining("CHECK constraint failed");
	}

	@Test
	void validRowInsertedWithSqlIsAccepted() {
		assertThatCode(() -> jdbc.update(INSERT, "alice", HASH, "ADMIN")).doesNotThrowAnyException();
	}

}
