package fullclanker.alchemist.adapter.in;

import fullclanker.alchemist.domain.exception.AiServiceUnavailableException;
import fullclanker.alchemist.domain.exception.ItemGenerationException;
import fullclanker.alchemist.domain.exception.ItemNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class CombineExceptionHandler {

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
		return problem(HttpStatus.BAD_REQUEST, "Request fields must be valid");
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
