package org.springframework.cloud.gateway.ratelimiter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.TransactionalMap;
import com.hazelcast.transaction.TransactionContext;
import com.hazelcast.transaction.TransactionOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.ratelimit.AbstractRateLimiter;
import org.springframework.validation.Validator;

public class HazelcastRateLimiter extends AbstractRateLimiter<HazelcastRateLimiter.RateLimiterConfig> {

	static class RateLimiterConfig {

		private int limit;

		public int getLimit() {
			return limit;
		}

		public void setLimit(int limit) {
			this.limit = limit;
		}

	}

	private final Logger logger = LoggerFactory.getLogger(HazelcastRateLimiter.class);
	private final Mono<HazelcastInstance> hazelcastCluster;

	public HazelcastRateLimiter(Validator defaultValidator, String internalHost, int instances) {
		super(HazelcastRateLimiter.RateLimiterConfig.class, "hazelcast-rate-limiter", defaultValidator);
		List<String> members = IntStream.range(0, instances)
		                                .mapToObj(id -> id + "." + internalHost)
		                                .collect(Collectors.toList());

		this.hazelcastCluster = Mono.defer(() -> {
            HazelcastInstance hazelcastInstance = initCluster(internalHost, members);
            return Mono.just(hazelcastInstance);
		}).cache();
	}

    @Override
	public Mono<Response> isAllowed(String routeId, String id) {
		final Response notAllowed = new Response(false, Collections.emptyMap());
		return this.hazelcastCluster
				.map(instance -> {
					final TransactionContext context = instance.newTransactionContext();
					final HazelcastRateLimiter.RateLimiterConfig config = getConfig().getOrDefault(routeId, new HazelcastRateLimiter.RateLimiterConfig());

					context.beginTransaction();
					final TransactionalMap<String, Integer> map = context.getMap("rates-limits");
					final Integer cachedValue = map.getForUpdate(id);
					final Integer noRequests = cachedValue == null ? 1 : cachedValue + 1;

					logger.info("Current limit is {}, total requests to date {}", config.getLimit(), noRequests);

					boolean limitExceeded = noRequests >= config.getLimit();
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

    private HazelcastInstance initCluster(String groupName, List<String> members) {
	    String instanceIndex = System.getenv("CF_INSTANCE_INDEX");
	    logger.info("Starting new cluster for group {} on instance {}", groupName, instanceIndex);

        Config cfg = new Config();
        cfg.getGroupConfig().setName(groupName);

        JoinConfig joinConfig = cfg.getNetworkConfig().getJoin();
        joinConfig.getMulticastConfig().setEnabled(false);
        TcpIpConfig tcpIpConfig = joinConfig.getTcpIpConfig().setEnabled(true);
        members.forEach(tcpIpConfig::addMember);

        return Hazelcast.newHazelcastInstance(cfg);
    }
}
