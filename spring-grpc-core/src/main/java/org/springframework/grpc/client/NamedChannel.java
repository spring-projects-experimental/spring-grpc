package org.springframework.grpc.client;

import java.time.Duration;

public record NamedChannel(String address,
						   String defaultLoadBalancingPolicy, NegotiationType negotiationType,
						   Boolean enableKeepAlive, Duration idleTimeout) {

	public static NamedChannel copyDefaultsFrom(NamedChannel config) {
		return new NamedChannel(config.address(), config.defaultLoadBalancingPolicy(),
				config.negotiationType(), config.enableKeepAlive(), config.idleTimeout());
	}
}
