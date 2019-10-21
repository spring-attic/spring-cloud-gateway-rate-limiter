package sample;

import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.ratelimiter.AtomixClusterInitializer;
import org.springframework.cloud.gateway.ratelimiter.AtomixRequestCounterFactory;
import org.springframework.cloud.gateway.ratelimiter.DefaultRateLimiter;
import org.springframework.cloud.gateway.ratelimiter.cloudfoundry.CloudFoundryInternalHostsDiscovery;
import org.springframework.cloud.gateway.ratelimiter.cloudfoundry.HostnameResolvableAvailabilityChecker;
import org.springframework.cloud.gateway.ratelimiter.cluster.ClusterMembersDiscovery;
import org.springframework.cloud.gateway.ratelimiter.cluster.MemberInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.Validator;

@SpringBootApplication
public class DemoApplication {

	@Bean
	ClusterMembersDiscovery clusterMembersDiscovery(@Value("${vcap.application.uris}") List<String> uris,
	                                                @Value("${CF_INSTANCE_INDEX:1}") int instanceIndex) {
		return new CloudFoundryInternalHostsDiscovery(uris, instanceIndex, new HostnameResolvableAvailabilityChecker());
	}

	@Bean
	AtomixClusterInitializer atomixClusterInitializer(ClusterMembersDiscovery clusterMembersDiscovery) {
		Mono<List<MemberInfo>> members = clusterMembersDiscovery.discover();
		Mono<MemberInfo> thisMember = clusterMembersDiscovery.thisMember();

		return new AtomixClusterInitializer(thisMember, members);
	}

	@Bean
	public RateLimiter rateLimiter(Validator defaultValidator, AtomixClusterInitializer atomixClusterInitializer) {
		return new DefaultRateLimiter(defaultValidator, new AtomixRequestCounterFactory(atomixClusterInitializer));
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
}
