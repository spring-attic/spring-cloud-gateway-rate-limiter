package org.springframework.cloud.gateway.ratelimiter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.ratelimit.AbstractRateLimiter;
import org.springframework.validation.Validator;

public class DefaultRateLimiter extends AbstractRateLimiter<RateLimiterConfig> {

	private static final String CONFIGURATION_PROPERTY_NAME = "rate-limiter";
	private static final RateLimiterConfig DEFAULT_CONFIG = new RateLimiterConfig();
	private static final Response NOT_ALLOWED = new Response(false, Collections.emptyMap());
	private final RequestCounterFactory requestCounterFactory;

	public DefaultRateLimiter(Validator validator, RequestCounterFactory requestCounterFactory) {
		super(RateLimiterConfig.class, CONFIGURATION_PROPERTY_NAME, validator);
		this.requestCounterFactory = requestCounterFactory;
	}

	@Override
	public Mono<Response> isAllowed(String routeId, String id) {
		final RateLimiterConfig config = getConfig().getOrDefault(routeId, DEFAULT_CONFIG);
		return requestCounterFactory.create(routeId, id, config.getLimit(), config.getDuration())
		                            .flatMap(requestCounter -> requestCounter.consume(id))
		                            .map(this::toResponse);
	}

	private Response toResponse(ConsumeResponse consumeResponse) {
		if (consumeResponse.isAllowed()) {
			final Map<String, String> headers = new HashMap<>();
			headers.put("X-Remaining", String.valueOf(consumeResponse.remainingRequests()));
			headers.put("X-Retry-In", String.valueOf(consumeResponse.retryDelayMs()));
			return new Response(true, headers);
		}
		else {
			return NOT_ALLOWED;
		}
	}
}
