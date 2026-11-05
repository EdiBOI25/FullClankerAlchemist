package lab.site.backend.adapter.in;

import lab.site.backend.domain.dto.RecipeDto;
import lab.site.backend.domain.port.in.FindRecipes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RecipeController {

	private final FindRecipes findRecipes;

	public RecipeController(FindRecipes findRecipes) {
		this.findRecipes = findRecipes;
	}

	@GetMapping("/recipes")
	public List<RecipeDto> findAll() {
		return findRecipes.findAll();
	}

}
