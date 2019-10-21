package org.springframework.cloud.gateway.ratelimiter;

import java.time.Instant;

import io.atomix.core.map.AsyncAtomicCounterMap;
import reactor.core.publisher.Mono;

public class AtomixRequestCounter implements RequestCounter {

	private final AsyncAtomicCounterMap<String> requestCount;
	private final int limit;

	AtomixRequestCounter(AsyncAtomicCounterMap<String> requestCount, int limit) {
		this.requestCount = requestCount;
		this.limit = limit;
	}

	@Override
	public Mono<ConsumeResponse> consume(String apiKey) {
		return Mono.fromFuture(requestCount.incrementAndGet(getKey(apiKey)))
		           .map(noRequests -> {
			           if (noRequests > limit) {
				           return new ConsumeResponse(false, 0, 0);
			           }
			           else {
				           return new ConsumeResponse(true, limit - noRequests, 0);
			           }
		           });
	}

	private String getKey(String id) {
		return id + "-" + Instant.now().getEpochSecond();
	}
}
