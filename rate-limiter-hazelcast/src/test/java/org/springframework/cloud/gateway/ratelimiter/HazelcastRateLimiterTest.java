package org.springframework.cloud.gateway.ratelimiter;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.ratelimiter.cluster.MemberInfo;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HazelcastRateLimiterTest {

	private String routeId;
	private RateLimiter<RateLimiterConfig> rateLimiter;

	@BeforeAll
	void setUpRateLimiterTest() {
		routeId = UUID.randomUUID().toString();

		RateLimiterConfig config = new RateLimiterConfig();
		config.setLimit(1);

		Mono<List<MemberInfo>> members = Mono.just(Collections.singletonList(new MemberInfo("localhost", 5701)));
		HazelcastClusterInitializer hazelcastClusterInitializer = new HazelcastClusterInitializer("test-group", members);
		HazelcastBucket4JRequestCounterFactory requestCounterFactory = new HazelcastBucket4JRequestCounterFactory(hazelcastClusterInitializer);
		rateLimiter = new DefaultRateLimiter(new NoOpValidator(), requestCounterFactory);

		rateLimiter.getConfig().put(routeId, config);
	}

	@Test
	@DisplayName("should allow request if limit for a key is not reached")
	void shouldAllowRequestBeforeLimit() {
		final String apiKey = UUID.randomUUID().toString();

		RateLimiter.Response block = rateLimiter.isAllowed(routeId, apiKey).block();
		assertThat(block.isAllowed()).isTrue();
	}

	@Test
	@DisplayName("should reject request if limit for a key is exceeded")
	void shouldRejectRequestAfterLimit() {
		final String apiKey = UUID.randomUUID().toString();
		rateLimiter.isAllowed(routeId, apiKey).block();

		RateLimiter.Response block = rateLimiter.isAllowed(routeId, apiKey).block();
		assertThat(block.isAllowed()).isFalse();
	}

	@Test
	@DisplayName("should not block threads that do not allow blocking")
	void shouldNotBlock() {
		BlockHoundHelper.install(true);
		final String apiKey = UUID.randomUUID().toString();

		try {
			StepVerifier.create(rateLimiter.isAllowed(routeId, apiKey).publishOn(Schedulers.parallel()))
			            .assertNext(response -> assertThat(response.isAllowed()).isTrue())
			            .verifyComplete();
		}
		finally {
			BlockHoundHelper.disable();
		}
	}
}
