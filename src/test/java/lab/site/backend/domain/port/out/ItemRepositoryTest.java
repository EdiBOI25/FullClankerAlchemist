package lab.site.backend.domain.port.out;

import jakarta.persistence.EntityManager;
import lab.site.backend.domain.model.Item;
import lab.site.backend.domain.model.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.datasource.url=jdbc:sqlite:file::memory:?cache=shared&foreign_keys=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ItemRepositoryTest {

	@Autowired
	private ItemRepository items;

	@Autowired
	private TagRepository tags;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void itemNameIsUniqueIgnoringCase() {
		items.saveAndFlush(new Item("fire", "🔥", Set.of()));

		assertThatThrownBy(() -> items.saveAndFlush(new Item("Fire", "🔥", Set.of())))
				.isInstanceOf(DataAccessException.class)
				.hasMessageContaining("UNIQUE constraint failed: item.name");
	}

	@Test
	void tagIsStoredLowercaseAndLinkedToItem() {
		Tag hot = tags.save(new Tag("Hot"));
		Item fire = items.saveAndFlush(new Item("fire", "🔥", Set.of(hot)));
		entityManager.clear();

		assertThat(items.findById(fire.getId()).orElseThrow().getTags())
				.extracting(Tag::getName)
				.containsExactly("hot");
	}

	@Test
	void itemTagRowForMissingItemFails() {
		Tag hot = tags.saveAndFlush(new Tag("hot"));

		assertThatThrownBy(() -> jdbc.update("insert into item_tag (item_id, tag_id) values (999, ?)", hot.getId()))
				.isInstanceOf(DataAccessException.class)
				.hasMessageContaining("FOREIGN KEY constraint failed");
	}

}
