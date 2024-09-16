/*
 * Copyright 2024-2024 the original author or authors.
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
 *
 */
package org.springframework.grpc.server;

import static java.util.Objects.requireNonNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Lists;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;

public class DefaultGrpcServerFactory<T extends ServerBuilder<T>> implements GrpcServerFactory {

	private static final String ANY_IP_ADDRESS = "*";

	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	private final List<ServerServiceDefinition> serviceList = Lists.newLinkedList();

	private final List<GrpcServerConfigurer> serverConfigurers;

	private final String address;

	private final int port;

	public DefaultGrpcServerFactory(String address, int port, final List<GrpcServerConfigurer> serverConfigurers) {
		this.serverConfigurers = requireNonNull(serverConfigurers, "serverConfigurers");
		this.address = address;
		this.port = port;
	}

	protected String getAddress() {
		return this.address;
	}

	protected int getPort() {
		return this.port;
	}

	@Override
	public Server createServer() {
		final T builder = newServerBuilder();
		configure(builder);
		return builder.build();
	}

	/**
	 * Creates a new server builder.
	 * @return The newly created server builder.
	 */
	@SuppressWarnings("unchecked")
	protected T newServerBuilder() {
		String address = getAddress();
		int port = getPort();
		if (address != null) {
			if (address.startsWith("unix:")) {
				String path = address.substring(5);
				return unixDomainServerBuilder(path);
			}
			if (!ANY_IP_ADDRESS.equals(address)) {
				return inetSocketServerBuilder(address, port);
			}
			// TODO: Add more support for address resolution
		}
		return (T) ServerBuilder.forPort(port);
	}

	@SuppressWarnings("unchecked")
	private T inetSocketServerBuilder(String path, int port) {
		if (NettyServerFactoryHelper.isAvailable()) {
			return (T) NettyServerFactoryHelper.forInetAddress(path, port);
		}
		else if (ShadedNettyServerFactoryHelper.isAvailable()) {
			return (T) ShadedNettyServerFactoryHelper.forInetAddress(path, port);
		}
		throw new IllegalStateException(
				"Netty Epoll not available. Add io.netty:netty-transport-native-epoll:linux-x86_64 to your classpath.");
	}

	@SuppressWarnings("unchecked")
	private T unixDomainServerBuilder(String path) {
		if (NettyServerFactoryHelper.isEpollAvailable()) {
			return (T) NettyServerFactoryHelper.forUnixDomainSocket(path);
		}
		else if (ShadedNettyServerFactoryHelper.isEpollAvailable()) {
			return (T) ShadedNettyServerFactoryHelper.forUnixDomainSocket(path);
		}
		throw new IllegalStateException(
				"Netty Epoll not available. Add io.netty:netty-transport-native-epoll:linux-x86_64 to your classpath.");
	}

	/**
	 * Configures the given server builder. This method can be overwritten to add features
	 * that are not yet supported by this library or use a {@link GrpcServerConfigurer}
	 * instead.
	 * @param builder The server builder to configure.
	 */
	protected void configure(final T builder) {
		configureServices(builder);
		for (final GrpcServerConfigurer serverConfigurer : this.serverConfigurers) {
			serverConfigurer.accept(builder);
		}
	}

	/**
	 * Configures the services that should be served by the server.
	 * @param builder The server builder to configure.
	 */
	protected void configureServices(final T builder) {
		final Set<String> serviceNames = new LinkedHashSet<>();

		for (final ServerServiceDefinition service : this.serviceList) {
			final String serviceName = service.getServiceDescriptor().getName();
			if (!serviceNames.add(serviceName)) {
				throw new IllegalStateException("Found duplicate service implementation: " + serviceName);
			}
			logger.info("Registered gRPC service: " + serviceName);
			builder.addService(service);
		}
	}

	@Override
	public void addService(final ServerServiceDefinition service) {
		this.serviceList.add(service);
	}

}
