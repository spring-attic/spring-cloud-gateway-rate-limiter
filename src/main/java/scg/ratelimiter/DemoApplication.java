package scg.ratelimiter;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.AbstractRateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.validation.Validator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

class HazelcastRateLimiterConfig {

    private int limit;

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}

@Component
class HazelcastRateLimiter extends AbstractRateLimiter<HazelcastRateLimiterConfig> {

    private final Logger logger = LoggerFactory.getLogger(HazelcastRateLimiter.class);

    private IMap<String, Integer> map;
    private AtomicBoolean initialized = new AtomicBoolean(false);

    protected HazelcastRateLimiter(Validator defaultValidator) {
        super(HazelcastRateLimiterConfig.class, "hazelcast-rate-limiter", defaultValidator);
    }

    // super hacky - but DNS records X.peer.apps.internal will only be available shortly after application started
    // so InitializingBean or any other option to start cluster during app startup won't work
    @Scheduled(initialDelay = 3000, fixedDelay = Integer.MAX_VALUE)
    public void run() {
        String groupName = "my-test-group";

        Config cfg = new Config();
        cfg.getGroupConfig().setName(groupName);

        JoinConfig joinConfig = cfg.getNetworkConfig().getJoin();
        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getTcpIpConfig().setEnabled(true)
                .addMember("0.peer.apps.internal")
                .addMember("1.peer.apps.internal");

        HazelcastInstance instance = Hazelcast.newHazelcastInstance(cfg);
        map = instance.getMap("rate-limit");
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
        HazelcastRateLimiterConfig config = getConfig().getOrDefault(routeId, new HazelcastRateLimiterConfig());

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

@Component
class HeaderKeyResolver implements KeyResolver {

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
        return apiKey == null ? Mono.empty() : Mono.just(apiKey);
    }
}

@SpringBootApplication
@EnableScheduling
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}
