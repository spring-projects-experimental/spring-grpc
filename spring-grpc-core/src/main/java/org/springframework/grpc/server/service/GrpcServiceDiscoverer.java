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

package org.springframework.grpc.server.service;

import java.util.List;

import io.grpc.ServerServiceDefinition;

/**
 * Discovers {@link ServerServiceDefinition gRPC services} to be provided by the server.
 *
 * @author Michael (yidongnan@gmail.com)
 * @author Chris Bono
 */
@FunctionalInterface
public interface GrpcServiceDiscoverer {

	/**
	 * Find gRPC services for the server to provide.
	 * @return list of services to add to the server - empty when no services available
	 */
	List<ServerServiceDefinition> findServices();

	/**
	 * Find gRPC service names.
	 * @return list of service names - empty when no services available
	 */
	default List<String> listServiceNames() {
		return findServices().stream()
			.map(ServerServiceDefinition::getServiceDescriptor)
			.map(descriptor -> descriptor.getName())
			.toList();
	}

	/**
	 * Find gRPC service.
	 * @param name the service name
	 * @return a service - null if no service has this name
	 */
	default ServerServiceDefinition findService(String name) {
		return findServices().stream()
			.filter(service -> service.getServiceDescriptor().getName().equals(name))
			.findFirst()
			.orElse(null);
	}

}
