package lab.site.backend.adapter.in;

import lab.site.backend.domain.exception.ItemGenerationException;
import lab.site.backend.domain.exception.ItemNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleInvalidRequest(MethodArgumentNotValidException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Request fields must be valid");
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail handleUnreadableRequest(HttpMessageNotReadableException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Request body must be valid JSON");
	}

	private ProblemDetail problem(HttpStatus status, String detail) {
		return ProblemDetail.forStatusAndDetail(status, detail);
	}

}
