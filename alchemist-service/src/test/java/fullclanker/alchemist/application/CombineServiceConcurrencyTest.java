package fullclanker.alchemist.application;

import fullclanker.alchemist.domain.dto.CombineResult;
import fullclanker.alchemist.domain.dto.GeneratedItem;
import fullclanker.alchemist.domain.port.in.CombineItems;
import fullclanker.alchemist.domain.port.out.ItemGenerator;
import fullclanker.alchemist.domain.port.out.RecipeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CombineServiceConcurrencyTest {

	@TempDir
	static Path temporaryDirectory;

	@Autowired
	private CombineItems combine;

	@Autowired
	private RecipeRepository recipes;

	@MockitoBean
	private ItemGenerator generator;

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url",
				() -> "jdbc:sqlite:" + temporaryDirectory.resolve("concurrency.db") + "?foreign_keys=true");
	}

	@Test
	void concurrentFirstCombinesOfAPairBothReturnTheSavedItem() throws Exception {
		assertOnlyOneCreates("Mist", () -> combine.combine("water", "air"), () -> combine.combine("water", "air"));
	}

	@Test
	void concurrentFirstCombinesOfTwoPairsWithTheSameResultShareTheItem() throws Exception {
		assertOnlyOneCreates("Cloud", () -> combine.combine("fire", "water"), () -> combine.combine("earth", "air"));

		assertThat(recipes.findAll())
				.filteredOn(recipe -> recipe.getResult().getName().equals("Cloud"))
				.hasSize(2);
	}

	/** Runs both combines at once, after both got the same generated item, and expects one to create it. */
	private void assertOnlyOneCreates(String name, Callable<CombineResult> first, Callable<CombineResult> second)
			throws Exception {
		CyclicBarrier bothGenerated = new CyclicBarrier(2);
		when(generator.generate(any(), any())).thenAnswer(call -> {
			bothGenerated.await(5, SECONDS);
			return new GeneratedItem(name, "☁️", List.of("gas"));
		});

		try (ExecutorService threads = Executors.newFixedThreadPool(2)) {
			Future<CombineResult> firstResult = threads.submit(first);
			Future<CombineResult> secondResult = threads.submit(second);

			assertThat(List.of(firstResult.get(10, SECONDS), secondResult.get(10, SECONDS)))
					.extracting(result -> result.item().name(), CombineResult::isNew)
					.containsExactlyInAnyOrder(tuple(name, true), tuple(name, false));
		}
	}

}
