package org.springframework.cloud.gateway.ratelimiter;

import java.util.Collections;

import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.ratelimiter.cluster.MemberInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.Validator;

@SpringBootApplication
public class TestApp {

	@Bean
	RateLimiter rateLimiter(Validator defaultValidator) {
		HazelcastClusterInitializer hazelcastClusterInitializer = new HazelcastClusterInitializer("test-group", Mono.just(Collections.singletonList(new MemberInfo("localhost", 5671))));
		HazelcastBucket4JRequestCounterFactory counterFactory = new HazelcastBucket4JRequestCounterFactory(hazelcastClusterInitializer);
		return new DefaultRateLimiter(defaultValidator, counterFactory);
	}

	@Bean
	KeyResolver keyResolver() {
		return exchange -> Mono.just("test-key");
	}

}
