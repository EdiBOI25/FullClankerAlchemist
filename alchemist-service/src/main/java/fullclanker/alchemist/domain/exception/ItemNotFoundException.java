package fullclanker.alchemist.domain.exception;

public class ItemNotFoundException extends RuntimeException {

	public ItemNotFoundException(String name) {
		super("Unknown item: %s".formatted(name));
	}

}
