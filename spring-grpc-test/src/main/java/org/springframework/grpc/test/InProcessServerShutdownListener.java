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

package org.springframework.grpc.test;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

import io.grpc.ManagedChannel;
import io.grpc.Server;

/**
 * Listener that automatically shuts down the in-process gRPC {@link Server} and
 * {@link ManagedChannel} when the Spring
 * {@link org.springframework.context.ApplicationContext} is closed. This helps to ensure
 * proper resource cleanup after the application context is no longer active, avoiding the
 * need for manual shutdown calls in tests or other classes.
 *
 * @author Andrei Lisa
 */
final class InProcessServerShutdownListener implements ApplicationListener<ContextClosedEvent> {

	private final Server grpcServer;

	private final ManagedChannel grpcChannel;

	InProcessServerShutdownListener(Server grpcServer, ManagedChannel grpcChannel) {
		this.grpcServer = grpcServer;
		this.grpcChannel = grpcChannel;
	}

	@Override
	public void onApplicationEvent(ContextClosedEvent event) {
		if (grpcChannel != null) {
			grpcChannel.shutdownNow();
		}
		if (grpcServer != null) {
			grpcServer.shutdownNow();
		}
	}

}
