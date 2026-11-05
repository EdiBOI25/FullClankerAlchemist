package fullclanker.alchemist.domain.port.out;

import fullclanker.alchemist.domain.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
	List<Recipe> findAllByOrderByIdAsc();

	Optional<Recipe> findByItemA_IdAndItemB_Id(Long itemAId, Long itemBId);
}