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
package org.springframework.grpc.server.security;

import org.springframework.core.Ordered;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/**
 * An interceptor that extracts the authentication credentials from the gRPC request
 * headers and metadata, authenticates the user, and sets the authentication in the
 * SecurityContext. This interceptor should be registered with the gRPC server to handle
 * authentication.
 *
 * @author Dave Syer
 */
public class AuthenticationServerInterceptor implements ServerInterceptor, Ordered {

	private final AuthenticationManager authenticationManager;

	private final GrpcAuthenticationExtractor extractor;

	private AuthorizationManager<CallContext> authorizationManager;

	@Override
	public int getOrder() {
		return GrpcSecurity.CONTEXT_FILTER_ORDER - 10;
	}

	public AuthenticationServerInterceptor(AuthenticationManager authenticationManager,
			GrpcAuthenticationExtractor extractor, AuthorizationManager<CallContext> authorizationManager) {
		this.authenticationManager = authenticationManager;
		this.extractor = extractor;
		this.authorizationManager = authorizationManager;
	}

	@Override
	public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
			ServerCallHandler<ReqT, RespT> next) {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		Authentication user = this.extractor.extract(headers, call.getAttributes());
		if (user != null) {
			user = this.authenticationManager.authenticate(user);
			securityContext.setAuthentication(user);
		}

		if (this.authorizationManager != null) {
			if (user == null) {
				user = new AnonymousAuthenticationToken("anonymous", "anonymous",
						AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
			}
			return new AuthenticatedListener<ReqT>(next.startCall(call, headers), this.authorizationManager,
					new CallContext(headers, call.getAttributes(), call.getMethodDescriptor()), user);
		}
		return new AuthenticatedListener<ReqT>(next.startCall(call, headers), null,
				new CallContext(headers, call.getAttributes(), call.getMethodDescriptor()), user);
	}

	static class AuthenticatedListener<ReqT> extends ForwardingServerCallListener<ReqT> {

		private final Listener<ReqT> delegate;

		private final CallContext context;

		private final Authentication authentication;

		private final AuthorizationManager<CallContext> authorizationManager;

		AuthenticatedListener(io.grpc.ServerCall.Listener<ReqT> delegate,
				AuthorizationManager<CallContext> authorizationManager, CallContext context, Authentication user) {
			this.delegate = delegate;
			this.authorizationManager = authorizationManager;
			this.context = context;
			this.authentication = user;
		}

		@Override
		public void onReady() {
			if (this.authentication == null || !this.authentication.isAuthenticated()
					|| this.authentication instanceof AnonymousAuthenticationToken) {
				throw new BadCredentialsException("not authenticated");
			}
			else {
				if (!this.authorizationManager.authorize(() -> this.authentication, this.context).isGranted()) {
					throw new AccessDeniedException("not allowed");
				}
			}
			super.onReady();
		}

		@Override
		protected Listener<ReqT> delegate() {
			return this.delegate;
		}

	}

}
