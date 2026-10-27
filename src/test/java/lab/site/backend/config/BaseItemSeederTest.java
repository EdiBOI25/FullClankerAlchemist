package lab.site.backend.config;

import lab.site.backend.domain.model.Item;
import lab.site.backend.domain.port.out.ItemRepository;
import lab.site.backend.domain.port.out.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
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

}
