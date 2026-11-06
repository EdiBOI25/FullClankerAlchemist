package fullclanker.ai.controller;

import fullclanker.ai.exception.ItemGenerationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GenerateExceptionHandler {

	/** Answers with our own message only; the cause may hold the provider's error text. */
	@ExceptionHandler(ItemGenerationException.class)
	ProblemDetail handleItemGeneration(ItemGenerationException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleInvalidRequest(MethodArgumentNotValidException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request fields must be valid");
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail handleUnreadableRequest(HttpMessageNotReadableException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request body must be valid JSON");
	}

}
