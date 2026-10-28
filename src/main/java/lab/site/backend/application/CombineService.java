package lab.site.backend.application;

import lab.site.backend.domain.dto.CombineResult;
import lab.site.backend.domain.dto.ItemDto;
import lab.site.backend.domain.model.Item;
import lab.site.backend.domain.model.Recipe;
import lab.site.backend.domain.port.in.CombineItems;
import lab.site.backend.domain.port.out.ItemRepository;
import lab.site.backend.domain.port.out.RecipeRepository;
import org.springframework.stereotype.Service;

@Service
public class CombineService implements CombineItems {

	private final ItemRepository items;
	private final RecipeRepository recipes;

	public CombineService(ItemRepository items, RecipeRepository recipes) {
		this.items = items;
		this.recipes = recipes;
	}

	@Override
	public CombineResult combine(String nameA, String nameB) {
		Item itemA = findItem(nameA);
		Item itemB = findItem(nameB);
		long leftId = Math.min(itemA.getId(), itemB.getId());
		long rightId = Math.max(itemA.getId(), itemB.getId());

		Recipe recipe = recipes.findByItemA_IdAndItemB_Id(leftId, rightId)
				.orElseThrow(() -> new IllegalArgumentException("No known recipe for %s + %s".formatted(nameA, nameB)));

		return new CombineResult(ItemDto.from(recipe.getResult()), false);
	}

	private Item findItem(String name) {
		String normalizedName = name == null ? "" : name.trim();
		if (normalizedName.isBlank()) {
			throw new IllegalArgumentException("Item name cannot be blank");
		}
		return items.findByNameIgnoreCase(normalizedName)
				.orElseThrow(() -> new IllegalArgumentException("Unknown item: %s".formatted(normalizedName)));
	}

}
