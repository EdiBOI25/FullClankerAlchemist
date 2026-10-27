package lab.site.backend.domain.port.out;

import lab.site.backend.domain.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

	Optional<Item> findByNameIgnoreCase(String name);

}
