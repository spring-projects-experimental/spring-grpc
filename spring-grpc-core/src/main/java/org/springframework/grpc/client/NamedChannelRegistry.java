package org.springframework.grpc.client;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

public class NamedChannelRegistry implements EnvironmentAware, VirtualTargets {

	private final NamedChannel defaultChannel;
	private final Map<String, NamedChannel> channels;
	private Environment environment;

	public NamedChannelRegistry(NamedChannel defaultChannel, Map<String, NamedChannel> configuredChannels) {
		this.defaultChannel = defaultChannel;
		this.channels = new HashMap<>(Map.of("default", defaultChannel));
		this.channels.putAll(configuredChannels);
		this.environment = new StandardEnvironment();
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Gets the default {@link NamedChannel} configured for the GRPC client.
	 * @return the default {@link NamedChannel}
	 */
	public NamedChannel getDefaultChannel() {
		return this.defaultChannel;
	}

	public NamedChannel getChannel(String name) {
		if ("default".equals(name)) {
			return this.defaultChannel;
		}
		return this.channels.computeIfAbsent(name, authority -> {
			NamedChannel channel = NamedChannel.copyDefaultsFrom(this.defaultChannel);
			if (!authority.contains(":/") && !authority.startsWith("unix:")) {
				authority = "static://" + authority;
			}
			// TODO: have to tweak this a bit as records are immutable
			//channel.address(authority);
			return channel;
		});
	}

	@Override
	public String getTarget(String authority) {
		NamedChannel channel = getChannel(authority);
		String address = channel.address();
		if (address.startsWith("static:") || address.startsWith("tcp:")) {
			address = address.substring(address.indexOf(":") + 1).replaceFirst("/*", "");
		}
		address = this.environment.resolvePlaceholders(address);
		return address.toString();
	}
}
