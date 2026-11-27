package fullclanker.alchemist.application;

import fullclanker.alchemist.TestPlayer;
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
import fullclanker.alchemist.domain.port.out.UserItemRepository;
import fullclanker.alchemist.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
@Import(TestPlayer.class)
@Transactional
class CombineServiceTest {

	private static final String PLAYER = TestPlayer.USERNAME;
	private static final String OTHER = TestPlayer.OTHER_USERNAME;

	@Autowired
	private CombineItems combine;

	@Autowired
	private ItemRepository items;

	@Autowired
	private RecipeRepository recipes;

	@Autowired
	private TagRepository tags;

	@Autowired
	private UserRepository users;

	@Autowired
	private UserItemRepository userItems;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private EntityManager em;

	@MockitoBean
	private ItemGenerator generator;

	@Test
	void unknownFirstItemIsRejected() {
		assertThatThrownBy(() -> combine.combine(PLAYER, "unknown", "fire"))
				.isInstanceOf(ItemNotFoundException.class)
				.hasMessage("Unknown item: unknown");
		verifyNoInteractions(generator);
	}

	@Test
	void unknownSecondItemIsRejected() {
		assertThatThrownBy(() -> combine.combine(PLAYER, "fire", "unknown"))
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

		CombineResult result = combine.combine(PLAYER, "FiRe", "WATER");

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

		CombineResult result = combine.combine(PLAYER, "fire", "water");

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

		CombineResult result = combine.combine(PLAYER, "water", "fire");

		assertThat(result.item().name()).isEqualTo("steam");
		assertThat(result.isNew()).isFalse();
	}

	@Test
	void itemCanCombineWithItself() {
		Item stone = saveOwnedItem("stone", "🪨");
		Item same = items.saveAndFlush(new Item("same", "🪩", Set.of()));
		recipes.saveAndFlush(new Recipe(stone, stone, same));

		CombineResult result = combine.combine(PLAYER, "stone", "stone");

		assertThat(result.item().name()).isEqualTo("same");
		assertThat(result.isNew()).isFalse();
	}

	@Test
	void unknownPairGeneratesTheItemAndSavesTheRecipe() {
		Item ice = saveOwnedItem("ice", "🧊");
		Item lava = saveOwnedItem("lava", "🌋");
		when(generator.generate("ice", "lava")).thenReturn(new GeneratedItem(" Obsidian ", "🪨", List.of("volcanic")));

		CombineResult result = combine.combine(PLAYER, "lava", "ice");
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
		combine.combine(PLAYER, "fire", "water");
		em.flush();
		em.clear();

		CombineResult again = combine.combine(PLAYER, "water", "fire");

		assertThat(again.isNew()).isFalse();
		assertThat(again.item().name()).isEqualTo("Steam");
		verify(generator, times(1)).generate(any(), any());
	}

	@Test
	void generatedTagsAreTrimmedAndLowercased() {
		when(generator.generate(any(), any())).thenReturn(new GeneratedItem("Magma", "🌋", List.of(" Volcanic ")));

		combine.combine(PLAYER, "fire", "earth");

		assertThat(savedTags("magma")).extracting(Tag::getName).containsExactly("volcanic");
	}

	@Test
	void existingTagIsReused() {
		long tagCount = tags.count();
		when(generator.generate(any(), any())).thenReturn(new GeneratedItem("Magma", "🌋", List.of(" Hot ")));

		combine.combine(PLAYER, "fire", "earth");

		assertThat(savedTags("magma")).extracting(Tag::getName).containsExactly("hot");
		assertThat(tags.count()).isEqualTo(tagCount);
	}

	@Test
	void duplicateTagIsSavedOnce() {
		long tagCount = tags.count();
		when(generator.generate(any(), any())).thenReturn(new GeneratedItem("Magma", "🌋", List.of("glow", "Glow ")));

		combine.combine(PLAYER, "fire", "earth");

		assertThat(savedTags("magma")).extracting(Tag::getName).containsExactly("glow");
		assertThat(tags.count()).isEqualTo(tagCount + 1);
	}

