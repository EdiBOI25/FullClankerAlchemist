package fullclanker.alchemist.adapter.in;

import jakarta.validation.ConstraintViolationException;
import fullclanker.alchemist.domain.exception.AiServiceUnavailableException;
import fullclanker.alchemist.domain.exception.ItemGenerationException;
import fullclanker.alchemist.domain.model.Item;
import fullclanker.alchemist.domain.model.Recipe;
import fullclanker.alchemist.domain.port.out.ItemGenerator;
import fullclanker.alchemist.domain.port.out.ItemRepository;
import fullclanker.alchemist.domain.port.out.RecipeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CombineControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ItemRepository items;

	@Autowired
	private RecipeRepository recipes;

	@MockitoBean
	private ItemGenerator generator;

	@Test
	void knownPairReturnsCombineResult() {
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
		verifyNoInteractions(generator);
	}

	@Test
	void unknownItemReturnsNotFoundProblem() {
		var response = assertThat(mvc.post().uri("/combine")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"a":"unknown","b":"fire"}
						"""))
				.hasStatus(404)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
				.bodyJson();

		response.extractingPath("$.detail").isEqualTo("Unknown item: unknown");
	}

	@Test
	void repeatedAiFailureReturnsBadGatewayProblem() {
		ConstraintViolationException cause = new ConstraintViolationException(Set.of());
		when(generator.generate(any(), any()))
				.thenThrow(new ItemGenerationException("AI returned an invalid item", cause));

		var response = assertThat(mvc.post().uri("/combine")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"a":"fire","b":"water"}
						"""))
				.hasStatus(502)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
				.bodyJson();

		response.extractingPath("$.status").isEqualTo(502);
		verify(generator).generate("fire", "water");
	}

	@Test
	void unavailableAiServiceReturnsServiceUnavailableProblem() {
		when(generator.generate(any(), any())).thenThrow(new AiServiceUnavailableException(
				"AI service is unavailable for fire + water, try again later", new RuntimeException("refused")));

		var response = assertThat(mvc.post().uri("/combine")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"a":"fire","b":"water"}
						"""))
				.hasStatus(503)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
				.bodyJson();

		response.extractingPath("$.detail").isEqualTo("AI service is unavailable for fire + water, try again later");
	}

	@Test
	void missingFieldReturnsBadRequestProblem() {
		assertThat(mvc.post().uri("/combine")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"a":"fire"}
						"""))
				.hasStatus(400)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void blankFieldReturnsBadRequestProblem() {
		assertThat(mvc.post().uri("/combine")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"a":" ","b":"water"}
						"""))
				.hasStatus(400)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void tooLongFieldReturnsBadRequestProblem() {
		String tooLong = "a".repeat(51);

		assertThat(mvc.post().uri("/combine")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"a":"%s","b":"water"}
						""".formatted(tooLong)))
				.hasStatus(400)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
	}

}
