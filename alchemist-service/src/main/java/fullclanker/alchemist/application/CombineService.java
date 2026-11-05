package fullclanker.alchemist.application;

import fullclanker.alchemist.domain.dto.CombineResult;
import fullclanker.alchemist.domain.dto.GeneratedItem;
import fullclanker.alchemist.domain.dto.ItemDto;
import fullclanker.alchemist.domain.exception.ItemNotFoundException;
import fullclanker.alchemist.domain.model.Item;
import fullclanker.alchemist.domain.model.Recipe;
import fullclanker.alchemist.domain.model.Tag;
import fullclanker.alchemist.domain.port.in.CombineItems;
import fullclanker.alchemist.domain.port.out.ItemGenerator;
import fullclanker.alchemist.domain.port.out.ItemRepository;
import fullclanker.alchemist.domain.port.out.RecipeRepository;
import fullclanker.alchemist.domain.port.out.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CombineService implements CombineItems {

	private final ItemRepository items;
	private final TagRepository tags;
	private final RecipeRepository recipes;
	private final ItemGenerator generator;

	public CombineService(ItemRepository items, TagRepository tags, RecipeRepository recipes, ItemGenerator generator) {
		this.items = items;
		this.tags = tags;
		this.recipes = recipes;
		this.generator = generator;
	}

	// Shortcut: the AI call runs inside the DB transaction, so concurrent combines can fail with SQLITE_BUSY; move it out and handle the unique-pair race (ADR-0011) and the item-name race in Phase 4/5
	@Override
	@Transactional
	public CombineResult combine(String nameA, String nameB) {
		Item itemA = findItem(nameA);
		Item itemB = findItem(nameB);
		Item left = itemA.getId() <= itemB.getId() ? itemA : itemB;
		Item right = left == itemA ? itemB : itemA;

		return recipes.findByItemA_IdAndItemB_Id(left.getId(), right.getId())
				.map(recipe -> new CombineResult(ItemDto.from(recipe.getResult()), false))
				.orElseGet(() -> generate(left, right));
	}

	private Item findItem(String name) {
		String normalizedName = name == null ? "" : name.trim();
		if (normalizedName.isBlank()) {
			throw new IllegalArgumentException("Item name cannot be blank");
		}
		return items.findByNameIgnoreCase(normalizedName)
				.orElseThrow(() -> new ItemNotFoundException(normalizedName));
	}

	private CombineResult generate(Item left, Item right) {
		GeneratedItem generated = generator.generate(left.getName(), right.getName());
		String name = generated.name().trim();
		// Shortcut: SQLite folds only ASCII case, so "Éclair" and "éclair" become two items; fix both the lookup and the column if accented names matter
		Optional<Item> existing = items.findByNameIgnoreCase(name);
		Item result = existing.orElseGet(() -> create(name, generated));
		recipes.save(new Recipe(left, right, result));
		return new CombineResult(ItemDto.from(result), existing.isEmpty());
	}

	private Item create(String name, GeneratedItem generated) {
		Set<Tag> itemTags = generated.tags().stream()
				.map(this::findOrCreateTag)
				.collect(Collectors.toSet());
		return items.save(new Item(name, generated.emoji(), itemTags));
	}

	// Shortcut: duplicates BaseItemSeeder's tag find-or-create; extract a shared helper only if a third caller needs it
	private Tag findOrCreateTag(String name) {
		String normalizedName = name.trim().toLowerCase(Locale.ROOT);
		return tags.findByName(normalizedName)
				.orElseGet(() -> tags.save(new Tag(normalizedName)));
	}

}
