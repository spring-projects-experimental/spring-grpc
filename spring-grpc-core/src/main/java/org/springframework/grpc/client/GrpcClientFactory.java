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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.grpc.internal.ClasspathScanner;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import io.grpc.ManagedChannel;
import io.grpc.stub.AbstractStub;

/**
 * A factory of gRPC clients that can be used to create client stubs as beans in an
 * application context. The best way to interact with the factory is to declare a bean of
 * type {@link GrpcClientFactoryCustomizer} in the application context. The customizer
 * will be called with the factory before it is used to create the beans.
 *
 * @author Dave Syer
 */
public class GrpcClientFactory {

	private static Map<Class<?>, StubFactory<?>> FACTORIES = new HashMap<>();

	private final ApplicationContext context;

	private Map<String, Supplier<ManagedChannel>> options = new HashMap<>();

	static {
		stubs(new BlockingStubFactory());
		stubs(new BlockingV2StubFactory());
		stubs(new FutureStubFactory());
		stubs(new ReactorStubFactory());
		stubs(new SimpleStubFactory());
		SpringFactoriesLoader.loadFactories(StubFactory.class, GrpcClientFactory.class.getClassLoader())
			.forEach(GrpcClientFactory::stubs);
	}

	public GrpcClientFactory(ApplicationContext context) {
		this.context = context;
	}

	public <T extends AbstractStub<T>> T getClient(String target, Class<T> type, Class<?> factory) {
		StubFactory<?> stubs = findFactory(factory, type);
		Supplier<ManagedChannel> channel = this.options.get(target);
		if (channel == null) {
			channel = () -> channels().createChannel(target, ChannelBuilderOptions.defaults());
		}
		Supplier<ManagedChannel> finalChannel = channel;
		@SuppressWarnings("unchecked")
		T client = (T) stubs.create(() -> finalChannel.get(), type);
		return client;
	}

	/**
	 * Register a channel factory for the given target. The channel will be created using
	 * the given options. If no options are provided, the default options will be used.
	 * @param target the name (or base url) of the target
	 * @param options the options to use to create the channel
	 */
	public void channel(String target, ChannelBuilderOptions options) {
		this.options.put(target, () -> channels().createChannel(target, options));
	}

	private static void stubs(StubFactory<? extends AbstractStub<?>> factory) {
		FACTORIES.put(factory.getClass(), factory);
	}

	public static StubFactory<?> findFactory(Class<?> type) {
		return findFactory(null, type);
	}

	private static StubFactory<?> findFactory(Class<?> factoryType, Class<?> type) {
		StubFactory<? extends AbstractStub<?>> factory = null;
		if (factoryType != null && factoryType != UnspecifiedStubFactory.class) {
			factory = FACTORIES.get(factoryType);
			if (!factory.supports(type)) {
				factory = null;
			}
		}
		else {
			List<StubFactory<?>> factories = new ArrayList<>(FACTORIES.values());
			AnnotationAwareOrderComparator.sort(factories);
			for (StubFactory<? extends AbstractStub<?>> value : factories) {
				if (value.supports(type)) {
					factory = value;
					break;
				}
			}
		}
		return factory;
	}

	private GrpcChannelFactory channels() {
		return this.context.getBean(GrpcChannelFactory.class);
	}

	public static void register(BeanDefinitionRegistry registry, GrpcClientRegistrationSpec spec) {
		for (Class<?> type : spec.types()) {
			StubFactory<?> factory = GrpcClientFactory.findFactory(spec.factory(), type);
			if (factory == null) {
				continue;
			}
			RootBeanDefinition beanDef = (RootBeanDefinition) BeanDefinitionBuilder.rootBeanDefinition(type)
				.setLazyInit(true)
				.setFactoryMethodOnBean("getClient", GrpcClientFactoryPostProcessor.class.getName())
				.addConstructorArgValue(spec.target())
				.addConstructorArgValue(type)
				.addConstructorArgValue(spec.factory())
				.getBeanDefinition();
			beanDef.setTargetType(type);
			String beanName = StringUtils.hasText(spec.prefix()) ? spec.prefix() + type.getSimpleName()
					: StringUtils.uncapitalize(type.getSimpleName());
			if (!registry.containsBeanDefinition(beanName)) {
				// Avoid registering the same bean definition multiple times
				registry.registerBeanDefinition(beanName, beanDef);
			}
		}
	}

	public record GrpcClientRegistrationSpec(String prefix, Class<? extends StubFactory<?>> factory, String target,
			Class<?>[] types) {

		private static ClasspathScanner SCANNER = new ClasspathScanner();

		public static GrpcClientRegistrationSpec defaults() {
			return new GrpcClientRegistrationSpec("default", new Class[0]);
		}

		public static GrpcClientRegistrationSpec of(String target) {
			return new GrpcClientRegistrationSpec(target, new Class[0]);
		}

		public GrpcClientRegistrationSpec(String target, Class<?>[] types) {
			this("", UnspecifiedStubFactory.class, target, types);
		}

		public GrpcClientRegistrationSpec(String prefix, String target, Class<?>[] types) {
			this(prefix, UnspecifiedStubFactory.class, target, types);
		}

		public GrpcClientRegistrationSpec factory(Class<? extends StubFactory<?>> factory) {
			return new GrpcClientRegistrationSpec(this.prefix, factory, this.target, this.types);
		}

		public GrpcClientRegistrationSpec types(Class<?>... types) {
			return new GrpcClientRegistrationSpec(this.prefix, this.factory, this.target, types);
		}

		public GrpcClientRegistrationSpec prefix(String prefix) {
			if (StringUtils.hasText(prefix)) {
				return new GrpcClientRegistrationSpec(prefix, this.factory, this.target, this.types);
			}
			else {
				return new GrpcClientRegistrationSpec("", this.factory, this.target, this.types);
			}
		}

		public GrpcClientRegistrationSpec packages(String... packages) {
			Set<Class<?>> allTypes = new HashSet<>();
			allTypes.addAll(Set.of(this.types));
			for (String basePackage : packages) {
				// TODO: find a global factory default if scanning and only add the types
				// that it supports
				allTypes.addAll(SCANNER.scan(basePackage, AbstractStub.class));
			}
			@SuppressWarnings("unchecked")
			Class<? extends AbstractStub<?>>[] newTypes = allTypes.toArray(new Class[0]);
			return new GrpcClientRegistrationSpec(this.prefix, this.factory, this.target, newTypes);
		}

		public GrpcClientRegistrationSpec packageClasses(Class<?>... packageClasses) {
			String[] packages = new String[packageClasses.length];
			for (Class<?> basePackageClass : packageClasses) {
				for (int i = 0; i < packageClasses.length; i++) {
					String basePackage = ClassUtils.getPackageName(basePackageClass);
					packages[i] = basePackage;
				}
			}
			return packages(packages);
		}
	}

}
