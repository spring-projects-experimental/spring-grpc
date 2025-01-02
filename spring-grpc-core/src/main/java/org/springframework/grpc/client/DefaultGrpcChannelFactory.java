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

package org.springframework.grpc.client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.Assert;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Default implementation of {@link GrpcChannelFactory} for creating and managing gRPC
 * channels.
 * <p>
 * Implements {@link DisposableBean} to shut down channels when no longer needed.
 *
 * @param <T> concrete type of channel builder used to create the channels
 * @author David Syer
 * @author Chris Bono
 */
public class DefaultGrpcChannelFactory<T extends ManagedChannelBuilder<T>>
		implements GrpcChannelFactory, DisposableBean {

	private final List<ManagedChannelWithShutdown> channels = new ArrayList<>();

	private final List<GrpcChannelBuilderCustomizer<T>> globalCustomizers = new ArrayList<>();

	private final ClientInterceptorsConfigurer interceptorsConfigurer;

	private ChannelCredentialsProvider credentials = ChannelCredentialsProvider.INSECURE;

	private VirtualTargets targets = VirtualTargets.DEFAULT;

	/**
	 * Construct a channel factory instance.
	 * @param globalCustomizers the global customizers to apply to all created channels
	 * @param interceptorsConfigurer configures the client interceptors on the created
	 * channels
	 */
	public DefaultGrpcChannelFactory(List<GrpcChannelBuilderCustomizer<T>> globalCustomizers,
			ClientInterceptorsConfigurer interceptorsConfigurer) {
		Assert.notNull(globalCustomizers, () -> "customizers must not be null");
		Assert.notNull(interceptorsConfigurer, () -> "interceptorsConfigurer must not be null");
		this.globalCustomizers.addAll(globalCustomizers);
		this.interceptorsConfigurer = interceptorsConfigurer;
	}

	public void setVirtualTargets(VirtualTargets targets) {
		this.targets = targets;
	}

	public void setCredentialsProvider(ChannelCredentialsProvider credentials) {
		this.credentials = credentials;
	}

	@Override
	public ManagedChannel createChannel(String target, ChannelBuilderOptions options) {
		var targetUri = this.targets.getTarget(target);
		T builder = newChannelBuilder(targetUri, this.credentials.getChannelCredentials(target));
		// Handle interceptors
		this.interceptorsConfigurer.configureInterceptors(builder, options.interceptors(),
				options.mergeWithGlobalInterceptors());
		// Handle customizers
		this.globalCustomizers.forEach((c) -> c.customize(target, builder));
		var customizer = options.<T>customizer();
		if (customizer != null) {
			customizer.customize(target, builder);
		}
		var channel = builder.build();
		var shutdownGracePeriod = options.shutdownGracePeriod();
		this.channels.add(new ManagedChannelWithShutdown(channel, shutdownGracePeriod));
		return channel;
	}

	/**
	 * Creates a new {@link ManagedChannelBuilder} instance for the given target and
	 * credentials. The {@code target} is a valid nameresolver-compliant URI or an
	 * authority string as described in {@link Grpc#newChannelBuilder}.
	 * @param target the target of the channel
	 * @param credentials the credentials for the channel
	 * @return a new builder for the given target and credentials
	 */
	@SuppressWarnings("unchecked")
	protected T newChannelBuilder(String target, ChannelCredentials credentials) {
		return (T) Grpc.newChannelBuilder(target, credentials);
	}

	@Override
	public void destroy() {
		this.channels.forEach((c) -> {
			var shutdownGracePeriod = c.shutdownGracePeriod();
			var channel = c.channel();
			// TODO use grace period to do the magical shutdown here
			channel.shutdown();
		});
	}

	record ManagedChannelWithShutdown(ManagedChannel channel, Duration shutdownGracePeriod) {
	}

}
