package org.springframework.cloud.gateway.ratelimiter;

import reactor.core.publisher.Mono;

public interface RequestCounter {
	Mono<ConsumeResponse> consume(String apiKey);
}
