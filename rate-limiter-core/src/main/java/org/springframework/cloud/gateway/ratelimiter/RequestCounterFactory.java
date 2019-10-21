package org.springframework.cloud.gateway.ratelimiter;

import java.time.Duration;

import reactor.core.publisher.Mono;

public interface RequestCounterFactory {
	Mono<RequestCounter> create(String routeId, String apiKey, int limit, Duration duration);
}
