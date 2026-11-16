package fullclanker.alchemist.config;

import fullclanker.alchemist.domain.dto.Credentials;
import fullclanker.alchemist.domain.model.Role;
import fullclanker.alchemist.domain.model.User;
import fullclanker.alchemist.domain.port.in.CreateUser;
import fullclanker.alchemist.domain.port.out.UserRepository;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.stream.Collectors;

/** Creates the admin from ADMIN_USERNAME / ADMIN_PASSWORD, or updates its password if it changed. */
@Component
@Order(2)
public class AdminSeeder implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

	private final CreateUser createUser;
	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final Validator validator;
	private final String username;
	private final String password;

	public AdminSeeder(CreateUser createUser, UserRepository users, PasswordEncoder passwordEncoder, Validator validator,
			@Value("${alchemist.admin.username}") String username, @Value("${alchemist.admin.password}") String password) {
		this.createUser = createUser;
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.validator = validator;
		this.username = username;
		this.password = password;
	}

	@Override
	public void run(String... args) {
		if (username.isEmpty() && password.isEmpty()) {
			log.info("ADMIN_USERNAME and ADMIN_PASSWORD are empty; no admin created or updated");
			return;
		}
		if (username.isEmpty() || password.isEmpty()) {
			String missing = username.isEmpty() ? "ADMIN_USERNAME" : "ADMIN_PASSWORD";
			throw new IllegalStateException(missing + " is empty; set both ADMIN_USERNAME and ADMIN_PASSWORD in .env, or neither");
		}

		Credentials credentials = new Credentials(username, password);
		String violations = validator.validate(credentials).stream()
				.map(violation -> "ADMIN_" + violation.getPropertyPath().toString().toUpperCase(Locale.ROOT) + " "
						+ violation.getMessage())
				.sorted()
				.collect(Collectors.joining("; "));
		if (!violations.isEmpty()) {
			throw new IllegalStateException(violations);
		}

		users.findByUsernameIgnoreCase(username).ifPresentOrElse(this::update, () -> {
			createUser.create(credentials, Role.ADMIN);
			log.info("Admin {} created", username);
		});
	}

	private void update(User user) {
		if (user.getRole() != Role.ADMIN) {
			log.warn("ADMIN_USERNAME {} belongs to a normal user; that user is left unchanged and no admin is created",
					user.getUsername());
			return;
		}
		if (!passwordEncoder.matches(password, user.getPassword())) {
			user.changePassword(passwordEncoder.encode(password));
			users.save(user);
			log.info("Admin {} password updated from ADMIN_PASSWORD", user.getUsername());
		}
	}

}
