package lab.site.backend.application;

import jakarta.persistence.EntityManager;
import lab.site.backend.domain.dto.CombineResult;
import lab.site.backend.domain.dto.GeneratedItem;
import lab.site.backend.domain.model.Item;
import lab.site.backend.domain.model.Recipe;
import lab.site.backend.domain.model.Tag;
import lab.site.backend.domain.port.in.CombineItems;
import lab.site.backend.domain.port.out.ItemGenerator;
import lab.site.backend.domain.port.out.ItemRepository;
import lab.site.backend.domain.port.out.RecipeRepository;
import lab.site.backend.domain.port.out.TagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class CombineServiceTest {

	@Autowired
	private CombineItems combine;

	@Autowired
	private ItemRepository items;

	@Autowired
	private RecipeRepository recipes;

	@Autowired
	private TagRepository tags;

	@Autowired
	private EntityManager em;

	@MockitoBean
	private ItemGenerator generator;

	@Test
	void knownPairReturnsResultWithIsNewFalse() {
		Item fire = items.findByNameIgnoreCase("fire").orElseThrow();
		Item water = items.findByNameIgnoreCase("water").orElseThrow();
		Item steam = items.saveAndFlush(new Item("steam", "🌫️", Set.of()));
		recipes.saveAndFlush(new Recipe(fire, water, steam));

		CombineResult result = combine.combine("fire", "water");

		assertThat(result.item().name()).isEqualTo("steam");
		assertThat(result.isNew()).isFalse();
		verifyNoInteractions(generator);
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

	@Test
	void unknownPairGeneratesTheItemAndSavesTheRecipe() {
		Item ice = items.saveAndFlush(new Item("ice", "🧊", Set.of()));
		Item lava = items.saveAndFlush(new Item("lava", "🌋", Set.of()));
		when(generator.generate("ice", "lava")).thenReturn(new GeneratedItem(" Obsidian ", "🪨", List.of("volcanic")));

		CombineResult result = combine.combine("lava", "ice");
		em.flush();
		em.clear();

		assertThat(result.isNew()).isTrue();
		assertThat(result.item().name()).isEqualTo("Obsidian");
		assertThat(result.item().tags()).containsExactly("volcanic");
		assertThat(items.findByNameIgnoreCase("obsidian").orElseThrow().getTags())
				.extracting(Tag::getName)
				.containsExactly("volcanic");
		assertThat(recipes.findByItemA_IdAndItemB_Id(ice.getId(), lava.getId()))
				.hasValueSatisfying(recipe -> assertThat(recipe.getResult().getName()).isEqualTo("Obsidian"));
		verify(generator).generate("ice", "lava");
	}

	@Test
	void sameUnknownPairAgainComesFromTheDb() {
		when(generator.generate(any(), any())).thenReturn(new GeneratedItem("Steam", "💨", List.of("gas")));
		combine.combine("fire", "water");

		CombineResult again = combine.combine("water", "fire");

		assertThat(again.isNew()).isFalse();
		assertThat(again.item().name()).isEqualTo("Steam");
		verify(generator, times(1)).generate(any(), any());
	}

	@Test
	void generatedTagsAreTrimmedAndLowercased() {
		when(generator.generate(any(), any())).thenReturn(new GeneratedItem("Magma", "🌋", List.of(" Volcanic ")));

		combine.combine("fire", "earth");

		assertThat(savedTags("magma")).extracting(Tag::getName).containsExactly("volcanic");
	}

	@Test
	void existingTagIsReused() {
		long tagCount = tags.count();
		when(generator.generate(any(), any())).thenReturn(new GeneratedItem("Magma", "🌋", List.of(" Hot ")));

		combine.combine("fire", "earth");

		assertThat(savedTags("magma")).extracting(Tag::getName).containsExactly("hot");
		assertThat(tags.count()).isEqualTo(tagCount);
	}

	@Test
	void duplicateTagIsSavedOnce() {
		long tagCount = tags.count();
		when(generator.generate(any(), any())).thenReturn(new GeneratedItem("Magma", "🌋", List.of("glow", "Glow ")));

		combine.combine("fire", "earth");

		assertThat(savedTags("magma")).extracting(Tag::getName).containsExactly("glow");
		assertThat(tags.count()).isEqualTo(tagCount + 1);
	}

	private Set<Tag> savedTags(String itemName) {
		em.flush();
		em.clear();
		return items.findByNameIgnoreCase(itemName).orElseThrow().getTags();
	}

}
