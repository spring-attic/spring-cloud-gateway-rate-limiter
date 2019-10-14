package org.springframework.cloud.gateway.ratelimiter;

public class RateLimiterConfig {

	private int limit;

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

}
