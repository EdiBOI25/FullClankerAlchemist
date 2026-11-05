package lab.site.backend.domain.port.out;

import jakarta.persistence.EntityManager;
import lab.site.backend.domain.model.Item;
import lab.site.backend.domain.model.Recipe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class RecipeRepositoryTest {

	@Autowired
	private ItemRepository items;

	@Autowired
	private RecipeRepository recipes;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void recipeCanBeFoundByIngredientPair() {
		Item fire = saveItem("fire");
		Item water = saveItem("water");
		Item steam = saveItem("steam");
		Recipe recipe = recipes.saveAndFlush(new Recipe(fire, water, steam));
		entityManager.clear();

		assertThat(recipes.findByItemA_IdAndItemB_Id(fire.getId(), water.getId()))
				.get()
				.extracting(Recipe::getId)
				.isEqualTo(recipe.getId());
	}

	@Test
	void reverseIngredientOrderIsStoredInCanonicalOrder() {
		Item first = saveItem("first");
		Item second = saveItem("second");
		Item result = saveItem("result");
		Recipe recipe = new Recipe(second, first, result);

		assertThat(recipe.getItemA().getId()).isEqualTo(first.getId());
		assertThat(recipe.getItemB().getId()).isEqualTo(second.getId());
	}

	@Test
	void duplicateIngredientPairIsRejected() {
		Item first = saveItem("first");
		Item second = saveItem("second");
		Item result = saveItem("result");
		recipes.saveAndFlush(new Recipe(first, second, result));

		assertThatThrownBy(() -> recipes.saveAndFlush(new Recipe(second, first, result)))
				.isInstanceOf(DataAccessException.class)
				.hasMessageContaining("UNIQUE constraint failed: recipe.item_a_id, recipe.item_b_id");
	}

	@Test
	void unorderedIngredientPairInsertedWithSqlFails() {
		Item first = saveItem("first");
		Item second = saveItem("second");
		Item result = saveItem("result");

		assertThatThrownBy(() -> jdbc.update(
				"insert into recipe (item_a_id, item_b_id, result_id) values (?, ?, ?)",
				second.getId(), first.getId(), result.getId()))
				.isInstanceOf(DataAccessException.class)
				.hasMessageContaining("CHECK constraint failed");
	}

	@Test
	void recipeForMissingItemInsertedWithSqlFails() {
		Item first = saveItem("first");
		Item result = saveItem("result");

		assertThatThrownBy(() -> jdbc.update(
				"insert into recipe (item_a_id, item_b_id, result_id) values (?, ?, ?)",
				first.getId(), 999L, result.getId()))
				.isInstanceOf(DataAccessException.class)
				.hasMessageContaining("FOREIGN KEY constraint failed");
	}

	@Test
	void itemCanBeCombinedWithItself() {
		Item item = saveItem("item");
		Recipe recipe = recipes.saveAndFlush(new Recipe(item, item, item));

		assertThat(recipe.getItemA().getId()).isEqualTo(item.getId());
		assertThat(recipe.getItemB().getId()).isEqualTo(item.getId());
		assertThat(recipes.findByItemA_IdAndItemB_Id(item.getId(), item.getId())).isPresent();
	}

	private Item saveItem(String name) {
		return items.saveAndFlush(new Item(name, null, Set.of()));
	}

}