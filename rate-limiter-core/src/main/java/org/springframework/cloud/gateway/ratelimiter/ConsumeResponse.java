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

	public boolean isAllowed() {
		return this.isAllowed;
	}

	public long remainingRequests() {
		return this.remainingRequests;
	}

	public long retryDelayMs() {
		return this.retryDelayMs;
	}
}
