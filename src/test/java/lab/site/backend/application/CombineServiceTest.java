package lab.site.backend.application;

import jakarta.persistence.EntityManager;
import lab.site.backend.domain.dto.CombineResult;
import lab.site.backend.domain.dto.GeneratedItem;
import lab.site.backend.domain.exception.ItemNotFoundException;
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
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
	void unknownFirstItemIsRejected() {
		assertThatThrownBy(() -> combine.combine("unknown", "fire"))
				.isInstanceOf(ItemNotFoundException.class)
				.hasMessage("Unknown item: unknown");
		verifyNoInteractions(generator);
	}

	@Test
	void unknownSecondItemIsRejected() {
		assertThatThrownBy(() -> combine.combine("fire", "unknown"))
				.isInstanceOf(ItemNotFoundException.class)
				.hasMessage("Unknown item: unknown");
		verifyNoInteractions(generator);
	}

	@Test
	void knownItemNamesIgnoreCase() {
		Item fire = items.findByNameIgnoreCase("fire").orElseThrow();
		Item water = items.findByNameIgnoreCase("water").orElseThrow();
		Item steam = items.saveAndFlush(new Item("steam", "🌫️", Set.of()));
		recipes.saveAndFlush(new Recipe(fire, water, steam));

		CombineResult result = combine.combine("FiRe", "WATER");

		assertThat(result.item().name()).isEqualTo("steam");
		assertThat(result.isNew()).isFalse();
		verifyNoInteractions(generator);
	}

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
		em.flush();
		em.clear();

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

	@Test
	void existingNameFromTheAiReusesThatItem() {
		Item steam = items.saveAndFlush(new Item("steam", "🌫️", Set.of()));
		Item ice = items.saveAndFlush(new Item("ice", "🧊", Set.of()));
		Item lava = items.saveAndFlush(new Item("lava", "🌋", Set.of()));
		long itemCount = items.count();
		when(generator.generate(any(), any())).thenReturn(new GeneratedItem(" STEAM ", "💨", List.of("vapor")));

		CombineResult result = combine.combine("ice", "lava");
		em.flush();
		em.clear();

		assertThat(result.isNew()).isFalse();
		assertThat(result.item().id()).isEqualTo(steam.getId());
		assertThat(items.count()).isEqualTo(itemCount);
		assertThat(recipes.findByItemA_IdAndItemB_Id(ice.getId(), lava.getId()))
				.hasValueSatisfying(recipe -> assertThat(recipe.getResult().getId()).isEqualTo(steam.getId()));
	}

	@Test
	void reusedItemKeepsItsEmojiAndTags() {
		items.saveAndFlush(new Item("steam", "🌫️", Set.of(tags.findByName("gas").orElseThrow())));
		long tagCount = tags.count();
		when(generator.generate(any(), any())).thenReturn(new GeneratedItem("Steam", "💨", List.of("vapor")));

		CombineResult result = combine.combine("fire", "water");

		assertThat(result.item().emoji()).isEqualTo("🌫️");
		assertThat(result.item().tags()).containsExactly("gas");
		assertThat(savedTags("steam")).extracting(Tag::getName).containsExactly("gas");
		assertThat(items.findByNameIgnoreCase("steam").orElseThrow().getEmoji()).isEqualTo("🌫️");
		assertThat(tags.count()).isEqualTo(tagCount);
	}

	@Test
	void inputNameFromTheAiReusesThatInput() {
		Item fire = items.findByNameIgnoreCase("fire").orElseThrow();
		Item air = items.findByNameIgnoreCase("air").orElseThrow();
		long itemCount = items.count();
		when(generator.generate(any(), any())).thenReturn(new GeneratedItem("Fire", "🔥", List.of("hot")));

		CombineResult result = combine.combine("fire", "air");

		assertThat(result.isNew()).isFalse();
		assertThat(result.item().id()).isEqualTo(fire.getId());
		assertThat(items.count()).isEqualTo(itemCount);
		assertThat(recipes.findByItemA_IdAndItemB_Id(Math.min(fire.getId(), air.getId()), Math.max(fire.getId(), air.getId())))
				.hasValueSatisfying(recipe -> assertThat(recipe.getResult().getId()).isEqualTo(fire.getId()));
	}

	@Test
	void generatorFailureSavesNothing() {
		Item fire = items.findByNameIgnoreCase("fire").orElseThrow();
		Item air = items.findByNameIgnoreCase("air").orElseThrow();
		long itemCount = items.count();
		RuntimeException failure = new RuntimeException("AI down");
		when(generator.generate(any(), any())).thenThrow(failure);

		assertThatThrownBy(() -> combine.combine("fire", "air")).isSameAs(failure);
		assertThat(items.count()).isEqualTo(itemCount);
		assertThat(recipes.findByItemA_IdAndItemB_Id(fire.getId(), air.getId())).isEmpty();
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void failedRecipeSaveAlsoRollsBackTheItem() {
		Item water = items.findByNameIgnoreCase("water").orElseThrow();
		Item air = items.findByNameIgnoreCase("air").orElseThrow();
		when(generator.generate(any(), any())).thenAnswer(call -> {
			recipes.save(new Recipe(water, air, water));
			return new GeneratedItem("Mist", "🌫️", List.of("gas"));
		});

		// Shortcut: SQLite's unique violation surfaces as JpaSystemException; map error code 19 in SqliteDialect when the race is handled (Phase 4/5)
		assertThatThrownBy(() -> combine.combine("water", "air"))
				.isInstanceOf(DataAccessException.class)
				.hasMessageContaining("UNIQUE constraint failed");
		assertThat(items.findByNameIgnoreCase("mist")).isEmpty();
	}

	private Set<Tag> savedTags(String itemName) {
		em.flush();
		em.clear();
		return items.findByNameIgnoreCase(itemName).orElseThrow().getTags();
	}

}
