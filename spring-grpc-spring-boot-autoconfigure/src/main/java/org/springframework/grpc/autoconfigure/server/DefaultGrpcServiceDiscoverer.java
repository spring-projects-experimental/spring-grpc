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

import java.util.HashMap;
import java.util.List;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.grpc.server.GrpcServiceDiscoverer;

/**
 * The default {@link GrpcServiceDiscoverer} that finds all {@link BindableService} beans
 * and configures and binds them.
 *
 * @author Chris Bono
 */
class DefaultGrpcServiceDiscoverer implements GrpcServiceDiscoverer {

	private final ObjectProvider<BindableService> grpcServicesProvider;

	private final ObjectProvider<ServerInterceptor> serverInterceptorsProvider;

	private final ApplicationContext applicationContext;

	public DefaultGrpcServiceDiscoverer(ObjectProvider<BindableService> grpcServicesProvider,
			ObjectProvider<ServerInterceptor> serverInterceptorsProvider, ApplicationContext applicationContext) {
		this.grpcServicesProvider = grpcServicesProvider;
		this.serverInterceptorsProvider = serverInterceptorsProvider;
		this.applicationContext = applicationContext;
	}

	@Override
	public List<ServerServiceDefinition> findServices() {
		List<ServerInterceptor> globalInterceptors = findGlobalInterceptors();
		return grpcServicesProvider.orderedStream()
			.map(BindableService::bindService)
			.map((svc) -> ServerInterceptors.interceptForward(svc, globalInterceptors))
			.toList();
	}

	// VisibleForTesting
	List<ServerInterceptor> findGlobalInterceptors() {
		// We find unordered map of beans (keyed by name) with the annotation and then
		// reverse the map for easy lookup by bean.
		// We then get an ordered stream of all server interceptors and filter
		// out those that are not present in map of annotated interceptor beans.
		var nameToBeanMap = applicationContext.getBeansWithAnnotation(GlobalServerInterceptor.class);
		var beanToNameMap = new HashMap<Object, String>();
		nameToBeanMap.forEach((name, bean) -> beanToNameMap.put(bean, name));
		return this.serverInterceptorsProvider.orderedStream().filter(beanToNameMap::containsKey).toList();
	}

}
