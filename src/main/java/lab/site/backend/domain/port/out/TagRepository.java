package lab.site.backend.domain.port.out;

import lab.site.backend.domain.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

	Optional<Tag> findByName(String name);

}
