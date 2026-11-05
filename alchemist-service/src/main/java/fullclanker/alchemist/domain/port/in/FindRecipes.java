package fullclanker.alchemist.domain.port.in;

import fullclanker.alchemist.domain.dto.RecipeDto;

import java.util.List;

public interface FindRecipes {

	List<RecipeDto> findAll();

}
