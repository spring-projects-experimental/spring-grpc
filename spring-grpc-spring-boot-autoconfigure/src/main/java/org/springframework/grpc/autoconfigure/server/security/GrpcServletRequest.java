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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.boot.security.servlet.ApplicationContextRequestMatcher;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;

import io.grpc.BindableService;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Factory for a request matcher used to match against resource locations for gRPC
 * services.
 */
public class GrpcServletRequest {

	private GrpcServletRequest() {
	}

	/**
	 * Returns a matcher that includes all gRPC services from the application context. The
	 * {@link GrpcServletRequestMatcher#excluding(Class<?>...) excluding} method can be
	 * used to remove specific services by class if required. For example:
	 * <pre class="code">
	 * GrpcServletRequest.all().excluding(MyCustomService.class)
	 * </pre>
	 * @return the configured {@link RequestMatcher}
	 */
	public static GrpcServletRequestMatcher all() {
		return new GrpcServletRequestMatcher();
	}

	/**
	 * The request matcher used to match against resource locations.
	 */
	public static final class GrpcServletRequestMatcher extends ApplicationContextRequestMatcher<ApplicationContext> {

		private final Set<Class<?>> exclusions;

		private volatile RequestMatcher delegate;

		private GrpcServletRequestMatcher() {
			this(new HashSet<>());
		}

		private GrpcServletRequestMatcher(Set<Class<?>> exclusions) {
			super(ApplicationContext.class);
			this.exclusions = exclusions;
		}

		/**
		 * Return a new {@link GrpcServletRequestMatcher} based on this one but excluding
		 * the specified services.
		 * @param rest additional services to exclude
		 * @return a new {@link GrpcServletRequestMatcher}
		 */
		public GrpcServletRequestMatcher excluding(Class<?>... rest) {
			return excluding(Set.of(rest));
		}

		/**
		 * Return a new {@link GrpcServletRequestMatcher} based on this one but excluding
		 * the specified services.
		 * @param exclusions additional services to exclude
		 * @return a new {@link GrpcServletRequestMatcher}
		 */
		public GrpcServletRequestMatcher excluding(Set<Class<?>> exclusions) {
			Assert.notNull(exclusions, "Exclusions must not be null");
			Set<Class<?>> subset = new LinkedHashSet<>(this.exclusions);
			subset.addAll(exclusions);
			return new GrpcServletRequestMatcher(subset);
		}

		@Override
		protected void initialized(Supplier<ApplicationContext> context) {
			List<RequestMatcher> matchers = getDelegateMatchers(context.get()).toList();
			this.delegate = matchers.isEmpty() ? request -> false : new OrRequestMatcher(matchers);
		}

		private Stream<RequestMatcher> getDelegateMatchers(ApplicationContext context) {
			return getPatterns(context).map(AntPathRequestMatcher::new);
		}

		private Stream<String> getPatterns(ApplicationContext context) {
			return context.getBeanProvider(BindableService.class)
				.stream()
				.filter(service -> !this.exclusions.stream()
					.anyMatch(type -> type.isAssignableFrom(service.getClass())))
				.map(BindableService::bindService)
				.map(service -> "/" + service.getServiceDescriptor().getName() + "/**");
		}

		@Override
		protected boolean matches(HttpServletRequest request, Supplier<ApplicationContext> context) {
			return this.delegate.matches(request);
		}

	}

}
