package caching.hzdemo;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class Tester {

    // super hacky - but DNS records X.peer.apps.internal will only be available after application started
    // so InitializingBean or any other option to start cluster during app startup aren't going to work
    @Scheduled(initialDelay = 5000, fixedDelay = Integer.MAX_VALUE)
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
    }
}

@SpringBootApplication
@EnableScheduling
public class HzDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(HzDemoApplication.class, args);
    }

}
