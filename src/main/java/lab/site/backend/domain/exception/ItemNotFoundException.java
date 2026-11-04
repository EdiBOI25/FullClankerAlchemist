package lab.site.backend.domain.exception;

public class ItemNotFoundException extends RuntimeException {

	public ItemNotFoundException(String name) {
		super("Unknown item: %s".formatted(name));
	}

}
