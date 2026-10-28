package lab.site.backend.application;

import lab.site.backend.domain.dto.CombineResult;
import lab.site.backend.domain.model.Item;
import lab.site.backend.domain.model.Recipe;
import lab.site.backend.domain.port.in.CombineItems;
import lab.site.backend.domain.port.out.ItemRepository;
import lab.site.backend.domain.port.out.RecipeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CombineServiceTest {

	@Autowired
	private CombineItems combine;

	@Autowired
	private ItemRepository items;

	@Autowired
	private RecipeRepository recipes;

	@Test
	void knownPairReturnsResultWithIsNewFalse() {
		Item fire = items.findByNameIgnoreCase("fire").orElseThrow();
		Item water = items.findByNameIgnoreCase("water").orElseThrow();
		Item steam = items.saveAndFlush(new Item("steam", "🌫️", Set.of()));
		recipes.saveAndFlush(new Recipe(fire, water, steam));

		CombineResult result = combine.combine("fire", "water");

		assertThat(result.item().name()).isEqualTo("steam");
		assertThat(result.isNew()).isFalse();
	}

	@Test
	void reverseOrderReturnsSameKnownResult() {
		Item fire = items.findByNameIgnoreCase("fire").orElseThrow();
		Item water = items.findByNameIgnoreCase("water").orElseThrow();
		Item steam = items.saveAndFlush(new Item("steam", "🌫️", Set.of()));
		recipes.saveAndFlush(new Recipe(fire, water, steam));

		CombineResult result = combine.combine("water", "fire");

		assertThat(result.item().name()).isEqualTo("steam");
		assertThat(result.isNew()).isFalse();
	}

	@Test
	void itemCanCombineWithItself() {
		Item stone = items.saveAndFlush(new Item("stone", "🪨", Set.of()));
		Item same = items.saveAndFlush(new Item("same", "🪩", Set.of()));
		recipes.saveAndFlush(new Recipe(stone, stone, same));

		CombineResult result = combine.combine("stone", "stone");

		assertThat(result.item().name()).isEqualTo("same");
		assertThat(result.isNew()).isFalse();
	}

}
