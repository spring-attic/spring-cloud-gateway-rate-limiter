package org.springframework.cloud.gateway.ratelimiter;

public class ConsumeResponse {

	private final boolean isAllowed;
	private final long remainingRequests;
	private final long retryDelayMs;

	public ConsumeResponse(boolean isAllowed, long remainingRequests, long retryDelayMs) {
		this.isAllowed = isAllowed;
		this.remainingRequests = remainingRequests;
		this.retryDelayMs = retryDelayMs;
	}

	boolean isAllowed() {
		return this.isAllowed;
	}

	long remainingRequests() {
		return this.remainingRequests;
	}

	long retryDelayMs() {
		return this.retryDelayMs;
	}
}
