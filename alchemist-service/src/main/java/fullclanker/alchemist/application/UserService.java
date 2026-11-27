package fullclanker.alchemist.application;

import fullclanker.alchemist.domain.dto.Credentials;
import fullclanker.alchemist.domain.dto.UserDto;
import fullclanker.alchemist.domain.exception.UsernameTakenException;
import fullclanker.alchemist.domain.model.Item;
import fullclanker.alchemist.domain.model.Role;
import fullclanker.alchemist.domain.model.User;
import fullclanker.alchemist.domain.port.in.CreateUser;
import fullclanker.alchemist.domain.port.out.ItemRepository;
import fullclanker.alchemist.domain.port.out.UserItemRepository;
import fullclanker.alchemist.domain.port.out.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class UserService implements CreateUser {

	private final UserRepository users;
	private final ItemRepository items;
	private final UserItemRepository userItems;
	private final PasswordEncoder passwordEncoder;
	private final TransactionTemplate transactions;

	public UserService(UserRepository users, ItemRepository items, UserItemRepository userItems,
			PasswordEncoder passwordEncoder, TransactionTemplate transactions) {
		this.users = users;
		this.items = items;
		this.userItems = userItems;
		this.passwordEncoder = passwordEncoder;
		this.transactions = transactions;
	}

	/** Creates the user with the 4 base items in their collection. */
	@Override
	public UserDto create(Credentials credentials, Role role) {
		String hash = passwordEncoder.encode(credentials.password());
		User saved = transactions.execute(status -> {
			if (users.findByUsernameIgnoreCase(credentials.username()).isPresent()) {
				throw new UsernameTakenException(credentials.username());
			}
			User user = users.save(new User(credentials.username(), hash, role));
			Item.BASE_NAMES.forEach(name -> items.findByNameIgnoreCase(name)
					.ifPresent(item -> userItems.addIfMissing(user, item)));
			return user;
		});
		return new UserDto(saved.getUsername(), saved.getRole());
	}

}
