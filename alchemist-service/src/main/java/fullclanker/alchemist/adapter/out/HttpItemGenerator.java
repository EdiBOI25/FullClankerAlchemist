package fullclanker.alchemist.adapter.out;

import fullclanker.alchemist.domain.dto.GeneratedItem;
import fullclanker.alchemist.domain.exception.AiServiceUnavailableException;
import fullclanker.alchemist.domain.exception.ItemGenerationException;
import fullclanker.alchemist.domain.port.out.ItemGenerator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpConnectTimeoutException;
import java.time.Duration;
import java.util.Set;

@Component
public class HttpItemGenerator implements ItemGenerator {

	private static final Logger log = LoggerFactory.getLogger(HttpItemGenerator.class);

	private final RestClient aiService;
	private final Validator validator;

	public HttpItemGenerator(RestClient aiServiceClient, Validator validator) {
		this.aiService = aiServiceClient;
		this.validator = validator;
	}

	/** Asks ai-service for the item, retrying once when it can't be reached, and validates the reply again. */
	@Override
	public GeneratedItem generate(String nameA, String nameB) {
		long started = System.nanoTime();
		GeneratedItem item;
		try {
			item = call(nameA, nameB);
		} catch (AiServiceUnavailableException first) {
			log.warn("AI service unavailable for {} + {}; retrying", nameA, nameB, first);
			try {
				item = call(nameA, nameB);
			} catch (AiServiceUnavailableException second) {
				second.addSuppressed(first);
				throw second;
			}
		}
		if (item == null) {
			throw new ItemGenerationException("AI returned no item for %s + %s".formatted(nameA, nameB));
		}
		Set<ConstraintViolation<GeneratedItem>> violations = validator.validate(item);
		if (!violations.isEmpty()) {
			throw new ItemGenerationException("AI returned an invalid item for %s + %s".formatted(nameA, nameB),
					new ConstraintViolationException(violations));
		}
		log.info("Generated item for {} + {} -> {} (duration={} ms)", nameA, nameB, item.name(),
				Duration.ofNanos(System.nanoTime() - started).toMillis());
		return item;
	}

	private GeneratedItem call(String nameA, String nameB) {
		try {
			return aiService.post()
					.uri("/generate")
					.contentType(MediaType.APPLICATION_JSON)
					.body(new GenerateRequest(nameA, nameB))
					.retrieve()
					.body(GeneratedItem.class);
		} catch (RestClientException e) {
			if (isUnreachable(e)) {
				throw new AiServiceUnavailableException(
						"AI service is unavailable for %s + %s, try again later".formatted(nameA, nameB), e);
			}
			throw new ItemGenerationException("AI call failed for %s + %s".formatted(nameA, nameB), e);
		}
	}

	/** True when no connection to ai-service could be made: refused, unknown host or connect timeout. */
	private static boolean isUnreachable(Throwable failure) {
		// Shortcut: only the JDK client's connect timeout counts as unreachable; set spring.http.clients.imperative.factory=jdk if another HTTP library lands
		for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConnectException || cause instanceof UnknownHostException
					|| cause instanceof HttpConnectTimeoutException) {
				return true;
			}
		}
		return false;
	}

	private record GenerateRequest(String a, String b) {
	}

}
