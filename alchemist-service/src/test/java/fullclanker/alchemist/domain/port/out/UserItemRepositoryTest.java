package fullclanker.alchemist.domain.port.out;

import fullclanker.alchemist.domain.model.Item;
import fullclanker.alchemist.domain.model.Role;
import fullclanker.alchemist.domain.model.User;
import fullclanker.alchemist.domain.model.UserItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class UserItemRepositoryTest {

	private static final String INSERT = "insert into user_item (user_id, item_id) values (?, ?)";

	@Autowired
	private UserItemRepository userItems;

	@Autowired
	private UserRepository users;

	@Autowired
	private ItemRepository items;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void ownedItemIsFound() {
		User alice = saveUser("alice");
		Item fire = saveItem("fire");
		Item water = saveItem("water");
		userItems.saveAndFlush(new UserItem(alice, fire));

		assertThat(userItems.existsByUserAndItem(alice, fire)).isTrue();
		assertThat(userItems.existsByUserAndItem(alice, water)).isFalse();
	}

	@Test
	void addIfMissingAddsAnItemOnce() {
		User alice = saveUser("alice");
		Item fire = saveItem("fire");

		userItems.addIfMissing(alice, fire);
		userItems.flush();
		userItems.addIfMissing(alice, fire);
		userItems.flush();

		assertThat(rows(alice, fire)).isEqualTo(1);
	}

	@Test
	void duplicatePairInsertedWithSqlFails() {
		User alice = saveUser("alice");
		Item fire = saveItem("fire");
		jdbc.update(INSERT, alice.getId(), fire.getId());

		assertThatThrownBy(() -> jdbc.update(INSERT, alice.getId(), fire.getId()))
				.isInstanceOf(DataAccessException.class)
				.hasMessageContaining("UNIQUE constraint failed");
	}

	@Test
	void missingUserInsertedWithSqlFails() {
		Item fire = saveItem("fire");

		assertThatThrownBy(() -> jdbc.update(INSERT, 999L, fire.getId()))
				.isInstanceOf(DataAccessException.class)
				.hasMessageContaining("FOREIGN KEY constraint failed");
	}

	@Test
	void missingItemInsertedWithSqlFails() {
		User alice = saveUser("alice");

		assertThatThrownBy(() -> jdbc.update(INSERT, alice.getId(), 999L))
				.isInstanceOf(DataAccessException.class)
				.hasMessageContaining("FOREIGN KEY constraint failed");
	}

	private int rows(User user, Item item) {
		return jdbc.queryForObject("select count(*) from user_item where user_id = ? and item_id = ?", Integer.class,
				user.getId(), item.getId());
	}

	private User saveUser(String username) {
		return users.saveAndFlush(new User(username, "h".repeat(60), Role.USER));
	}

	private Item saveItem(String name) {
		return items.saveAndFlush(new Item(name, null, Set.of()));
	}

}
