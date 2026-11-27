package fullclanker.alchemist;

import fullclanker.alchemist.domain.dto.Credentials;
import fullclanker.alchemist.domain.model.Role;
import fullclanker.alchemist.domain.port.in.CreateUser;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Creates the users "player" and "other" at startup; every MockMvc request logs in as "player" with HTTP Basic. */
@TestConfiguration
public class TestPlayer {

	public static final String USERNAME = "player";
	public static final String OTHER_USERNAME = "other";
	public static final String PASSWORD = "password1";

	/** The Authorization header value that logs in as the given test user, e.g. {@link #OTHER_USERNAME}. */
	public static String basicAuth(String username) {
		return "Basic " + HttpHeaders.encodeBasicAuth(username, PASSWORD, StandardCharsets.UTF_8);
	}

	@Bean
	CommandLineRunner createTestPlayer(CreateUser createUser) {
		return args -> {
			createUser.create(new Credentials(USERNAME, PASSWORD), Role.USER);
			createUser.create(new Credentials(OTHER_USERNAME, PASSWORD), Role.USER);
		};
	}

	// Shortcut: no .with(...) login (httpBasic, user, anonymous) or @WithMockUser overrides this; use .header(AUTHORIZATION, basicAuth(...)).
	@Bean
	MockMvcBuilderCustomizer logInAsTestPlayer() {
		return builder -> builder.defaultRequest(get("/").with(httpBasic(USERNAME, PASSWORD)));
	}

}
