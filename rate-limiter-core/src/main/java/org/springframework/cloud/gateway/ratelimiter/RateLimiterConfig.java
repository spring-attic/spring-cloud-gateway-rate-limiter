package org.springframework.cloud.gateway.ratelimiter;

import java.time.Duration;

public class RateLimiterConfig {

	private int limit = 0;
	private Duration duration = Duration.ofSeconds(1);

	public Duration getDuration() {
		return duration;
	}

	public int getLimit() {
		return limit;
	}

	public void setDuration(Duration duration) {
		this.duration = duration;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

}
