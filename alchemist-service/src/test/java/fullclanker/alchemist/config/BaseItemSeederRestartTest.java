package fullclanker.alchemist.config;

import fullclanker.alchemist.AlchemistApplication;
import fullclanker.alchemist.domain.model.Role;
import fullclanker.alchemist.domain.model.User;
import fullclanker.alchemist.domain.port.out.ItemRepository;
import fullclanker.alchemist.domain.port.out.UserItemRepository;
import fullclanker.alchemist.domain.port.out.UserRepository;
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

	@Autowired
	private UserRepository users;

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", BaseItemSeederRestartTest::databaseUrl);
	}

	@Test
	void restartDoesNotDuplicateBaseItems() {
		assertBaseItems(items);

		try (ConfigurableApplicationContext restarted = SpringApplication.run(
				AlchemistApplication.class,
				"--spring.datasource.url=" + databaseUrl(),
				"--spring.main.web-application-type=none")) {
			assertBaseItems(restarted.getBean(ItemRepository.class));
		}
	}

	@Test
	void restartGivesTheBaseItemsToAUserWithoutThem() {
		users.save(new User("veteran", "h".repeat(60), Role.USER));

		try (ConfigurableApplicationContext restarted = SpringApplication.run(
				AlchemistApplication.class,
				"--spring.datasource.url=" + databaseUrl(),
				"--spring.main.web-application-type=none")) {
			assertThat(restarted.getBean(UserItemRepository.class).findAll())
					.filteredOn(userItem -> userItem.getUser().getUsername().equals("veteran"))
					.extracting(userItem -> userItem.getItem().getName())
					.containsExactlyInAnyOrder("fire", "water", "earth", "air");
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
