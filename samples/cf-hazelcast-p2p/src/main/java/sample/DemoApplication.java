package sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.ratelimiter.HazelcastRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.Validator;

@SpringBootApplication
@EnableScheduling
public class DemoApplication {

	@Bean
	public HazelcastRateLimiter hazelcastRateLimiter(Validator defaultValidator) {
		return new HazelcastRateLimiter(defaultValidator, "peer.apps.internal", 2);
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}
