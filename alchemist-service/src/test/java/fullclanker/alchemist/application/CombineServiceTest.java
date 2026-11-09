package fullclanker.alchemist.application;

import jakarta.persistence.EntityManager;
import fullclanker.alchemist.domain.dto.CombineResult;
import fullclanker.alchemist.domain.dto.GeneratedItem;
import fullclanker.alchemist.domain.exception.ItemNotFoundException;
import fullclanker.alchemist.domain.model.Item;
import fullclanker.alchemist.domain.model.Recipe;
import fullclanker.alchemist.domain.model.Tag;
import fullclanker.alchemist.domain.port.in.CombineItems;
import fullclanker.alchemist.domain.port.out.ItemGenerator;
import fullclanker.alchemist.domain.port.out.ItemRepository;
import fullclanker.alchemist.domain.port.out.RecipeRepository;
import fullclanker.alchemist.domain.port.out.TagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
	void recipeSavedByAnotherCombineIsReturned() {
		Item water = items.findByNameIgnoreCase("water").orElseThrow();
		Item air = items.findByNameIgnoreCase("air").orElseThrow();
		Item mist = items.saveAndFlush(new Item("Mist", "🌫️", Set.of()));
		when(generator.generate(any(), any())).thenAnswer(call -> {
			recipes.saveAndFlush(new Recipe(water, air, mist));
			return new GeneratedItem("Other", "💨", List.of("gas"));
		});

		CombineResult result = combine.combine("water", "air");

		assertThat(result.item().name()).isEqualTo("Mist");
		assertThat(result.isNew()).isFalse();
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void itemSavedByAnotherCombineIsReused() {
		Item water = items.findByNameIgnoreCase("water").orElseThrow();
		Item air = items.findByNameIgnoreCase("air").orElseThrow();
		when(generator.generate(any(), any())).thenAnswer(call -> {
			items.saveAndFlush(new Item("Mist", "🌫️", Set.of()));
			return new GeneratedItem("Mist", "💨", List.of("gas"));
		});

		CombineResult result = combine.combine("water", "air");

		assertThat(result.item().name()).isEqualTo("Mist");
		assertThat(result.isNew()).isFalse();
	}

	private Set<Tag> savedTags(String itemName) {
		em.flush();
		em.clear();
		return items.findByNameIgnoreCase(itemName).orElseThrow().getTags();
	}

}
