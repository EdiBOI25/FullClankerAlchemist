package fullclanker.alchemist;

import fullclanker.alchemist.domain.model.Item;
import fullclanker.alchemist.domain.model.Recipe;
import fullclanker.alchemist.domain.port.out.ItemRepository;
import fullclanker.alchemist.domain.port.out.RecipeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Combines through the real HttpItemGenerator while ai-service is down (the test config points it at a closed port). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AiServiceDownTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ItemRepository items;

	@Autowired
	private RecipeRepository recipes;

	@Test
	void knownPairStillCombines() {
		Item fire = items.findByNameIgnoreCase("fire").orElseThrow();
		Item water = items.findByNameIgnoreCase("water").orElseThrow();
		Item steam = items.saveAndFlush(new Item("steam", "🌫️", Set.of()));
		recipes.saveAndFlush(new Recipe(fire, water, steam));

		var response = assertThat(mvc.post().uri("/combine")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"a":"fire","b":"water"}
						"""))
				.hasStatusOk()
				.bodyJson();

		response.extractingPath("$.item.name").isEqualTo("steam");
		response.extractingPath("$.isNew").isEqualTo(false);
	}

	@Test
	void newPairReturnsServiceUnavailableAndSavesNothing() {
		Item fire = items.findByNameIgnoreCase("fire").orElseThrow();
		Item air = items.findByNameIgnoreCase("air").orElseThrow();
		long itemCount = items.count();

		var response = assertThat(mvc.post().uri("/combine")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"a":"fire","b":"air"}
						"""))
				.hasStatus(503)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
				.bodyJson();

		response.extractingPath("$.detail").isEqualTo("AI service is unavailable for fire + air, try again later");
		assertThat(items.count()).isEqualTo(itemCount);
		assertThat(recipes.findByItemA_IdAndItemB_Id(fire.getId(), air.getId())).isEmpty();
	}

}
