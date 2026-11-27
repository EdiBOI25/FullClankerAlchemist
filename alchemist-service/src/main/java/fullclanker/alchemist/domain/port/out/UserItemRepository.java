package fullclanker.alchemist.domain.port.out;

import fullclanker.alchemist.domain.model.Item;
import fullclanker.alchemist.domain.model.User;
import fullclanker.alchemist.domain.model.UserItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserItemRepository extends JpaRepository<UserItem, UserItem.Key> {

	boolean existsByUserAndItem(User user, Item item);

	/** Adds the item to the user's collection in one statement; the primary key makes a second add a no-op. Call it inside a transaction. */
	@Modifying
	@Query(value = "insert or ignore into user_item (user_id, item_id) values (:#{#user.id}, :#{#item.id})",
			nativeQuery = true)
	void addIfMissing(User user, Item item);

}
