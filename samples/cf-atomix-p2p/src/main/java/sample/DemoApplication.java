package sample;

import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.ratelimiter.AtomixRateLimiter;
import org.springframework.cloud.gateway.ratelimiter.CloudFoundryInternalHostsDiscovery;
import org.springframework.cloud.gateway.ratelimiter.ClusterMembersDiscovery;
import org.springframework.cloud.gateway.ratelimiter.HostnameResolvableAvailabilityChecker;
import org.springframework.cloud.gateway.ratelimiter.MemberAvailabilityChecker;
import org.springframework.cloud.gateway.ratelimiter.MemberInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.Validator;

@SpringBootApplication
public class DemoApplication {

	@Bean
	MemberAvailabilityChecker memberAvailabilityChecker() {
		return new HostnameResolvableAvailabilityChecker();
	}

	@Bean
	ClusterMembersDiscovery clusterMembersDiscovery(@Value("${vcap.application.uris}") List<String> uris,
	                                                @Value("${CF_INSTANCE_INDEX:1}") int instanceIndex,
	                                                MemberAvailabilityChecker memberAvailabilityChecker) {
		return new CloudFoundryInternalHostsDiscovery(uris, instanceIndex, memberAvailabilityChecker);
	}

	@Bean
	public AtomixRateLimiter rateLimiter(Validator defaultValidator, ClusterMembersDiscovery clusterMembersDiscovery) {
		Mono<List<MemberInfo>> members = clusterMembersDiscovery.discover();
		Mono<MemberInfo> thisMember = clusterMembersDiscovery.thisMember();
		return new AtomixRateLimiter(defaultValidator, thisMember, members);
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
}
