package org.springframework.cloud.gateway.ratelimiter;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRateLimiterTest {

	private RequestCounterFactory mockRequestCounterFactory;
	private DefaultRateLimiter rateLimiter;
	private String routeId;

	@BeforeEach
	void setUp() {
		routeId = UUID.randomUUID().toString();
		mockRequestCounterFactory = (routeId, apiKey, limit, duration) -> Mono.just((RequestCounter) apiKey1 -> {
			if ("allowed-api-key".equals(apiKey1)) {
				return Mono.just(new ConsumeResponse(true, 10, 100));
			}

			return Mono.just(new ConsumeResponse(false, 0, 0));
		});

		rateLimiter = new DefaultRateLimiter(new NoOpValidator(), mockRequestCounterFactory);
	}

	@Test
	void shouldNotAllowRequestIfLimitExceeded() {
		RateLimiter.Response response = rateLimiter.isAllowed(routeId, "not-allowed-api-key").block();
		assertThat(response.isAllowed()).isFalse();
	}

	@Test
	void shouldAllowRequestIfLimitIsNotExceeded() {
		RateLimiter.Response response = rateLimiter.isAllowed(routeId, "allowed-api-key").block();
		assertThat(response.isAllowed()).isTrue();
	}

	@Test
	void shouldPassConsumerResponseDetailsToHeaders() {
		RateLimiter.Response response = rateLimiter.isAllowed(routeId, "allowed-api-key").block();

		assertThat(response.getHeaders()).containsEntry("X-Remaining", "10");
		assertThat(response.getHeaders()).containsEntry("X-Retry-In", "100");
	}
}
