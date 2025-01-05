/*
 * Copyright 2024-2024 the original author or authors.
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
package org.springframework.grpc.autoconfigure.client;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.grpc.client.NamedChannel;

@ConfigurationProperties(prefix = "spring.grpc.client")
public class GrpcClientProperties {

	@NestedConfigurationProperty
	private final NamedChannel defaultChannel = new NamedChannel();

	private final Map<String, NamedChannel> channels = new HashMap<>();

	GrpcClientProperties() {
		this.defaultChannel.setAddress("static://localhost:9090");
	}

	public Map<String, NamedChannel> getChannels() {
		return this.channels;
	}

	/**
	 * Gets the default {@link NamedChannel} configured for the GRPC client.
	 * @return the default {@link NamedChannel}
	 */
	public NamedChannel getDefaultChannel() {
		return this.defaultChannel;
	}

}
