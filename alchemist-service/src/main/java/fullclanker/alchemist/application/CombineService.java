package fullclanker.alchemist.application;

import fullclanker.alchemist.domain.dto.CombineResult;
import fullclanker.alchemist.domain.dto.GeneratedItem;
import fullclanker.alchemist.domain.dto.ItemDto;
import fullclanker.alchemist.domain.exception.ItemNotFoundException;
import fullclanker.alchemist.domain.model.Item;
import fullclanker.alchemist.domain.model.Recipe;
import fullclanker.alchemist.domain.model.Tag;
import fullclanker.alchemist.domain.model.User;
import fullclanker.alchemist.domain.port.in.CombineItems;
import fullclanker.alchemist.domain.port.out.ItemGenerator;
import fullclanker.alchemist.domain.port.out.ItemRepository;
import fullclanker.alchemist.domain.port.out.RecipeRepository;
import fullclanker.alchemist.domain.port.out.TagRepository;
import fullclanker.alchemist.domain.port.out.UserItemRepository;
import fullclanker.alchemist.domain.port.out.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CombineService implements CombineItems {

	private final ItemRepository items;
	private final TagRepository tags;
	private final RecipeRepository recipes;
	private final UserRepository users;
	private final UserItemRepository userItems;
	private final ItemGenerator generator;
	private final TransactionTemplate transactions;

	public CombineService(ItemRepository items, TagRepository tags, RecipeRepository recipes, UserRepository users,
			UserItemRepository userItems, ItemGenerator generator, TransactionTemplate transactions) {
		this.items = items;
		this.tags = tags;
		this.recipes = recipes;
		this.users = users;
		this.userItems = userItems;
		this.generator = generator;
		this.transactions = transactions;
	}

	@Override
	public CombineResult combine(String username, String nameA, String nameB) {
		User user = users.findByUsernameIgnoreCase(username).orElseThrow();
		Item itemA = findOwnedItem(user, nameA);
		Item itemB = findOwnedItem(user, nameB);
		Item left = itemA.getId() <= itemB.getId() ? itemA : itemB;
		Item right = left == itemA ? itemB : itemA;

		return recipes.findByItemA_IdAndItemB_Id(left.getId(), right.getId())
				.map(recipe -> transactions.execute(status -> {
					userItems.addIfMissing(user, recipe.getResult());
					return new CombineResult(ItemDto.from(recipe.getResult()), false);
				}))
				.orElseGet(() -> generate(user, left, right));
	}

	/** An item the user doesn't own is reported like an unknown one. */
	private Item findOwnedItem(User user, String name) {
		String normalizedName = name == null ? "" : name.trim();
		if (normalizedName.isBlank()) {
			throw new IllegalArgumentException("Item name cannot be blank");
		}
		return items.findByNameIgnoreCase(normalizedName)
				.filter(item -> userItems.existsByUserAndItem(user, item))
				.orElseThrow(() -> new ItemNotFoundException(normalizedName));
	}

	private CombineResult generate(User user, Item left, Item right) {
		GeneratedItem generated = generator.generate(left.getName(), right.getName());
		return transactions.execute(status -> {
			Optional<Recipe> existingRecipe = recipes.findByItemA_IdAndItemB_Id(left.getId(), right.getId());
			if (existingRecipe.isPresent()) {
				userItems.addIfMissing(user, existingRecipe.get().getResult());
				return new CombineResult(ItemDto.from(existingRecipe.get().getResult()), false);
			}

			String name = generated.name().trim();
			// Shortcut: SQLite folds only ASCII case, so "Éclair" and "éclair" become two items; fix both the lookup and the column if accented names matter
			Optional<Item> existingItem = items.findByNameIgnoreCase(name);
			Item result = existingItem.orElseGet(() -> create(name, generated));
			recipes.save(new Recipe(left, right, result));
			userItems.addIfMissing(user, result);
			return new CombineResult(ItemDto.from(result), existingItem.isEmpty());
		});
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
