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

package org.springframework.grpc.autoconfigure.server;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.grpc.ServerBuilder;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.grpc.server.DefaultGrpcServerFactory;

/**
 * Helper class used to map {@link GrpcServerProperties} to
 * {@link DefaultGrpcServerFactory}.
 *
 * @author Chris Bono
 * @param <T> the type of server builder
 */
class DefaultServerFactoryPropertyMapper<T extends ServerBuilder<T>> {

	final GrpcServerProperties properties;

	DefaultServerFactoryPropertyMapper(GrpcServerProperties properties) {
		this.properties = properties;
	}

	/**
	 * Map the properties to the server factory's server builder.
	 * @param serverBuilder the builder
	 */
	void customizeServerBuilder(T serverBuilder) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		GrpcServerProperties.KeepAlive keepAlive = this.properties.getKeepAlive();
		map.from(keepAlive.getTime()).to(durationProperty(serverBuilder::keepAliveTime));
		map.from(keepAlive.getTimeout()).to(durationProperty(serverBuilder::keepAliveTimeout));
	}

	Consumer<Duration> durationProperty(BiConsumer<Long, TimeUnit> setter) {
		return (duration) -> setter.accept(duration.toNanos(), TimeUnit.NANOSECONDS);
	}

}
