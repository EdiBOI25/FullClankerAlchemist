package fullclanker.alchemist.domain.port.out;

import fullclanker.alchemist.domain.model.Item;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

	Optional<Item> findByNameIgnoreCase(String name);

	@EntityGraph(attributePaths = "tags")
	List<Item> findAllByOrderByIdAsc();

}
