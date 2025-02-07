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
package org.springframework.grpc.server.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

/**
 * A gRPC {@link ServerInterceptor} that handles exceptions thrown during the processing
 * of gRPC calls. It intercepts the call and wraps the {@link ServerCall.Listener} with an
 * {@link ExceptionHandlerListener} that catches exceptions in {@code onMessage} and
 * {@code onHalfClose} methods, and delegates the exception handling to the provided
 * {@link GrpcExceptionHandler}.
 *
 * <p>
 * A fallback mechanism is used to return UNONOWN in case the {@link GrpcExceptionHandler}
 * returns a null.
 *
 * @author Dave Syer
 * @see ServerInterceptor
 * @see GrpcExceptionHandler
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GrpcExceptionHandlerInterceptor implements ServerInterceptor {

	private final GrpcExceptionHandler exceptionHandler;

	public GrpcExceptionHandlerInterceptor(GrpcExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * Intercepts a gRPC server call to handle exceptions.
	 * @param <ReqT> the type of the request message
	 * @param <RespT> the type of the response message
	 * @param call the server call object
	 * @param headers the metadata headers for the call
	 * @param next the next server call handler in the interceptor chain
	 * @return a listener for the request messages
	 */
	@Override
	public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
			ServerCallHandler<ReqT, RespT> next) {
		Listener<ReqT> listener;
		try {
			listener = next.startCall(call, headers);
		}
		catch (Throwable t) {
			call.close(this.exceptionHandler.handleException(t), headers(t));
			listener = new Listener<ReqT>() {
			};
			return listener;
		}
		return new ExceptionHandlerListener<>(listener, call, new FallbackHandler(this.exceptionHandler));
	}

	private static Metadata headers(Throwable t) {
		Metadata result = Status.trailersFromThrowable(t);
		return result != null ? result : new Metadata();
	}

	static class ExceptionHandlerListener<ReqT, RespT> extends SimpleForwardingServerCallListener<ReqT> {

		private ServerCall<ReqT, RespT> call;

		private GrpcExceptionHandler exceptionHandler;

		ExceptionHandlerListener(ServerCall.Listener<ReqT> delegate, ServerCall<ReqT, RespT> call,
				GrpcExceptionHandler exceptionHandler) {
			super(delegate);
			this.call = call;
			this.exceptionHandler = exceptionHandler;
		}

		@Override
		public void onReady() {
			try {
				super.onReady();
			}
			catch (Throwable t) {
				handle(t);
			}
		}

		@Override
		public void onMessage(ReqT message) {
			try {
				super.onMessage(message);
			}
			catch (Throwable t) {
				handle(t);
			}
		}

		@Override
		public void onHalfClose() {
			try {
				super.onHalfClose();
			}
			catch (Throwable t) {
				handle(t);
			}
		}

		private void handle(Throwable t) {
			if (t instanceof IllegalStateException && t.getMessage().equals("call is closed")) {
				// gRPC server thinks the call is already closed. It must be a race
				// condition because you don't see it if you debug and set a breakpoint
				// here.
				return;
			}
			else {
				Status status = Status.fromThrowable(t);
				try {
					status = this.exceptionHandler.handleException(t);
				}
				catch (Throwable e) {
				}
				try {
					this.call.close(status, headers(t));
				}
				catch (Throwable e) {
					throw new IllegalStateException("Failed to close the call", e);
				}
			}
		}

	}

	static class FallbackHandler implements GrpcExceptionHandler {

		private final GrpcExceptionHandler exceptionHandler;

		FallbackHandler(GrpcExceptionHandler exceptionHandler) {
			this.exceptionHandler = exceptionHandler;
		}

		@Override
		public Status handleException(Throwable exception) {
			Status status = this.exceptionHandler.handleException(exception);
			return status != null ? status : Status.fromThrowable(exception);
		}

	}

}
