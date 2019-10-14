package org.springframework.cloud.gateway.ratelimiter;

import java.util.Collections;
import java.util.List;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.TransactionalMap;
import com.hazelcast.transaction.TransactionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.scheduler.Schedulers;

import org.springframework.cloud.gateway.filter.ratelimit.AbstractRateLimiter;
import org.springframework.validation.Validator;

public class HazelcastRateLimiter extends AbstractRateLimiter<RateLimiterConfig> {

	private RateLimiterConfig defaultConfig = new RateLimiterConfig();

	private final Logger logger = LoggerFactory.getLogger(HazelcastRateLimiter.class);
	private final Mono<HazelcastInstance> hazelcastCluster;

	public HazelcastRateLimiter(Validator defaultValidator, String groupName, Mono<List<MemberInfo>> membersSupplier) {
		super(RateLimiterConfig.class, "rate-limiter", defaultValidator);

		ReplayProcessor<HazelcastInstance> processor = ReplayProcessor.create();
		membersSupplier.publishOn(Schedulers.elastic())
		       .map(members -> initCluster(groupName, members))
		       .doOnNext(processor::onNext)
		       .subscribe();

		this.hazelcastCluster = processor.next();
	}

	HazelcastRateLimiter(Validator defaultValidator, String groupName, Mono<List<MemberInfo>> members, RateLimiterConfig config) {
		this(defaultValidator, groupName, members);
		this.defaultConfig = config;
	}

	@Override
	public Mono<Response> isAllowed(String routeId, String id) {
		final Response notAllowed = new Response(false, Collections.emptyMap());
		return this.hazelcastCluster
				.map(instance -> {
					final TransactionContext context = instance.newTransactionContext();
					final RateLimiterConfig config = getConfig().getOrDefault(routeId, defaultConfig);

					context.beginTransaction();
					final TransactionalMap<String, Integer> map = context.getMap("rate-limits");
					final Integer cachedValue = map.getForUpdate(id);
					final Integer noRequests = cachedValue == null ? 1 : cachedValue + 1;

					logger.info("Current limit is {}, total requests to date {}", config.getLimit(), noRequests);

					boolean limitExceeded = noRequests > config.getLimit();
					if (!limitExceeded) {
						map.put(id, noRequests);
					}

					context.commitTransaction();

					if (limitExceeded) {
						logger.info("Exceeded number of allowed requests");
						return notAllowed;
					}

					final int remainingRequests = config.getLimit() - noRequests;

					return new Response(true, Collections.singletonMap("X-Remaining-Limit", String.valueOf(remainingRequests)));
				});
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
