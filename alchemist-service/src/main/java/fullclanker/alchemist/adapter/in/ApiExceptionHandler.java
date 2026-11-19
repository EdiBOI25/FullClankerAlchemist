package fullclanker.alchemist.adapter.in;

import fullclanker.alchemist.domain.exception.AiServiceUnavailableException;
import fullclanker.alchemist.domain.exception.ItemGenerationException;
import fullclanker.alchemist.domain.exception.ItemNotFoundException;
import fullclanker.alchemist.domain.exception.UsernameTakenException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	/** The user lookup itself failed (e.g. the database is locked), so the credentials were never checked. */
	@ExceptionHandler(AuthenticationServiceException.class)
	ProblemDetail handleAuthenticationService(AuthenticationServiceException exception) {
		log.error("Could not check credentials", exception);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Could not check credentials, try again later");
	}

	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail handleAuthentication() {
		return problem(HttpStatus.UNAUTHORIZED, "Invalid or missing credentials");
	}

	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail handleAccessDenied() {
		return problem(HttpStatus.FORBIDDEN, "Access denied");
	}

	@ExceptionHandler(UsernameTakenException.class)
	ProblemDetail handleUsernameTaken(UsernameTakenException exception) {
		return problem(HttpStatus.CONFLICT, exception.getMessage());
	}

	@ExceptionHandler(ItemNotFoundException.class)
	ProblemDetail handleItemNotFound(ItemNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	@ExceptionHandler(ItemGenerationException.class)
	ProblemDetail handleItemGeneration(ItemGenerationException exception) {
		return problem(HttpStatus.BAD_GATEWAY, exception.getMessage());
	}

	@ExceptionHandler(AiServiceUnavailableException.class)
	ProblemDetail handleAiServiceUnavailable(AiServiceUnavailableException exception) {
		return problem(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleInvalidRequest(MethodArgumentNotValidException exception) {
		String fields = exception.getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.sorted()
				.collect(Collectors.joining("; "));
		return problem(HttpStatus.BAD_REQUEST, fields);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ProblemDetail handleInvalidMethodArgument(HandlerMethodValidationException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Request fields must be valid");
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Request fields must be valid");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
		return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail handleUnreadableRequest(HttpMessageNotReadableException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Request body must be valid JSON");
	}

	private ProblemDetail problem(HttpStatus status, String detail) {
		return ProblemDetail.forStatusAndDetail(status, detail);
	}

}
