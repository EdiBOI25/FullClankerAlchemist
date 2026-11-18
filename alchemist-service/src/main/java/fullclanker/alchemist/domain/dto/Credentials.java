package fullclanker.alchemist.domain.dto;

import fullclanker.alchemist.domain.model.User;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record Credentials(
		@NotNull @Pattern(regexp = User.USERNAME_PATTERN,
				message = "must be 3 to 30 characters: a-z, A-Z, 0-9, _ or -") String username,
		@NotNull @Password String password) {

	/** Masks the password so logs and exception messages never show it. */
	@Override
	public String toString() {
		return "Credentials[username=" + username + ", password=***]";
	}

}
