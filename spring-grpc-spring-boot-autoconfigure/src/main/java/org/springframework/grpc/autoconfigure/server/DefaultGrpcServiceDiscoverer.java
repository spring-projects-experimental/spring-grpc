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

package org.springframework.grpc.autoconfigure.server;

import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.grpc.server.GrpcServiceDiscoverer;
import org.springframework.lang.Nullable;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;

/**
 * The default {@link GrpcServiceDiscoverer} that finds all {@link BindableService} beans
 * and configures and binds them.
 *
 * @author Chris Bono
 */
public class DefaultGrpcServiceDiscoverer implements GrpcServiceDiscoverer {

	private final GrpcServiceConfigurer serviceConfigurer;

	private final ApplicationContext applicationContext;

	public DefaultGrpcServiceDiscoverer(GrpcServiceConfigurer serviceConfigurer,
			ApplicationContext applicationContext) {
		this.serviceConfigurer = serviceConfigurer;
		this.applicationContext = applicationContext;
	}

	@Override
	public List<ServerServiceDefinition> findServices() {
		return ApplicationContextBeanLookupUtils
			.getOrderedBeansWithAnnotation(this.applicationContext, BindableService.class, GrpcService.class)
			.entrySet()
			.stream()
			.map((e) -> this.serviceConfigurer.configure(e.getKey(), this.serviceInfo(e.getValue())))
			.toList();
	}

	@Nullable
	private GrpcServiceInfo serviceInfo(@Nullable GrpcService grpcService) {
		return grpcService != null ? GrpcServiceInfo.from(grpcService) : null;
	}

}
