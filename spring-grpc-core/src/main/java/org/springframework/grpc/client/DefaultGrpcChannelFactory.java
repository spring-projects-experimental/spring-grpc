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
 * */

package org.springframework.grpc.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.Assert;

import io.grpc.ChannelCredentials;
import io.grpc.ForwardingChannelBuilder2;
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

	private final Map<String, ManagedChannelBuilder<?>> builders = new ConcurrentHashMap<>();

	private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();

	private final List<GrpcChannelBuilderCustomizer> customizers = new ArrayList<>();

	private ChannelCredentialsProvider credentials = ChannelCredentialsProvider.INSECURE;

	private VirtualTargets targets = VirtualTargets.DEFAULT;

	public DefaultGrpcChannelFactory() {
		this(List.of());
	}

	public DefaultGrpcChannelFactory(List<GrpcChannelBuilderCustomizer> customizers) {
		Assert.notNull(customizers, () -> "customizers must not be null");
		this.customizers.addAll(customizers);
	}

	public void setVirtualTargets(VirtualTargets targets) {
		this.targets = targets;
	}

	public void setCredentialsProvider(ChannelCredentialsProvider credentials) {
		this.credentials = credentials;
	}

	@Override
	public ManagedChannelBuilder<?> createChannel(String authority) {
		ManagedChannelBuilder<?> target = this.builders.computeIfAbsent(authority, path -> {
			ManagedChannelBuilder<?> builder = newChannel(this.targets.getTarget(path),
					this.credentials.getChannelCredentials(path));
			this.customizers.forEach((c) -> c.customize(path, builder));
			return builder;
		});
		return new DisposableChannelBuilder(authority, target);
	}

	/**
	 * Creates a new {@link ManagedChannelBuilder} instance for the given target path and
	 * credentials.
	 * @param path the target path for the channel
	 * @param creds the credentials for the channel
	 * @return a new {@link ManagedChannelBuilder} for the given path and credentials
	 */
	protected ManagedChannelBuilder<?> newChannel(String path, ChannelCredentials creds) {
		return Grpc.newChannelBuilder(path, creds);
	}

	@Override
	public void destroy() {
		for (ManagedChannel channel : this.channels.values()) {
			channel.shutdown();
		}
	}

	/**
	 * A {@link ManagedChannelBuilder} wrapper that ensures the created channel is
	 * disposed of when no longer needed.
	 */
	class DisposableChannelBuilder extends ForwardingChannelBuilder2<DisposableChannelBuilder> {

		private final ManagedChannelBuilder<?> delegate;

		private final String authority;

		DisposableChannelBuilder(String authority, ManagedChannelBuilder<?> delegate) {
			this.authority = authority;
			this.delegate = delegate;
		}

		@Override
		protected ManagedChannelBuilder<?> delegate() {
			return this.delegate;
		}

		@Override
		public ManagedChannel build() {
			return DefaultGrpcChannelFactory.this.channels.computeIfAbsent(this.authority, name -> super.build());
		}

	}

}
