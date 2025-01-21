package org.springframework.grpc.server.security;

import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

public record CallContext(Metadata headers, Attributes attributes, MethodDescriptor<?, ?> method) {
}