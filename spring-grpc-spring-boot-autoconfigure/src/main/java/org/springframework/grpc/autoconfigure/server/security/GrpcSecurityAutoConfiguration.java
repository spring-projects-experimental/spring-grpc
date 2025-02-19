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
package org.springframework.grpc.autoconfigure.server.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.autoconfigure.server.ConditionalOnGrpcServerEnabled;
import org.springframework.grpc.autoconfigure.server.GrpcServerFactoryAutoConfiguration;
import org.springframework.grpc.autoconfigure.server.exception.GrpcExceptionHandlerAutoConfiguration;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.ServerBuilderCustomizer;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;
import org.springframework.grpc.server.security.GrpcSecurity;
import org.springframework.grpc.server.security.SecurityContextServerInterceptor;
import org.springframework.grpc.server.security.SecurityGrpcExceptionHandler;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.web.SecurityFilterChain;

import io.grpc.ServerBuilder;
import io.grpc.internal.GrpcUtil;

@ConditionalOnClass(ObjectPostProcessor.class)
@ConditionalOnGrpcServerEnabled
@AutoConfiguration(before = GrpcExceptionHandlerAutoConfiguration.class, after = SecurityAutoConfiguration.class)
public class GrpcSecurityAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	static class ExceptionHandlerAutoConfiguration {

		@Bean
		public GrpcExceptionHandler accessExceptionHandler() {
			return new SecurityGrpcExceptionHandler();
		}

	}

	@ConditionalOnBean(ObjectPostProcessor.class)
	@Configuration(proxyBeanMethods = false)
	@Conditional(GrpcServerFactoryAutoConfiguration.OnNativeGrpcServerCondition.class)
	static class GrpcNativeSecurityConfigurerAutoConfiguration {

		@Bean
		public GrpcSecurity grpcSecurity(ObjectPostProcessor<Object> objectPostProcessor,
				AuthenticationManagerBuilder authenticationManagerBuilder, ApplicationContext context) {
			return new GrpcSecurity(objectPostProcessor, authenticationManagerBuilder, context);
		}

	}

	@ConditionalOnBean(SecurityFilterChain.class)
	@Configuration(proxyBeanMethods = false)
	static class GrpcServletSecurityConfigurerAutoConfiguration {

		@Bean
		@GlobalServerInterceptor
		public SecurityContextServerInterceptor securityContextInterceptor() {
			return new SecurityContextServerInterceptor();
		}

		@Bean
		public <T extends ServerBuilder<T>> ServerBuilderCustomizer<T> securityContextExecutorCustomizer() {
			return (serverBuilder) -> serverBuilder
				.executor(new DelegatingSecurityContextExecutor(GrpcUtil.SHARED_CHANNEL_EXECUTOR.create()));
		}

	}

}
