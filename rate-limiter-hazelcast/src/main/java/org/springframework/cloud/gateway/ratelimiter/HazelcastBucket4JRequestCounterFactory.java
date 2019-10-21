package org.springframework.cloud.gateway.ratelimiter;

import java.time.Duration;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.RecoveryStrategy;
import io.github.bucket4j.grid.hazelcast.Hazelcast;
import reactor.core.publisher.Mono;

public class HazelcastBucket4JRequestCounterFactory implements RequestCounterFactory {

	private final HazelcastClusterInitializer hazelcastClusterInitializer;

	public HazelcastBucket4JRequestCounterFactory(HazelcastClusterInitializer hazelcastClusterInitializer) {
		this.hazelcastClusterInitializer = hazelcastClusterInitializer;
	}

	@Override
	public Mono<RequestCounter> create(String routeId, String apiKey, int limit, Duration duration) {
		Bandwidth bandwidth = Bandwidth.simple(limit, duration);
		return Mono.defer(() -> this.createBucket(routeId, apiKey, bandwidth))
		           .map(Bucket4JRequestCounter::new);
	}

	private Mono<Bucket> createBucket(String routeId, String apiKey, Bandwidth bandwidth) {
		return this.hazelcastClusterInitializer
				.getHazelcastCluster()
				.map(hazelcastInstance -> hazelcastInstance.<String, GridBucketState>getMap(routeId))
				.map(map -> Bucket4j.extension(Hazelcast.class)
				                    .builder()
				                    .addLimit(bandwidth)
				                    .build(map, apiKey, RecoveryStrategy.RECONSTRUCT));
	}
}
