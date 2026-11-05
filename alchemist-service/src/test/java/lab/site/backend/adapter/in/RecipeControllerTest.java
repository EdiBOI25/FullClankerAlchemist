package lab.site.backend.adapter.in;

import lab.site.backend.domain.model.Item;
import lab.site.backend.domain.model.Recipe;
import lab.site.backend.domain.port.out.ItemRepository;
import lab.site.backend.domain.port.out.RecipeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecipeControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ItemRepository items;

	@Autowired
	private RecipeRepository recipes;

	@Test
	void getRecipesReturnsEmptyListWhenNoRecipesExist() {
		assertThat(mvc.get().uri("/recipes"))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$").asArray().isEmpty();
	}

	@Test
	void getRecipesReturnsSavedRecipeWithItsItems() {
		Item fire = items.findByNameIgnoreCase("fire").orElseThrow();
		Item water = items.findByNameIgnoreCase("water").orElseThrow();
		Item steam = items.saveAndFlush(new Item("steam", "🌫️", Set.of()));
		recipes.saveAndFlush(new Recipe(fire, water, steam));

		var json = assertThat(mvc.get().uri("/recipes")).hasStatusOk().bodyJson();

		json.extractingPath("$[0].itemA").asMap().containsOnlyKeys("id", "name", "emoji", "iconUrl", "tags");
		json.extractingPath("$[0].itemB").asMap().containsOnlyKeys("id", "name", "emoji", "iconUrl", "tags");
		json.extractingPath("$[0].result").asMap().containsOnlyKeys("id", "name", "emoji", "iconUrl", "tags");
		json.extractingPath("$[0].itemA.name").isEqualTo("fire");
		json.extractingPath("$[0].itemB.name").isEqualTo("water");
		json.extractingPath("$[0].result.name").isEqualTo("steam");
	}

}
