package lab.site.backend.application;

import lab.site.backend.domain.dto.RecipeDto;
import lab.site.backend.domain.port.in.FindRecipes;
import lab.site.backend.domain.port.out.RecipeRepository;
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
