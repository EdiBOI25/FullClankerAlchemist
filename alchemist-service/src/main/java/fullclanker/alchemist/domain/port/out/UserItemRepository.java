package fullclanker.alchemist.domain.port.out;

import fullclanker.alchemist.domain.model.Item;
import fullclanker.alchemist.domain.model.User;
import fullclanker.alchemist.domain.model.UserItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserItemRepository extends JpaRepository<UserItem, UserItem.Key> {

	/** Whether the user owns the item; reads only user_item. */
	@Query("select count(ui) > 0 from UserItem ui where ui.user.id = :userId and ui.item.id = :itemId")
	boolean existsByUser_IdAndItem_Id(Long userId, Long itemId);

	/** Adds the item to the user's collection in one statement; only a second add of the same pair is ignored. Call it inside a transaction. */
	@Modifying
	@Query(value = "insert into user_item (user_id, item_id) values (:#{#user.id}, :#{#item.id}) "
			+ "on conflict (user_id, item_id) do nothing", nativeQuery = true)
	void addIfMissing(User user, Item item);

}
