package fullclanker.alchemist.config;

import fullclanker.alchemist.domain.model.Role;
import fullclanker.alchemist.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"alchemist.admin.username=boss", "alchemist.admin.password=password1"})
class AdminStartupTest {

	@Autowired
	private UserRepository users;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void adminFromPropertiesExistsAfterStartup() {
		assertThat(users.findByUsernameIgnoreCase("boss")).get().satisfies(boss -> {
			assertThat(boss.getRole()).isEqualTo(Role.ADMIN);
			assertThat(passwordEncoder.matches("password1", boss.getPassword())).isTrue();
		});
	}

}
