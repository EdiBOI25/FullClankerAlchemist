package lab.site.backend.domain.port.in;

import lab.site.backend.domain.dto.RecipeDto;

import java.util.List;

public interface FindRecipes {

	List<RecipeDto> findAll();

}
