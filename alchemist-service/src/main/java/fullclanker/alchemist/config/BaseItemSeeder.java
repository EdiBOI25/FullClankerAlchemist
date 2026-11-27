package fullclanker.alchemist.config;

import fullclanker.alchemist.domain.model.Item;
import fullclanker.alchemist.domain.model.Tag;
import fullclanker.alchemist.domain.port.out.ItemRepository;
import fullclanker.alchemist.domain.port.out.TagRepository;
import fullclanker.alchemist.domain.port.out.UserItemRepository;
import fullclanker.alchemist.domain.port.out.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/** Creates the 4 base items if missing, then gives them to every user who lacks one (users created before #61). */
@Component
@Order(1)
public class BaseItemSeeder implements CommandLineRunner {

	private final ItemRepository items;
	private final TagRepository tags;
	private final UserRepository users;
	private final UserItemRepository userItems;

	public BaseItemSeeder(ItemRepository items, TagRepository tags, UserRepository users,
			UserItemRepository userItems) {
		this.items = items;
		this.tags = tags;
		this.users = users;
		this.userItems = userItems;
	}

	@Override
	@Transactional
	public void run(String... args) {
		List<Item> baseItems = Item.BASE_ITEMS.stream()
				.map(base -> seed(base.name(), base.emoji(), base.tag()))
				.toList();
		users.findAll().forEach(user -> baseItems.forEach(item -> userItems.addIfMissing(user, item)));
	}

	/** Returns the base item, creating it if missing. */
	private Item seed(String itemName, String emoji, String tagName) {
		return items.findByNameIgnoreCase(itemName).orElseGet(() -> {
			Tag tag = tags.findByName(tagName)
					.orElseGet(() -> tags.save(new Tag(tagName)));
			return items.save(new Item(itemName, emoji, Set.of(tag)));
		});
	}

}
