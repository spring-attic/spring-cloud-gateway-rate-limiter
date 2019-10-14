package org.springframework.cloud.gateway.ratelimiter;

import java.util.List;

import reactor.core.publisher.Mono;

public interface ClusterMembersDiscovery {
	Mono<List<MemberInfo>> discover();
	Mono<MemberInfo> thisMember();
}
