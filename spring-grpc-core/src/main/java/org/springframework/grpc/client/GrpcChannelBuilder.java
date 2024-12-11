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

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import io.grpc.ClientInterceptor;
import io.grpc.ForwardingChannelBuilder2;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * An extended {@link ManagedChannelBuilder} that allows setting the shutdown grace period
 * for created channels as well as more options for client interceptors.
 *
 * @author Chris Bono
 */
public class GrpcChannelBuilder extends ForwardingChannelBuilder2<GrpcChannelBuilder> {

	private final ManagedChannelBuilder<?> delegate;

	private final Function<GrpcChannelBuilder, ManagedChannel> channelFactory;

	private Duration shutdownGracePeriod;

	private boolean mergeInterceptors;

	GrpcChannelBuilder(ManagedChannelBuilder<?> delegate, Function<GrpcChannelBuilder, ManagedChannel> channelFactory) {
		this.delegate = delegate;
		this.channelFactory = channelFactory;
	}

	/**
	 * Gets the time to wait for the channel to gracefully shutdown (completing all
	 * requests). If set to a negative value, the channel waits forever. If set to
	 * {@code 0} the channel will force shutdown immediately. Defaults to {@code 30s}.
	 * @return the time to wait for a graceful shutdown
	 */
	public Duration shutdownGracePeriod() {
		return this.shutdownGracePeriod;
	}

	/**
	 * Sets the time to wait for the channel to gracefully shutdown (completing all
	 * requests). If set to a negative value, the channel waits forever. If set to
	 * {@code 0} the channel will force shutdown immediately. Defaults to {@code 30s}.
	 * @param shutdownGracePeriod the time to wait for a graceful shutdown
	 * @return the builder instance
	 */
	public GrpcChannelBuilder shutdownGracePeriod(Duration shutdownGracePeriod) {
		this.shutdownGracePeriod = shutdownGracePeriod;
		return this;
	}

	/**
	 * Sets whether the provided interceptors should be merged and sorted with the global
	 * interceptors.
	 * @param mergeInterceptors whether the provided interceptors should be merged and
	 * sorted with the global interceptors or appended to
	 * @return the builder instance
	 */
	public GrpcChannelBuilder mergeInterceptors(boolean mergeInterceptors) {
		this.mergeInterceptors = mergeInterceptors;
		return this;
	}

	@Override
	public GrpcChannelBuilder intercept(List<ClientInterceptor> interceptors) {
		// Do things w/ the interceptors here based on the merge property
		return super.intercept(interceptors);
	}

	@Override
	protected ManagedChannelBuilder<?> delegate() {
		return this.delegate;
	}

	@Override
	public ManagedChannel build() {
		return this.channelFactory.apply(this);
	}

}
