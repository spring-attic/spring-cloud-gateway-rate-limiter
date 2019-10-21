package org.springframework.cloud.gateway.ratelimiter.cloudfoundry;

import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.ratelimiter.cluster.MemberInfo;

public interface MemberAvailabilityChecker {
	Mono<MemberInfo> check(MemberInfo memberInfo);
}
