package fullclanker.alchemist.config;

import fullclanker.alchemist.domain.model.Item;
import fullclanker.alchemist.domain.model.Role;
import fullclanker.alchemist.domain.model.Tag;
import fullclanker.alchemist.domain.model.User;
import fullclanker.alchemist.domain.model.UserItem;
import fullclanker.alchemist.domain.port.out.ItemRepository;
import fullclanker.alchemist.domain.port.out.TagRepository;
import fullclanker.alchemist.domain.port.out.UserItemRepository;
import fullclanker.alchemist.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BaseItemSeederTest {

	@Autowired
	private BaseItemSeeder seeder;

	@Autowired
	private ItemRepository items;

	@Autowired
	private TagRepository tags;

	@Autowired
	private UserRepository users;

	@Autowired
	private UserItemRepository userItems;

	@BeforeEach
	void clearDatabase() {
		userItems.deleteAll();
		users.deleteAll();
		items.deleteAll();
		tags.deleteAll();
		items.flush();
		tags.flush();
	}

	@Test
	void seedsBaseItemsAndIsIdempotent() {
		seeder.run();

		assertThat(items.findAll())
				.hasSize(4)
				.allSatisfy(item -> {
					assertThat(item.getEmoji()).isNotBlank();
					assertThat(item.getTags()).isNotEmpty();
				})
				.extracting(Item::getName)
				.containsExactlyInAnyOrder("fire", "water", "earth", "air");

		seeder.run();

		assertThat(items.findAll())
				.hasSize(4)
				.extracting(Item::getName)
				.containsExactlyInAnyOrder("fire", "water", "earth", "air");
		assertThat(tags.findAll()).hasSize(4);
	}

	@Test
	void seedsRemainingBaseItemsWhenSomeAlreadyExist() {
		Tag hot = tags.save(new Tag("hot"));
		items.save(new Item("fire", "🔥", Set.of(hot)));

		seeder.run();

		assertThat(items.findAll())
				.hasSize(4)
				.extracting(Item::getName)
				.containsExactlyInAnyOrder("fire", "water", "earth", "air");
		assertThat(tags.findAll()).hasSize(4);
	}

	@Test
	void existingUserWithoutItemsGetsTheBaseItems() {
		seeder.run();
		User alice = users.save(new User("alice", "h".repeat(60), Role.USER));

		seeder.run();

		assertThat(userItems.findAll())
				.extracting(UserItem::getUser, userItem -> userItem.getItem().getName())
				.containsExactlyInAnyOrder(
						tuple(alice, "fire"),
						tuple(alice, "water"),
						tuple(alice, "earth"),
						tuple(alice, "air"));
	}

	@Test
	void userMissingSomeBaseItemsGetsOnlyTheMissingOnes() {
		seeder.run();
		User alice = users.save(new User("alice", "h".repeat(60), Role.USER));
		userItems.save(new UserItem(alice, items.findByNameIgnoreCase("fire").orElseThrow()));

		seeder.run();

		assertThat(userItems.findAll())
				.extracting(userItem -> userItem.getItem().getName())
				.containsExactlyInAnyOrder("fire", "water", "earth", "air");
	}

}