	@Test
	void existingNameFromTheAiReusesThatItem() {
		Item steam = items.saveAndFlush(new Item("steam", "🌫️", Set.of()));
		Item ice = saveOwnedItem("ice", "🧊");
		Item lava = saveOwnedItem("lava", "🌋");
		long itemCount = items.count();
		when(generator.generate(any(), any())).thenReturn(new GeneratedItem(" STEAM ", "💨", List.of("vapor")));

		CombineResult result = combine.combine(PLAYER, "ice", "lava");
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

		CombineResult result = combine.combine(PLAYER, "fire", "water");

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

		CombineResult result = combine.combine(PLAYER, "fire", "air");

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

		assertThatThrownBy(() -> combine.combine(PLAYER, "fire", "air")).isSameAs(failure);
		assertThat(items.count()).isEqualTo(itemCount);
		assertThat(recipes.findByItemA_IdAndItemB_Id(fire.getId(), air.getId())).isEmpty();
	}

	@Test
	void itemOutsideTheCollectionIsRejectedLikeAnUnknownOne() {
		items.saveAndFlush(new Item("lava", "🌋", Set.of()));

		assertThatThrownBy(() -> combine.combine(PLAYER, "lava", "fire"))
				.isInstanceOf(ItemNotFoundException.class)
				.hasMessage("Unknown item: lava");
		assertThatThrownBy(() -> combine.combine(PLAYER, "fire", "lava"))
				.isInstanceOf(ItemNotFoundException.class)
				.hasMessage("Unknown item: lava");
		verifyNoInteractions(generator);
	}

	@Test
	void newItemJoinsTheCollection() {
		when(generator.generate(any(), any())).thenReturn(new GeneratedItem("Steam", "💨", List.of("gas")));

		combine.combine(PLAYER, "fire", "water");

		assertThat(collectionRows(PLAYER, "steam")).isEqualTo(1);
	}

	@Test
	void pairDiscoveredByAnotherUserGivesTheItemWithoutCallingTheAi() {
		when(generator.generate(any(), any())).thenReturn(new GeneratedItem("Steam", "💨", List.of("gas")));
		combine.combine(PLAYER, "fire", "water");
		em.flush();
		em.clear();

		CombineResult result = combine.combine(OTHER, "water", "fire");

		assertThat(result.item().name()).isEqualTo("Steam");
		assertThat(result.isNew()).isFalse();
		assertThat(collectionRows(OTHER, "steam")).isEqualTo(1);
		verify(generator, times(1)).generate(any(), any());
	}

	@Test
	void sameCombineTwiceAddsTheItemOnce() {
		when(generator.generate(any(), any())).thenReturn(new GeneratedItem("Steam", "💨", List.of("gas")));

		combine.combine(PLAYER, "fire", "water");
		combine.combine(PLAYER, "fire", "water");

		assertThat(collectionRows(PLAYER, "steam")).isEqualTo(1);
	}

	@Test
	void resultTheUserAlreadyOwnsAddsNothing() {
		Item fire = items.findByNameIgnoreCase("fire").orElseThrow();
		Item air = items.findByNameIgnoreCase("air").orElseThrow();
		recipes.saveAndFlush(new Recipe(fire, air, fire));

		combine.combine(PLAYER, "fire", "air");

		assertThat(collectionRows(PLAYER, "fire")).isEqualTo(1);
		assertThat(collectionSize(PLAYER)).isEqualTo(4);
	}

	private Item saveOwnedItem(String name, String emoji) {
		Item item = items.saveAndFlush(new Item(name, emoji, Set.of()));
		userItems.addIfMissing(users.findByUsernameIgnoreCase(PLAYER).orElseThrow(), item);
		return item;
	}

	private int collectionRows(String username, String itemName) {
		em.flush();
		return jdbc.queryForObject("select count(*) from user_item ui join user u on u.id = ui.user_id "
				+ "join item i on i.id = ui.item_id where u.username = ? and i.name = ? collate nocase",
				Integer.class, username, itemName);
	}

	private int collectionSize(String username) {
		em.flush();
		return jdbc.queryForObject("select count(*) from user_item ui join user u on u.id = ui.user_id "
				+ "where u.username = ?", Integer.class, username);
	}

	private Set<Tag> savedTags(String itemName) {
		em.flush();
		em.clear();
		return items.findByNameIgnoreCase(itemName).orElseThrow().getTags();
	}

}
