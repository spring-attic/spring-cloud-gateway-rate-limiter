package org.springframework.cloud.gateway.ratelimiter;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HazelcastRateLimiterTest {

	private static class NoOpValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return true;
		}

		@Override
		public void validate(Object target, Errors errors) { }


	}

	private HazelcastRateLimiter rateLimiter;

	@BeforeAll
	void setUp() {
		HazelcastRateLimiter.RateLimiterConfig config = new HazelcastRateLimiter.RateLimiterConfig();
		config.setLimit(1);

		rateLimiter = new HazelcastRateLimiter(new NoOpValidator(), "test-group", Collections.singletonList("localhost"), config);
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
}
