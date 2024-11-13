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

import java.util.Map;

import org.springframework.context.ConfigurableApplicationContext;

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

/**
 * A factory for managing an in-process gRPC server and channel, with automatic lifecycle
 * management.
 * <p>
 * This class is responsible for initializing, starting, and stopping an in-process gRPC
 * server. It automatically registers all available {@link BindableService} beans in the
 * application context and provides a corresponding {@link ManagedChannel} for
 * communication. The server and channel are managed as singletons, and their lifecycle is
 * tied to the Spring application's lifecycle.
 * </p>
 *
 * @author Andrei Lisa
 */

class InProcessGrpcServerFactory {

	private static final String CHANNEL_NAME = "grpcInProcessChannel";

	private final String serverName;

	private final boolean inProcessEnabled;

	private Server grpcServer;

	private final ConfigurableApplicationContext applicationContext;

	InProcessGrpcServerFactory(ConfigurableApplicationContext applicationContext, boolean inProcessEnabled) {
		this.applicationContext = applicationContext;
		this.serverName = InProcessServerBuilder.generateName();
		this.inProcessEnabled = inProcessEnabled;
	}

	void afterPropertiesSet() {
		if (!inProcessEnabled) {
			return;
		}

		if (grpcServer != null) {
			throw new IllegalStateException("Server already started.");
		}

		InProcessServerBuilder serverBuilder = InProcessServerBuilder.forName(serverName).directExecutor();

		Map<String, BindableService> bindableServices = applicationContext.getBeansOfType(BindableService.class);
		bindableServices.values().forEach(serverBuilder::addService);

		grpcServer = serverBuilder.build();
		try {
			grpcServer.start();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to start in-process gRPC server", e);
		}

		registerChannel();
	}

	private void registerChannel() {
		ManagedChannel grpcChannel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
		applicationContext.getBeanFactory().registerSingleton(CHANNEL_NAME, grpcChannel);
		applicationContext.addApplicationListener(new InProcessServerShutdownListener(grpcServer, grpcChannel));
	}

}
