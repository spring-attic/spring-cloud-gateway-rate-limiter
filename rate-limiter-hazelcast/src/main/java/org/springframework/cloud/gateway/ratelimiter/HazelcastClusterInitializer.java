package org.springframework.cloud.gateway.ratelimiter;

import java.util.List;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.scheduler.Schedulers;

import org.springframework.cloud.gateway.ratelimiter.cluster.MemberInfo;

public class HazelcastClusterInitializer {

	private final Logger logger = LoggerFactory.getLogger(HazelcastClusterInitializer.class);
	private final RateLimiterConfig defaultConfig = new RateLimiterConfig();
	private final Mono<HazelcastInstance> hazelcastCluster;

	public HazelcastClusterInitializer(String groupName, Mono<List<MemberInfo>> membersSupplier) {
		ReplayProcessor<HazelcastInstance> processor = ReplayProcessor.create();
		membersSupplier.publishOn(Schedulers.elastic())
		               .map(members -> initCluster(groupName, members))
		               .doOnNext(processor::onNext)
		               .subscribe();

		this.hazelcastCluster = processor.next();
	}

	public Mono<HazelcastInstance> getHazelcastCluster() {
		return hazelcastCluster;
	}

	private HazelcastInstance initCluster(String groupName, List<MemberInfo> members) {
		logger.info("Starting new cluster for group {}", groupName);

		Config cfg = new Config();
		cfg.getGroupConfig().setName(groupName);

		JoinConfig joinConfig = cfg.getNetworkConfig().getJoin();
		joinConfig.getMulticastConfig().setEnabled(false);
		TcpIpConfig tcpIpConfig = joinConfig.getTcpIpConfig().setEnabled(true);
		members.forEach(memberInfo -> tcpIpConfig.addMember(memberInfo.getHost()));

		return Hazelcast.newHazelcastInstance(cfg);
	}
}
