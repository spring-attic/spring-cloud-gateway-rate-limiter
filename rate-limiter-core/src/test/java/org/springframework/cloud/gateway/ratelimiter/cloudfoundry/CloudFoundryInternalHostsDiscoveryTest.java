package org.springframework.cloud.gateway.ratelimiter.cloudfoundry;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.ratelimiter.cluster.MemberInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CloudFoundryInternalHostsDiscoveryTest {

	@Test
	void shouldThrowAnExceptionWhenNoInternalHostIsProvided() {
		assertThrows(IllegalStateException.class, () -> {
			CloudFoundryInternalHostsDiscovery hostsDiscovery =
					new CloudFoundryInternalHostsDiscovery(Collections.singletonList("app.cf.apps.example.com"), 1, Mono::just);

			hostsDiscovery.discover().block();
		});
	}

	@Test
	void shouldBuildListOfInternalHosts() {
		CloudFoundryInternalHostsDiscovery hostsDiscovery =
				new CloudFoundryInternalHostsDiscovery(Collections.singletonList("app.apps.internal"), 2, Mono::just);

		List<MemberInfo> members = hostsDiscovery.discover().block();

		assertThat(members).extracting("host").contains("0.app.apps.internal");
		assertThat(members).extracting("host").contains("1.app.apps.internal");
	}
}
