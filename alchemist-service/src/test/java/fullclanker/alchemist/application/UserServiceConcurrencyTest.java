package fullclanker.alchemist.application;

import fullclanker.alchemist.domain.dto.Credentials;
import fullclanker.alchemist.domain.exception.UsernameTakenException;
import fullclanker.alchemist.domain.model.Role;
import fullclanker.alchemist.domain.model.User;
import fullclanker.alchemist.domain.port.in.CreateUser;
import fullclanker.alchemist.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserServiceConcurrencyTest {

	@TempDir
	static Path temporaryDirectory;

	@Autowired
	private CreateUser createUser;

	@Autowired
	private UserRepository users;

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url",
				() -> "jdbc:sqlite:" + temporaryDirectory.resolve("users.db") + "?foreign_keys=true");
	}

	@Test
	void concurrentCreatesOfOneNameInDifferentCaseCreateOneUser() throws Exception {
		CyclicBarrier bothStarted = new CyclicBarrier(2);
		List<Throwable> failures = new ArrayList<>();

		try (ExecutorService threads = Executors.newFixedThreadPool(2)) {
			List<Future<?>> creates = List.of("alice", "ALICE").stream()
					.<Future<?>>map(name -> threads.submit(() -> {
						bothStarted.await(5, SECONDS);
						createUser.create(new Credentials(name, "password1"), Role.USER);
						return null;
					}))
					.toList();
			for (Future<?> create : creates) {
				try {
					create.get(10, SECONDS);
				} catch (ExecutionException e) {
					failures.add(e.getCause());
				}
			}
		}

		assertThat(failures).singleElement().isInstanceOf(UsernameTakenException.class);
		assertThat(users.findAll()).singleElement().extracting(User::getUsername).isIn("alice", "ALICE");
	}

}
