package org.springframework.cloud.gateway.ratelimiter;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HazelcastRateLimiterTest {

	private RateLimiter<RateLimiterConfig> rateLimiter;

	@BeforeAll
	void setUpRateLimiterTest() {
		RateLimiterConfig config = new RateLimiterConfig();
		config.setLimit(1);

		rateLimiter = new HazelcastRateLimiter(new NoOpValidator(), "test-group", Mono.just(Collections.singletonList(new MemberInfo("localhost", 5701))), config);
	}

	@Test
	@DisplayName("should allow request if limit for a key is not reached")
	void shouldAllowRequestBeforeLimit() {
		final String apiKey = UUID.randomUUID().toString();

		RateLimiter.Response block = rateLimiter.isAllowed(UUID.randomUUID().toString(), apiKey).block();
		assertThat(block.isAllowed()).isTrue();
	}

	@Test
	@DisplayName("should reject request if limit for a key is exceeded")
	void shouldRejectRequestAfterLimit() {
		final String apiKey = UUID.randomUUID().toString();
		rateLimiter.isAllowed("foo", apiKey).block();

		RateLimiter.Response block = rateLimiter.isAllowed(UUID.randomUUID().toString(), apiKey).block();
		assertThat(block.isAllowed()).isFalse();
	}

	@Test
	@DisplayName("should not block threads that do not allow blocking")
	void shouldNotBlock() {
		final String apiKey = UUID.randomUUID().toString();

		BlockHound.install();
		StepVerifier.create(rateLimiter.isAllowed(UUID.randomUUID().toString(), apiKey))
		            .assertNext(response -> assertThat(response.isAllowed()).isTrue())
		            .verifyComplete();
	}
}
