package org.springframework.cloud.gateway.ratelimiter;

import java.net.UnknownHostException;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HostnameResolvableAvailabilityCheckerTest {

	private HostnameResolvableAvailabilityChecker checker;

	@BeforeEach
	void setUp() {
		checker = new HostnameResolvableAvailabilityChecker();
	}

	@Test
	void shouldAcceptValidHostname() {
		MemberInfo memberInfo = new MemberInfo("google.com");
		MemberInfo info = checker.check(memberInfo).block();

		assertThat(info).isEqualToComparingFieldByFieldRecursively(memberInfo);
	}

	@Test
	void shouldRejectValidHostname() {
		MemberInfo memberInfo = new MemberInfo("not-an-existing-host-" + UUID.randomUUID().toString() + ".com");
		assertThatThrownBy(() -> checker.check(memberInfo).block()).isNotNull();
	}
}
