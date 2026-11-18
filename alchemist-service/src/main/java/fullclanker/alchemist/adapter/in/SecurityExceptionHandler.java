package fullclanker.alchemist.adapter.in;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(SecurityExceptionHandler.class);

	/** The user lookup itself failed (e.g. the database is locked), so the credentials were never checked. */
	@ExceptionHandler(AuthenticationServiceException.class)
	ProblemDetail handleAuthenticationService(AuthenticationServiceException exception) {
		log.error("Could not check credentials", exception);
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
				"Could not check credentials, try again later");
	}

	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail handleAuthentication() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid or missing credentials");
	}

	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail handleAccessDenied() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
	}

}
