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

import java.util.function.Supplier;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.stub.AbstractStub;

public abstract class AbstractStubFactory<T extends AbstractStub<?>> implements StubFactory<T> {

	private final Class<? extends AbstractStub<?>> baseType;

	@SuppressWarnings("unchecked")
	protected AbstractStubFactory(Class<?> baseType) {
		this.baseType = (Class<? extends AbstractStub<?>>) baseType;
	}

	@Override
	public boolean supports(Class<?> type) {
		return this.baseType.isAssignableFrom(type);
	}

	@Override
	public T create(Supplier<ManagedChannel> channel, Class<? extends AbstractStub<?>> type) {
		Class<?> factory = type.getEnclosingClass();
		@SuppressWarnings("unchecked")
		T stub = (T) createStub(channel, factory, methodName());
		return stub;
	}

	private Object createStub(Supplier<ManagedChannel> channel, Class<?> factory, String method) {
		try {
			return factory.getMethod(method, Channel.class).invoke(null, channel.get());
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to create stub", e);
		}
	}

	protected abstract String methodName();

}
