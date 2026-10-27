package lab.site.backend.config;

import lab.site.backend.domain.model.Item;
import lab.site.backend.domain.model.Tag;
import lab.site.backend.domain.port.out.ItemRepository;
import lab.site.backend.domain.port.out.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

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

	@BeforeEach
	void clearDatabase() {
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

}
