package org.springframework.cloud.gateway.ratelimiter;

import java.time.Duration;
import java.time.Instant;

import io.atomix.core.Atomix;
import reactor.core.publisher.Mono;

public class AtomixRequestCounterFactory implements RequestCounterFactory {

	private final Mono<Atomix> atomixCluster;

	public AtomixRequestCounterFactory(AtomixClusterInitializer atomixClusterInitializer) {
		atomixCluster = atomixClusterInitializer.getAtomixCluster();
	}

	@Override
	public Mono<RequestCounter> create(String routeId, String apiKey, int limit, Duration duration) {
		return atomixCluster
				.map(atomix -> atomix.<String>getAtomicCounterMap(routeId).async())
				.map(map -> new AtomixRequestCounter(map, limit));
	}
}
