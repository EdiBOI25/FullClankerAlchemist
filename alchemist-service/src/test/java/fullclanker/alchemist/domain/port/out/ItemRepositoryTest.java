package fullclanker.alchemist.domain.port.out;

import jakarta.persistence.EntityManager;
import fullclanker.alchemist.domain.model.Item;
import fullclanker.alchemist.domain.model.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

@DataJpaTest
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
	void validItemAndTagAreSaved() {
		Tag tag = tags.save(new Tag("valid"));

		assertThatCode(() -> items.saveAndFlush(new Item("valid item", "emoji", Set.of(tag))))
				.doesNotThrowAnyException();
		assertThat(tag.getId()).isNotNull();
	}

	@Test
	void blankItemNameIsRejectedBeforeInsert() {
		assertThatThrownBy(() -> items.saveAndFlush(new Item(" ", "🔥", Set.of())))
				.isInstanceOf(jakarta.validation.ConstraintViolationException.class);
	}

	@Test
	void overlongItemNameIsRejectedBeforeInsert() {
		assertThatThrownBy(() -> items.saveAndFlush(new Item("a".repeat(51), "🔥", Set.of())))
				.isInstanceOf(jakarta.validation.ConstraintViolationException.class);
	}

	@Test
	void blankTagNameIsRejectedBeforeInsert() {
		assertThatThrownBy(() -> tags.saveAndFlush(new Tag(" ")))
				.isInstanceOf(jakarta.validation.ConstraintViolationException.class);
	}

	@Test
	void overlongTagNameIsRejectedBeforeInsert() {
		assertThatThrownBy(() -> tags.saveAndFlush(new Tag("a".repeat(31))))
				.isInstanceOf(jakarta.validation.ConstraintViolationException.class);
	}

	@Test
	void itemWithEmojiAtTheLimitIsSaved() {
		assertThatCode(() -> items.saveAndFlush(new Item("family", "👨‍👩‍👧‍👦" + "a".repeat(5), Set.of())))
				.doesNotThrowAnyException();
	}

	@Test
	void itemWithTooLongEmojiIsRejectedBeforeInsert() {
		assertThatThrownBy(() -> items.saveAndFlush(new Item("valid", "12345678901234567", Set.of())))
				.isInstanceOf(jakarta.validation.ConstraintViolationException.class);
	}

	@Test
	void uppercaseTagInsertedWithSqlFails() {
		assertThatThrownBy(() -> jdbc.update("insert into tag (name) values ('Hot')"))
				.isInstanceOf(DataAccessException.class)
				.hasMessageContaining("CHECK constraint failed");
	}

	@Test
	void itemTagRowForMissingItemFails() {
		Tag hot = tags.saveAndFlush(new Tag("hot"));

		assertThatThrownBy(() -> jdbc.update("insert into item_tag (item_id, tag_id) values (999, ?)", hot.getId()))
				.isInstanceOf(DataAccessException.class)
				.hasMessageContaining("FOREIGN KEY constraint failed");
	}

	@Test
	void itemTagRowForMissingTagFails() {
		Item fire = items.saveAndFlush(new Item("fire", "🔥", Set.of()));

		assertThatThrownBy(() -> jdbc.update("insert into item_tag (item_id, tag_id) values (?, 999)", fire.getId()))
				.isInstanceOf(DataAccessException.class)
				.hasMessageContaining("FOREIGN KEY constraint failed");
	}

}
