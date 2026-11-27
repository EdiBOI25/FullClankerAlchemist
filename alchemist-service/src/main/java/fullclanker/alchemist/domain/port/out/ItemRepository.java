package fullclanker.alchemist.domain.port.out;

import fullclanker.alchemist.domain.model.Item;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

	Optional<Item> findByNameIgnoreCase(String name);

	@EntityGraph(attributePaths = "tags")
	List<Item> findAllByOrderByIdAsc();

	/** The items in the user's collection, oldest first. */
	@EntityGraph(attributePaths = "tags")
	@Query("select i from Item i where exists "
			+ "(select 1 from UserItem ui where ui.item = i and ui.user.username = :username) order by i.id")
	List<Item> findAllOwnedBy(String username);

}
