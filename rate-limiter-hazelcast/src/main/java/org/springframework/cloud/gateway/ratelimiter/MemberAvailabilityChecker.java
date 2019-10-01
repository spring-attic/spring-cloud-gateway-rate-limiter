package org.springframework.cloud.gateway.ratelimiter;

import reactor.core.publisher.Mono;

public interface MemberAvailabilityChecker {
	Mono<MemberInfo> check(MemberInfo memberInfo);
}
