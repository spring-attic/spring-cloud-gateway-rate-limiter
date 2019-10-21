package org.springframework.cloud.gateway.ratelimiter;

import io.github.bucket4j.AsyncBucket;
import io.github.bucket4j.Bucket;
import reactor.core.publisher.Mono;

public class Bucket4JRequestCounter implements RequestCounter {

	private final AsyncBucket bucket;

	Bucket4JRequestCounter(Bucket bucket) {
		this.bucket = bucket.asAsync();
	}

	@Override
	public Mono<ConsumeResponse> consume(String apiKey) {
		return Mono.fromFuture(bucket.tryConsumeAndReturnRemaining(1))
		           .map(consumptionProbe -> new ConsumeResponse(
				           consumptionProbe.isConsumed(),
				           consumptionProbe.getRemainingTokens(),
				           consumptionProbe.getNanosToWaitForRefill() * 1000));
	}
}
