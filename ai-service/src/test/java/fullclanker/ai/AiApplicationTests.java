package fullclanker.ai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.zipkin.autoconfigure.ZipkinProperties;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "ZIPKIN_ENDPOINT=http://zipkin:9411/api/v2/spans")
@AutoConfigureMockMvc
class AiApplicationTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ZipkinProperties zipkin;

	@Test
	void contextLoads() {
	}

	@Test
	void actuatorHealthIsUp() {
		assertThat(mvc.get().uri("/actuator/health"))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.status")
				.isEqualTo("UP");
	}

	@Test
	void zipkinEndpointComesFromTheEnvironment() {
		assertThat(zipkin.getEndpoint()).isEqualTo("http://zipkin:9411/api/v2/spans");
	}

}
