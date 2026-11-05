package fullclanker.alchemist.domain.exception;

public class ItemGenerationException extends RuntimeException {

	public ItemGenerationException(String message) {
		super(message);
	}

	public ItemGenerationException(String message, Throwable cause) {
		super(message, cause);
	}

}
