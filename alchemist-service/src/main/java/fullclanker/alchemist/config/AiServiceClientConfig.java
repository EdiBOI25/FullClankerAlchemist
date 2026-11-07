package fullclanker.alchemist.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class AiServiceClientConfig {

	/** RestClient for ai-service with its URL and read timeout. */
	@Bean
	public RestClient aiServiceClient(RestClient.Builder builder, ClientHttpRequestFactoryBuilder<?> factoryBuilder,
			HttpClientSettings settings, @Value("${ai-service.url}") String url,
			@Value("${ai-service.read-timeout}") Duration readTimeout) {
		return builder.baseUrl(url)
				.requestFactory(factoryBuilder.build(settings.withReadTimeout(readTimeout)))
				.build();
	}

}
