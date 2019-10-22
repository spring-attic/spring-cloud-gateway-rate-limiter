package org.springframework.cloud.gateway.ratelimiter;

import reactor.blockhound.BlockHound;

class BlockHoundHelper {

	private static volatile boolean isEnabled;

	static void install(boolean enabled) {
		isEnabled = enabled;

		BlockHound.install(builder -> builder.blockingMethodCallback(it -> {
			if (isEnabled) {
				new Error(it.toString()).printStackTrace();
			}
		}));
	}

	static void enable() {
		isEnabled = true;
	}

	static void disable() {
		isEnabled = false;
	}
}
