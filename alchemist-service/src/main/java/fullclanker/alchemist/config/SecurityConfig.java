package fullclanker.alchemist.config;

import fullclanker.alchemist.domain.port.out.UserRepository;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/** HTTP Basic on every endpoint except signup and health; other actuator endpoints are admin-only. MVC exception handlers write 401 and 403. */
	@Bean
	@ConditionalOnWebApplication
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.logout(logout -> logout.disable())
				.requestCache(cache -> cache.disable())
				.httpBasic(basic -> basic.authenticationEntryPoint((request, response, exception) ->
						resolver.resolveException(request, response, null, exception)))
				.exceptionHandling(exceptions -> exceptions.accessDeniedHandler((request, response, exception) ->
						resolver.resolveException(request, response, null, exception)))
				.authorizeHttpRequests(requests -> requests
						.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
						.requestMatchers(HttpMethod.POST, "/auth/signup").permitAll()
						.requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
						.requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("ADMIN")
						.anyRequest().authenticated())
				.build();
	}

	/** Loads users by username, ignoring case, with their role. */
	@Bean
	public UserDetailsService userDetailsService(UserRepository users) {
		return username -> users.findByUsernameIgnoreCase(username)
				.map(user -> User.withUsername(user.getUsername())
						.password(user.getPassword())
						.roles(user.getRole().name())
						.build())
				.orElseThrow(() -> new UsernameNotFoundException(username));
	}

}
