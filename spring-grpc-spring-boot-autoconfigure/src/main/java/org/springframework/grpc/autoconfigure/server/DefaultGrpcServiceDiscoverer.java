/*
 * Copyright (c) 2016-2023 The gRPC-Spring Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.grpc.autoconfigure.server;

import java.util.List;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.grpc.server.GrpcServiceDiscoverer;

/**
 * The default {@link GrpcServiceDiscoverer} that finds all {@link BindableService} beans
 * and configures and binds them.
 *
 * @author Michael (yidongnan@gmail.com)
 * @author Chris Bono
 */
class DefaultGrpcServiceDiscoverer implements GrpcServiceDiscoverer {

	private final ObjectProvider<BindableService> grpcServicesProvider;

	DefaultGrpcServiceDiscoverer(ObjectProvider<BindableService> grpcServicesProvider) {
		this.grpcServicesProvider = grpcServicesProvider;
	}

	@Override
	public List<ServerServiceDefinition> findServices() {
		return grpcServicesProvider.orderedStream().map(BindableService::bindService).toList();
	}

}
