package fullclanker.alchemist.domain.exception;

public class UsernameTakenException extends RuntimeException {

	public UsernameTakenException(String username) {
		super("Username already taken: %s".formatted(username));
	}

}
