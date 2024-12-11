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
 * Implements {@link DisposableBean} to shut down channels when no longer needed
 *
 * @author David Syer
 * @author Chris Bono
 */
public class DefaultGrpcChannelFactory implements GrpcChannelFactory, DisposableBean {

	private final List<ManagedChannelWithShutdown> channels = new ArrayList<>();

	private final List<GrpcChannelBuilderCustomizer> customizers = new ArrayList<>();

	private final ClientInterceptorsConfigurer interceptorsConfigurer;

	private ChannelCredentialsProvider credentials = ChannelCredentialsProvider.INSECURE;

	private VirtualTargets targets = VirtualTargets.DEFAULT;

	public DefaultGrpcChannelFactory(List<GrpcChannelBuilderCustomizer> customizers,
			ClientInterceptorsConfigurer interceptorsConfigurer) {
		Assert.notNull(customizers, () -> "customizers must not be null");
		Assert.notNull(interceptorsConfigurer, () -> "interceptorsConfigurer must not be null");
		this.customizers.addAll(customizers);
		this.interceptorsConfigurer = interceptorsConfigurer;
	}

	public void setVirtualTargets(VirtualTargets targets) {
		this.targets = targets;
	}

	public void setCredentialsProvider(ChannelCredentialsProvider credentials) {
		this.credentials = credentials;
	}

	@Override
	public GrpcChannelBuilder createChannel(String target) {
		ManagedChannelBuilder<?> builder = newChannelBuilder(this.targets.getTarget(target),
				this.credentials.getChannelCredentials(target));
		this.customizers.forEach((c) -> c.customize(target, builder));
		return new GrpcChannelBuilder(builder, this::buildAndRegisterChannel);
	}

	/**
	 * Creates a new {@link ManagedChannelBuilder} instance for the given target path and
	 * credentials.
	 * @param path the target path for the channel
	 * @param creds the credentials for the channel
	 * @return a new builder for the given path and credentials
	 */
	protected ManagedChannelBuilder<?> newChannelBuilder(String path, ChannelCredentials creds) {
		return Grpc.newChannelBuilder(path, creds);
	}

	private ManagedChannel buildAndRegisterChannel(GrpcChannelBuilder channelBuilder) {
		ManagedChannel channel = channelBuilder.delegate().build();
		this.channels.add(new ManagedChannelWithShutdown(channel, channelBuilder.shutdownGracePeriod()));
		return channel;
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
