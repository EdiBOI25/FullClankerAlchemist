package fullclanker.alchemist.application;

import fullclanker.alchemist.domain.dto.RecipeDto;
import fullclanker.alchemist.domain.port.in.FindRecipes;
import fullclanker.alchemist.domain.port.out.RecipeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecipeService implements FindRecipes {

	private final RecipeRepository recipes;

	public RecipeService(RecipeRepository recipes) {
		this.recipes = recipes;
	}

	@Override
	public List<RecipeDto> findAll() {
		return recipes.findAllByOrderByIdAsc().stream()
				.map(RecipeDto::from)
				.toList();
	}

}
