package fullclanker.alchemist.adapter.in;

import fullclanker.alchemist.domain.dto.Credentials;
import fullclanker.alchemist.domain.dto.UserDto;
import fullclanker.alchemist.domain.model.Role;
import fullclanker.alchemist.domain.port.in.CreateUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final CreateUser createUser;

	public AuthController(CreateUser createUser) {
		this.createUser = createUser;
	}

	/** Creates a USER account; doesn't log in. */
	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public UserDto signUp(@Valid @RequestBody Credentials credentials) {
		createUser.create(credentials, Role.USER);
		return new UserDto(credentials.username(), Role.USER);
	}

	/** The logged-in user. */
	@GetMapping("/me")
	public UserDto me(@AuthenticationPrincipal UserDetails user) {
		Role role = user.getAuthorities().stream()
				.map(authority -> Role.valueOf(authority.getAuthority().substring("ROLE_".length())))
				.findFirst()
				.orElseThrow();
		return new UserDto(user.getUsername(), role);
	}

}
