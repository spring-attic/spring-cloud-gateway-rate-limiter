package sample;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.ratelimiter.HazelcastRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.validation.Validator;

import java.util.Arrays;
import java.util.List;

@SpringBootApplication
@EnableScheduling
public class DemoApplication {

    @Autowired
    private HazelcastRateLimiter hazelcastRateLimiter;

    @Bean
    public HazelcastRateLimiter hazelcastRateLimiter(Validator defaultValidator) {
        return new HazelcastRateLimiter(defaultValidator);
    }

    // super hacky - but DNS records X.peer.apps.internal will only be available shortly after application started
    // so InitializingBean or any other option to start cluster during app startup won't work
    @Scheduled(initialDelay = 3000, fixedDelay = Integer.MAX_VALUE)
    void startHazelcastCluster() {
        List<String> members = Arrays.asList("0.peer.apps.internal", "1.peer.apps.internal");
        hazelcastRateLimiter.initialize(members);
    }

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}
