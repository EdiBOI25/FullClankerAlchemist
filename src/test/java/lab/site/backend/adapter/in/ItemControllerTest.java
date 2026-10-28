package lab.site.backend.adapter.in;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class ItemControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void getItemsReturnsBaseItemsInDiscoveryOrder() {
		var json = assertThat(mvc.get().uri("/items")).hasStatusOk().bodyJson();

		json.extractingPath("$[0]").asMap().containsOnlyKeys("id", "name", "emoji", "iconUrl", "tags");
		json.extractingPath("$[*].name").asArray().containsExactly("fire", "water", "earth", "air");
		json.extractingPath("$[*].emoji").asArray().containsExactly("🔥", "💧", "🌍", "💨");
		json.extractingPath("$[*].tags").asArray()
				.containsExactly(List.of("hot"), List.of("fluid"), List.of("solid"), List.of("gas"));
	}

}
