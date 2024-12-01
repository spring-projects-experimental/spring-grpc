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
package org.springframework.grpc.autoconfigure.client;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.grpc.autoconfigure.client.GrpcClientProperties.NamedChannel;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;
import org.springframework.util.unit.DataSize;

import io.grpc.ManagedChannelBuilder;

/**
 * A {@link GrpcChannelBuilderCustomizer} that maps {@link GrpcClientProperties client
 * properties} to a channel builder.
 *
 * @author David Syer
 * @author Chris Bono
 */
class ClientPropertiesChannelBuilderCustomizer implements GrpcChannelBuilderCustomizer {

	private final GrpcClientProperties properties;

	ClientPropertiesChannelBuilderCustomizer(GrpcClientProperties properties) {
		this.properties = properties;
	}

	@Override
	public void customize(String authority, ManagedChannelBuilder<?> builder) {
		NamedChannel channel = this.properties.getChannels().get(authority);
		if (channel == null) {
			return;
		}
		PropertyMapper mapper = PropertyMapper.get().alwaysApplyingWhenNonNull();
		mapper.from(channel.getUserAgent()).to(builder::userAgent);
		mapper.from(channel.getDefaultLoadBalancingPolicy()).to(builder::defaultLoadBalancingPolicy);
		mapper.from(channel.getMaxInboundMessageSize()).asInt(DataSize::toBytes).to(builder::maxInboundMessageSize);
		mapper.from(channel.getMaxInboundMetadataSize()).asInt(DataSize::toBytes).to(builder::maxInboundMessageSize);
		mapper.from(channel.getKeepAliveTime()).to(durationProperty(builder::keepAliveTime));
		mapper.from(channel.getKeepAliveTimeout()).to(durationProperty(builder::keepAliveTimeout));
		mapper.from(channel.getIdleTimeout()).to(durationProperty(builder::idleTimeout));
		mapper.from(channel.isKeepAliveWithoutCalls()).to(builder::keepAliveWithoutCalls);
		if (channel.getHealth().isEnabled()) {
			String serviceNameToCheck = channel.getHealth().getServiceName() != null
					? channel.getHealth().getServiceName() : "";
			Map<String, ?> healthCheckConfig = Map.of("healthCheckConfig", Map.of("serviceName", serviceNameToCheck));
			builder.defaultServiceConfig(healthCheckConfig);
		}
	}

	Consumer<Duration> durationProperty(BiConsumer<Long, TimeUnit> setter) {
		return (duration) -> setter.accept(duration.toNanos(), TimeUnit.NANOSECONDS);
	}

}
