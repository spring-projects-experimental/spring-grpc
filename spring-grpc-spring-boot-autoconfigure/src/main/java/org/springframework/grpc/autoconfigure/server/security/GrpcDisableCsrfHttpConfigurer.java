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

import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

/**
 * A custom {@link AbstractHttpConfigurer} that disables CSRF protection for gRPC
 * requests.
 * <p>
 * This configurer checks the application context to determine if CSRF protection should
 * be disabled for gRPC requests based on the property
 * {@code spring.grpc.security.csrf.enabled}. By default, CSRF protection is disabled
 * unless explicitly enabled in the application properties.
 * </p>
 *
 * @see AbstractHttpConfigurer
 * @see HttpSecurity
 * @author Dave Syer
 */
public class GrpcDisableCsrfHttpConfigurer extends AbstractHttpConfigurer<GrpcDisableCsrfHttpConfigurer, HttpSecurity> {

	@Override
	public void init(HttpSecurity http) throws Exception {
		ApplicationContext context = http.getSharedObject(ApplicationContext.class);
		if (context != null && isServletEnabledAndCsrfDisabled(context)) {
			http.csrf(csrf -> csrf.ignoringRequestMatchers(GrpcServletRequest.all()));
		}
	}

	private boolean isServletEnabledAndCsrfDisabled(ApplicationContext context) {
		return context.getEnvironment().getProperty("spring.grpc.server.servlet.enabled", Boolean.class, true)
				&& !context.getEnvironment().getProperty("spring.grpc.security.csrf.enabled", Boolean.class, false);
	}

}
