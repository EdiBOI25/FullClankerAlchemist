package lab.site.backend.adapter.in;

import lab.site.backend.domain.model.Item;
import lab.site.backend.domain.model.Tag;
import lab.site.backend.domain.port.out.ItemRepository;
import lab.site.backend.domain.port.out.TagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ItemControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ItemRepository items;

	@Autowired
	private TagRepository tags;

	@Test
	void getItemsReturnsBaseItemsInDiscoveryOrder() {
		var json = assertThat(mvc.get().uri("/items")).hasStatusOk().bodyJson();

		json.extractingPath("$[0]").asMap().containsOnlyKeys("id", "name", "emoji", "iconUrl", "tags");
		json.extractingPath("$[*].name").asArray().containsExactly("fire", "water", "earth", "air");
		json.extractingPath("$[*].emoji").asArray().containsExactly("🔥", "💧", "🌍", "💨");
		json.extractingPath("$[*].tags").asArray()
				.containsExactly(List.of("hot"), List.of("fluid"), List.of("solid"), List.of("gas"));
	}

	@Test
	void filterByOneTagIgnoresCase() {
		assertThat(mvc.get().uri("/items").param("tag", "HOT"))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$[*].name").asArray().containsExactly("fire");
	}

	@Test
	void filterByTwoTagsReturnsOnlyItemsWithBoth() {
		Tag hot = tags.findByName("hot").orElseThrow();
		Tag solid = tags.findByName("solid").orElseThrow();
		items.save(new Item("lava", "🌋", Set.of(hot, solid)));

		assertThat(mvc.get().uri("/items").param("tag", "hot", "solid"))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$[*].name").asArray().containsExactly("lava");
	}

	@Test
	void filterByUnknownTagReturnsEmptyList() {
		assertThat(mvc.get().uri("/items").param("tag", "unknown"))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$").asArray().isEmpty();
	}

}
