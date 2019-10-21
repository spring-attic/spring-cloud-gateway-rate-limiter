package org.springframework.cloud.gateway.ratelimiter;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.ratelimiter.cluster.MemberInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootApplication
class TestApp {

	@Bean
	RateLimiter rateLimiter(Validator defaultValidator) {
		HazelcastClusterInitializer hazelcastClusterInitializer = new HazelcastClusterInitializer("test-group", Mono.just(Collections.singletonList(new MemberInfo("localhost", 5671))));
		HazelcastBucket4JRequestCounterFactory counterFactory = new HazelcastBucket4JRequestCounterFactory(hazelcastClusterInitializer);
		return new DefaultRateLimiter(defaultValidator, counterFactory);
	}

	@Bean
	KeyResolver keyResolver() {
		return exchange -> Mono.just("test-key");
	}

}

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HazelcastRateLimiterComponentTest {

	private final Logger logger = LoggerFactory.getLogger(HazelcastRateLimiterComponentTest.class);

	@LocalServerPort
	int port;
	private WebClient webClient;

	@BeforeEach
	void setUp() {
		webClient = WebClient.builder().baseUrl("http://localhost:" + port).build();
	}

	@Test
	void shouldLimitNumberOfRequests() {
		AtomicInteger counter = new AtomicInteger();
		int cpus = Runtime.getRuntime().availableProcessors();

		logger.info("Running {} concurrent requests", cpus);

		Flux.range(0, cpus)
		    .parallel(cpus)
		    .runOn(Schedulers.parallel())
		    .flatMap(integer -> webClient.head().uri("/github")
		                                 .exchange()
		                                 .map(ClientResponse::statusCode)
		                                 .doOnNext(httpStatus -> {
			                                 logger.info("Got " + httpStatus);
			                                 if (httpStatus == HttpStatus.OK) {
				                                 counter.incrementAndGet();
			                                 }
		                                 }))
		    .sequential()
		    .blockLast();

		assertThat(counter.get()).isEqualTo(3);
	}
}
