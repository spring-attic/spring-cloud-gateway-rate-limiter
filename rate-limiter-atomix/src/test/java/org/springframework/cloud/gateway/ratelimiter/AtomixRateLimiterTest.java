package org.springframework.cloud.gateway.ratelimiter;

import java.io.File;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import io.atomix.cluster.Node;
import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.core.Atomix;
import io.atomix.core.map.AtomicCounterMap;
import io.atomix.protocols.raft.partition.RaftPartitionGroup;
import io.atomix.storage.StorageLevel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.ratelimiter.cluster.MemberInfo;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AtomixRateLimiterTest {

	private RateLimiter<RateLimiterConfig> rateLimiter;
	private Atomix atomixNode;
	private String routeId;

	@BeforeAll
	void startTestNode() {
		atomixNode = Atomix.builder()
		                   .withMemberId("test-node")
		                   .withPort(5678)
		                   .withMembershipProvider(BootstrapDiscoveryProvider
				                   .builder()
				                   .withNodes(
						                   Node.builder().withHost("localhost").withPort(5678).build(),
						                   Node.builder().withHost("localhost").withPort(5679).build()
				                   )
				                   .build())
		                   .withManagementGroup(RaftPartitionGroup
				                   .builder("system")
				                   .withNumPartitions(1)
				                   .withMembers("test-node", "localhost")
				                   .withStorageLevel(StorageLevel.MAPPED)
				                   .withDataDirectory(new File(".test-node-system"))
				                   .build())
		                   .withPartitionGroups(RaftPartitionGroup
				                   .builder("raft")
				                   .withNumPartitions(1)
				                   .withMembers("test-node", "localhost")
				                   .withStorageLevel(StorageLevel.MAPPED)
				                   .withDataDirectory(new File(".test-node-raft"))
				                   .build())
		                   .build();
	}

	@AfterAll
	void deleteTestNodeData() {
		FileSystemUtils.deleteRecursively(new File(".data"));
		FileSystemUtils.deleteRecursively(new File(".test-node-system"));
		FileSystemUtils.deleteRecursively(new File(".test-node-raft"));
	}

	@BeforeAll
	void setUpRateLimiterTest() {
		routeId = UUID.randomUUID().toString();

		RateLimiterConfig config = new RateLimiterConfig();
		config.setLimit(1);

		AtomixClusterInitializer atomixClusterInitializer = new AtomixClusterInitializer(Mono.just(new MemberInfo("localhost", 5679)), Mono.just(Collections.singletonList(new MemberInfo("localhost", 5678))));
		AtomixRequestCounterFactory atomixRequestCounterFactory = new AtomixRequestCounterFactory(atomixClusterInitializer);

		rateLimiter = new DefaultRateLimiter(new NoOpValidator(), atomixRequestCounterFactory);
		rateLimiter.getConfig().put(routeId, config);
	}

	@Test
	@DisplayName("should allow request if limit for a key is not reached")
	void shouldAllowRequestBeforeLimit() {
		final String apiKey = UUID.randomUUID().toString();

		RateLimiter.Response block = rateLimiter.isAllowed(routeId, apiKey).block();
		assertThat(block.isAllowed()).isTrue();
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	@DisplayName("should share rate limit with cluster members")
	void shouldConnectToClusterMembers() {
		final String apiKey = UUID.randomUUID().toString();

		atomixNode.start().join();
		AtomicCounterMap<String> rateLimit = atomixNode.getAtomicCounterMap(routeId);
		mockExistingRequests(rateLimit, apiKey);

		RateLimiter.Response response = rateLimiter.isAllowed(routeId, apiKey).block();

		assertThat(response.isAllowed()).isEqualTo(false);
	}

	@Test
	@DisplayName("should reject request if limit for a key is exceeded")
	void shouldRejectRequestAfterLimit() {
		final String apiKey = UUID.randomUUID().toString();
		rateLimiter.isAllowed(routeId, apiKey).block();

		RateLimiter.Response block = rateLimiter.isAllowed(routeId, apiKey).block();
		assertThat(block.isAllowed()).isFalse();
	}

	@Test
	@DisplayName("should not block threads that do not allow blocking")
	void shouldNotBlock() {
		final String apiKey = UUID.randomUUID().toString();

		BlockHound.install();
		StepVerifier.create(rateLimiter.isAllowed(routeId, apiKey))
		            .assertNext(response -> assertThat(response.isAllowed()).isTrue())
		            .verifyComplete();
	}

	/**
	 * Put mock data about existing requests for the given apiKey directly to the map. To avoid flakyness, it targets
	 * not just current second, but -10..+10 seconds in case if test runs longer than a second.
	 */
	private void mockExistingRequests(AtomicCounterMap<String> map, String apiKey) {
		long epochSecond = Instant.now().getEpochSecond();
		for (long i = epochSecond - 10; i < epochSecond + 10; i++) {
			final String mapKey = apiKey + "-" + i;
			map.put(mapKey, 3);
		}
	}
}
