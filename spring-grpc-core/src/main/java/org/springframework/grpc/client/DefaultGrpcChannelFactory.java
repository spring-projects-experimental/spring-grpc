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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.DisposableBean;

import io.grpc.ForwardingChannelBuilder2;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class DefaultGrpcChannelFactory implements GrpcChannelFactory, DisposableBean {

	private final Map<String, ManagedChannelBuilder<?>> builders = new ConcurrentHashMap<>();

	private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();

	private final List<GrpcChannelConfigurer> configurers = new ArrayList<>();

	public DefaultGrpcChannelFactory() {
		this(List.of());
	}

	public DefaultGrpcChannelFactory(List<GrpcChannelConfigurer> configurers) {
		this.configurers.addAll(configurers);
	}

	@Override
	public ManagedChannelBuilder<?> createChannel(String authority) {
		ManagedChannelBuilder<?> target = builders.computeIfAbsent(authority, path -> {
			ManagedChannelBuilder<?> builder = newChannel(path);
			for (GrpcChannelConfigurer configurer : configurers) {
				configurer.configure(path, builder);
			}
			return builder;
		});
		return new DisposableChannelBuilder(authority, target);

	}

	protected ManagedChannelBuilder<?> newChannel(String path) {
		return Grpc.newChannelBuilder(path, InsecureChannelCredentials.create());
	}

	@Override
	public void destroy() throws Exception {
		for (ManagedChannel channel : channels.values()) {
			channel.shutdown();
		}
	}

	class DisposableChannelBuilder extends ForwardingChannelBuilder2<DisposableChannelBuilder> {

		private final ManagedChannelBuilder<?> delegate;

		private final String authority;

		public DisposableChannelBuilder(String authority, ManagedChannelBuilder<?> delegate) {
			this.authority = authority;
			this.delegate = delegate;
		}

		@Override
		protected ManagedChannelBuilder<?> delegate() {
			return delegate;
		}

		@Override
		public ManagedChannel build() {
			ManagedChannel channel = channels.computeIfAbsent(authority, name -> super.build());
			return channel;
		}

	}

}
