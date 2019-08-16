package sample;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.ratelimiter.HazelcastRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.Validator;

@SpringBootApplication
@EnableScheduling
public class DemoApplication {

	@Value("${vcap.application.uris}")
	List<String> uris;

	@Bean
	public HazelcastRateLimiter hazelcastRateLimiter(Validator defaultValidator) {
		int instances = 2;
		String internalHost = uris
				.stream()
				.filter(uri -> uri.endsWith(".apps.internal"))
				.findFirst()
				.orElseThrow(() ->
						new IllegalStateException(String.format("No internal route found in %s, add <app-name>.apps.internal route", String.join(", ", uris))));

		List<String> members = IntStream.range(0, instances)
		                                .mapToObj(id -> id + "." + internalHost)
		                                .collect(Collectors.toList());

		return new HazelcastRateLimiter(defaultValidator, internalHost, members);
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}
