package fullclanker.alchemist.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

@Configuration
public class AiServiceClientConfig {

	/** RestClient for ai-service with its URL and read timeout. */
	@Bean
	public RestClient aiServiceClient(RestClient.Builder builder, ClientHttpRequestFactoryBuilder<?> factoryBuilder,
			HttpClientSettings settings, @Value("${ai-service.url}") String url,
			@Value("${ai-service.read-timeout}") Duration readTimeout) {
		return builder.baseUrl(checkedUrl(url))
				.requestFactory(factoryBuilder.build(settings.withReadTimeout(readTimeout)))
				.build();
	}

	/** Stops startup unless the URL is an absolute http(s) URL with a host. */
	private static String checkedUrl(String url) {
		String hint = "AI_SERVICE_URL (ai-service.url) must look like http://localhost:2501, got '" + url + "'";
		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			throw new IllegalStateException(hint, e);
		}
		if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null) {
			throw new IllegalStateException(hint);
		}
		return url;
	}

}
