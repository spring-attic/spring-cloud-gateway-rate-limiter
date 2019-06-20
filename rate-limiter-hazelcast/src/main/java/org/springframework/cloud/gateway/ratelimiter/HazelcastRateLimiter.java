package org.springframework.cloud.gateway.ratelimiter;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.AbstractRateLimiter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.validation.Validator;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private IMap<String, Integer> map;
    private AtomicBoolean initialized = new AtomicBoolean(false);

    public HazelcastRateLimiter(Validator defaultValidator) {
        super(HazelcastRateLimiter.RateLimiterConfig.class, "hazelcast-rate-limiter", defaultValidator);
    }

    public void initialize(List<String> members) {
        String groupName = "my-test-group";

        Config cfg = new Config();
        cfg.getGroupConfig().setName(groupName);

        JoinConfig joinConfig = cfg.getNetworkConfig().getJoin();
        joinConfig.getMulticastConfig().setEnabled(false);
        TcpIpConfig tcpIpConfig = joinConfig.getTcpIpConfig().setEnabled(true);
        members.forEach(tcpIpConfig::addMember);

        HazelcastInstance instance = Hazelcast.newHazelcastInstance(cfg);
        map = instance.getMap("rate-limits");
        initialized.set(true);
    }


    @Override
    public Mono<Response> isAllowed(String routeId, String id) {
        final Response notAllowed = new Response(false, Collections.emptyMap());

        if (!initialized.get()) {
            logger.info("Cluster is not yet initialized");
            return Mono.just(notAllowed);
        }

        Integer noRequests = map.getOrDefault(id, 0) + 1;
        HazelcastRateLimiter.RateLimiterConfig config = getConfig().getOrDefault(routeId, new HazelcastRateLimiter.RateLimiterConfig());

        logger.info("Current limit is {}, total requests to date {}", config.getLimit(), noRequests);

        if (noRequests >= config.getLimit()) {
            logger.info("Exceeded number of allowed requests");
            return Mono.just(notAllowed);
        }

        map.put(id, noRequests);
        final int remainingRequests = config.getLimit() - noRequests;

        return Mono.just(new Response(true, Collections.singletonMap("X-Remaining-Limit", String.valueOf(remainingRequests))));
    }
}
