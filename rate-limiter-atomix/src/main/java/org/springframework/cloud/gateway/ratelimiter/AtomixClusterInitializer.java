package org.springframework.cloud.gateway.ratelimiter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.atomix.cluster.Node;
import io.atomix.cluster.NodeId;
import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.core.Atomix;
import io.atomix.protocols.raft.partition.RaftPartitionGroup;
import io.atomix.storage.StorageLevel;
import io.atomix.utils.net.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.scheduler.Schedulers;

import org.springframework.cloud.gateway.ratelimiter.cluster.MemberInfo;

public class AtomixClusterInitializer {

	private final static int ATOMIX_PORT = 5679;
	private final Logger logger = LoggerFactory.getLogger(AtomixClusterInitializer.class);

	private RateLimiterConfig defaultConfig = new RateLimiterConfig();
	private Mono<Atomix> atomix;

	public AtomixClusterInitializer(Mono<MemberInfo> currentNode, Mono<List<MemberInfo>> membersSupplier) {
		ReplayProcessor<Atomix> processor = ReplayProcessor.create();
		atomix = processor.next();

		membersSupplier.map(this::membersToNodes)
		               .zipWith(currentNode)
		               .map(nodesAndCurrent -> {
			               final List<Node> nodes = nodesAndCurrent.getT1();
			               final MemberInfo current = nodesAndCurrent.getT2();

			               logger.info("Using nodes {}, this node is {}", nodes, currentNode);

			               return buildCluster(nodes, current);
		               })
		               .doOnNext(atomix -> atomix.start().join())
		               .subscribeOn(Schedulers.elastic())
		               .doOnNext(processor::onNext)
		               .subscribe();
	}

	public Mono<Atomix> getAtomixCluster() {
		return atomix;
	}

	private Atomix buildCluster(List<Node> nodes, MemberInfo current) {
		final Set<String> allMembers = nodes.stream()
		                                    .map(Node::id)
		                                    .map(NodeId::toString)
		                                    .collect(Collectors.toSet());

		return Atomix.builder()
		             .withMemberId(current.getHost())
		             .withAddress(new Address(current.getHost(), ATOMIX_PORT))
		             .withMembershipProvider(BootstrapDiscoveryProvider
				             .builder()
				             .withNodes(nodes)
				             .build())
		             .withManagementGroup(RaftPartitionGroup
				             .builder("system")
				             .withNumPartitions(1)
				             .withMembers(allMembers)
				             .withStorageLevel(StorageLevel.MEMORY)
				             .build())
		             .withPartitionGroups(RaftPartitionGroup
				             .builder("raft")
				             .withNumPartitions(1)
				             .withMembers(allMembers)
				             .withStorageLevel(StorageLevel.MEMORY)
				             .build())
		             .build();
	}

	private List<Node> membersToNodes(List<MemberInfo> members) {
		return members.stream()
		              .map(memberInfo -> Node.builder()
		                                     .withId(memberInfo.getHost())
		                                     .withHost(memberInfo.getHost())
		                                     .withPort(ATOMIX_PORT)
		                                     .build())
		              .collect(Collectors.toList());
	}
}
