package lab.site.backend.config;

import lab.site.backend.Application;
import lab.site.backend.domain.port.out.ItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BaseItemSeederRestartTest {

	@TempDir
	static Path temporaryDirectory;

	@Autowired
	private ItemRepository items;

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", BaseItemSeederRestartTest::databaseUrl);
	}

	@Test
	void restartDoesNotDuplicateBaseItems() {
		assertBaseItems(items);

		try (ConfigurableApplicationContext restarted = SpringApplication.run(
				Application.class,
				"--spring.datasource.url=" + databaseUrl(),
				"--spring.main.web-application-type=none")) {
			assertBaseItems(restarted.getBean(ItemRepository.class));
		}
	}

	private static void assertBaseItems(ItemRepository items) {
		assertThat(items.findAll())
				.hasSize(4)
				.extracting(item -> item.getName())
				.containsExactlyInAnyOrder("fire", "water", "earth", "air");
	}

	private static String databaseUrl() {
		return "jdbc:sqlite:" + temporaryDirectory.resolve("seeder.db") + "?foreign_keys=true";
	}

}
