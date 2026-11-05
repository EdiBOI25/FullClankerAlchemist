package lab.site.backend.domain.dto;

import lab.site.backend.domain.model.Recipe;

public record RecipeDto(ItemDto itemA, ItemDto itemB, ItemDto result) {

	public static RecipeDto from(Recipe recipe) {
		return new RecipeDto(
				ItemDto.from(recipe.getItemA()),
				ItemDto.from(recipe.getItemB()),
				ItemDto.from(recipe.getResult()));
	}

}
