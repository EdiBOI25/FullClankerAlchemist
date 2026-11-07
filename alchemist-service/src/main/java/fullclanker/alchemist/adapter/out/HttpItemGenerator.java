package fullclanker.alchemist.adapter.out;

import fullclanker.alchemist.domain.dto.GeneratedItem;
import fullclanker.alchemist.domain.exception.ItemGenerationException;
import fullclanker.alchemist.domain.port.out.ItemGenerator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Set;

@Component
public class HttpItemGenerator implements ItemGenerator {

	private final RestClient aiService;
	private final Validator validator;

	public HttpItemGenerator(RestClient aiServiceClient, Validator validator) {
		this.aiService = aiServiceClient;
		this.validator = validator;
	}

	/** Asks ai-service for the item and validates the reply again. */
	@Override
	public GeneratedItem generate(String nameA, String nameB) {
		GeneratedItem item;
		try {
			item = aiService.post()
					.uri("/generate")
					.contentType(MediaType.APPLICATION_JSON)
					.body(new GenerateRequest(nameA, nameB))
					.retrieve()
					.body(GeneratedItem.class);
		} catch (RestClientException e) {
			throw new ItemGenerationException("AI call failed for %s + %s".formatted(nameA, nameB), e);
		}
		if (item == null) {
			throw new ItemGenerationException("AI returned no item for %s + %s".formatted(nameA, nameB));
		}
		Set<ConstraintViolation<GeneratedItem>> violations = validator.validate(item);
		if (!violations.isEmpty()) {
			throw new ItemGenerationException("AI returned an invalid item for %s + %s".formatted(nameA, nameB),
					new ConstraintViolationException(violations));
		}
		return item;
	}

	private record GenerateRequest(String a, String b) {
	}

}
