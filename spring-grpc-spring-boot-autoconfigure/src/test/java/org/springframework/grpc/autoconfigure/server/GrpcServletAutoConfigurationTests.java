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

import java.util.concurrent.atomic.AtomicBoolean;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.internal.GrpcUtil;
import io.grpc.servlet.jakarta.GrpcServlet;
import io.grpc.servlet.jakarta.ServletAdapter;
import io.grpc.servlet.jakarta.ServletServerBuilder;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.grpc.server.ServerBuilderCustomizer;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link GrpcServerAutoConfiguration}.
 *
 * @author Chris Bono
 * @author Toshiaki Maki
 */
class GrpcServletAutoConfigurationTests {

	private WebApplicationContextRunner contextRunner() {
		BindableService service = mock();
		ServerServiceDefinition serviceDefinition = ServerServiceDefinition.builder("my-service").build();
		when(service.bindService()).thenReturn(serviceDefinition);
		// NOTE: we use noop server lifecycle to avoid startup
		return new WebApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(GrpcServerAutoConfiguration.class, GrpcServerFactoryAutoConfiguration.class))
			.withBean(BindableService.class, () -> service);
	}

	@Test
	void whenGrpcNotOnClasspathAutoConfigurationIsSkipped() {
		this.contextRunner()
			.withClassLoader(new FilteredClassLoader(BindableService.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerAutoConfiguration.class)
				.doesNotHaveBean(ServletRegistrationBean.class));
	}

	@Test
	void whenNoBindableServicesRegisteredAutoConfigurationIsSkipped() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(GrpcServerAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerAutoConfiguration.class)
				.doesNotHaveBean(ServletRegistrationBean.class));
	}

	@Test
	void whenWebApplicationServletIsAutoConfigured() {
		this.contextRunner().run((context) -> {
			assertThat(context).getBean(ServletRegistrationBean.class)
				.isNotNull()
				.extracting("servlet")
				.isInstanceOf(GrpcServlet.class)
				.extracting("servletAdapter")
				.isInstanceOf(ServletAdapter.class)
				.extracting("maxInboundMessageSize")
				.isEqualTo(GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE);
		});
	}

	@Test
	void whenCustomizerIsRegistered() {
		AtomicBoolean invoked = new AtomicBoolean(false);
		ServerBuilderCustomizer<ServletServerBuilder> customizer = serverBuilder -> invoked.set(true);
		this.contextRunner().withBean(ServerBuilderCustomizer.class, () -> customizer).run(context -> {
			assertThat(context).getBean(ServletRegistrationBean.class).isNotNull();
			assertThat(invoked.get()).isTrue();
		});
	}

	@Test
	void whenMaxInboundMessageSizeIsConfigured() {
		this.contextRunner().withPropertyValues("spring.grpc.server.max-inbound-message-size=10KB").run(context -> {
			assertThat(context).getBean(ServletRegistrationBean.class)
				.isNotNull()
				.extracting("servlet")
				.isInstanceOf(GrpcServlet.class)
				.extracting("servletAdapter")
				.isInstanceOf(ServletAdapter.class)
				.extracting("maxInboundMessageSize")
				.isEqualTo((int) DataSize.ofKilobytes(10).toBytes());
		});
	}

}
