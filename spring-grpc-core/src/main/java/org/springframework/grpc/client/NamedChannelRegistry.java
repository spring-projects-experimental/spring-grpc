/*
 * Copyright 2023-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.grpc.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

/**
 * Provides access to configured {@link NamedChannel channels}.
 *
 * @author Chris Bono
 */
public class NamedChannelRegistry implements EnvironmentAware, VirtualTargets {

	private final NamedChannel defaultChannel;

	private final Map<String, NamedChannel> channels;

	private Environment environment;

	public NamedChannelRegistry(NamedChannel defaultChannel, Map<String, NamedChannel> configuredChannels) {
		this.defaultChannel = defaultChannel;
		this.channels = new ConcurrentHashMap<>(configuredChannels);
		this.environment = new StandardEnvironment();
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Gets the default channel configuration.
	 * @return the default channel
	 */
	public NamedChannel getDefaultChannel() {
		return this.defaultChannel;
	}

	/**
	 * Gets the configured channel with the given name. If no channel is configured for
	 * the specified name then one is created using the default channel as a template.
	 * @param name the name of the channel
	 * @return the configured channel if found, or a newly created channel using the
	 * default channel as a template
	 */
	public NamedChannel getChannel(String name) {
		if ("default".equals(name)) {
			return this.defaultChannel;
		}
		return this.channels.computeIfAbsent(name, authority -> {
			NamedChannel channel = this.defaultChannel.copy();
			if (!authority.contains(":/") && !authority.startsWith("unix:")) {
				authority = "static://" + authority;
			}
			channel.setAddress(authority);
			return channel;
		});
	}

	@Override
	public String getTarget(String authority) {
		NamedChannel channel = this.getChannel(authority);
		String address = channel.getAddress();
		if (address.startsWith("static:") || address.startsWith("tcp:")) {
			address = address.substring(address.indexOf(":") + 1).replaceFirst("/*", "");
		}
		return this.environment.resolvePlaceholders(address);
	}

}
