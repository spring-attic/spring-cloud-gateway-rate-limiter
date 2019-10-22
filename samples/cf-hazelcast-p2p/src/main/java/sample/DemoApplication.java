package sample;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.ratelimiter.HazelcastBucket4JRequestCounterFactory;
import org.springframework.cloud.gateway.ratelimiter.HazelcastClusterInitializer;
import org.springframework.cloud.gateway.ratelimiter.HeaderKeyResolver;
import org.springframework.cloud.gateway.ratelimiter.RequestCounterFactory;
import org.springframework.cloud.gateway.ratelimiter.cloudfoundry.CloudFoundryInternalHostsDiscovery;
import org.springframework.cloud.gateway.ratelimiter.cloudfoundry.HostnameResolvableAvailabilityChecker;
import org.springframework.cloud.gateway.ratelimiter.cluster.ClusterMembersDiscovery;
import org.springframework.cloud.gateway.ratelimiter.DefaultRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.Validator;

@SpringBootApplication
public class DemoApplication {

	@Value("${vcap.application.application_id}")
	String appId;

	@Bean
	ClusterMembersDiscovery clusterMembersDiscovery(@Value("${vcap.application.uris}") List<String> uris,
	                                                @Value("${CF_INSTANCE_INDEX:1}") int instanceIndex) {
		return new CloudFoundryInternalHostsDiscovery(uris, instanceIndex, new HostnameResolvableAvailabilityChecker());
	}

	@Bean
	HazelcastClusterInitializer hazelcastClusterInitializer(ClusterMembersDiscovery clusterMembersDiscovery) {
		return new HazelcastClusterInitializer(appId, clusterMembersDiscovery.discover());
	}

	@Bean
	RequestCounterFactory requestCounterFactory(HazelcastClusterInitializer hazelcastClusterInitializer) {
		return new HazelcastBucket4JRequestCounterFactory(hazelcastClusterInitializer);
	}

	@Bean
	public DefaultRateLimiter rateLimiter(Validator defaultValidator, RequestCounterFactory requestCounterFactory) {
		return new DefaultRateLimiter(defaultValidator, requestCounterFactory);
	}

	@Bean
	public HeaderKeyResolver keyResolver() {
		return new HeaderKeyResolver("X-API-Key");
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
}
