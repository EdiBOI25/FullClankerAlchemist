package fullclanker.alchemist.adapter.in;

import fullclanker.alchemist.domain.dto.Credentials;
import fullclanker.alchemist.domain.dto.UserDto;
import fullclanker.alchemist.domain.model.Role;
import fullclanker.alchemist.domain.port.in.CreateUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
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
	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository sessions = new HttpSessionSecurityContextRepository();

	public AuthController(CreateUser createUser, AuthenticationManager authenticationManager) {
		this.createUser = createUser;
		this.authenticationManager = authenticationManager;
	}

	/** Creates a USER account; doesn't log in. */
	@PostMapping(path = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public UserDto signUp(@Valid @RequestBody Credentials credentials) {
		return createUser.create(credentials, Role.USER);
	}

	/** Starts a session with a new id for valid credentials; a failed login touches no session. */
	@PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
	public UserDto logIn(@Valid @RequestBody Credentials credentials, HttpServletRequest request,
			HttpServletResponse response) {
		Authentication authentication = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(credentials.username(), credentials.password()));
		if (request.getSession(false) != null) {
			request.changeSessionId();
		}
		sessions.saveContext(new SecurityContextImpl(authentication), request, response);
		return toDto((UserDetails) authentication.getPrincipal());
	}

	/** The logged-in user. */
	@GetMapping("/me")
	public UserDto me(@AuthenticationPrincipal UserDetails user) {
		return toDto(user);
	}

	private static UserDto toDto(UserDetails user) {
		Role role = user.getAuthorities().stream()
				.map(authority -> Role.valueOf(authority.getAuthority().substring("ROLE_".length())))
				.findFirst()
				.orElseThrow();
		return new UserDto(user.getUsername(), role);
	}

}
