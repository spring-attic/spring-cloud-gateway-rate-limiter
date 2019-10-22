package org.springframework.cloud.gateway.ratelimiter;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
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
