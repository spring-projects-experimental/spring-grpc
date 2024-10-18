package org.springframework.grpc.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GrpcUtilsTests {

	@Test
	void testGetPortFromAddress() {
		assertEquals(8080, GrpcUtils.getPort("localhost:8080"));
	}

	@Test
	void testGetNoPort() {
		assertEquals(9090, GrpcUtils.getPort("localhost"));
	}

	@Test
	void testGetPortFromAddressWithPath() {
		String address = "example.com:1234/path";
		assertEquals(1234, GrpcUtils.getPort(address));
	}

	@Test
	void testGetDomainAddress() {
		String address = "unix:/some/file/somewhere";
		assertEquals(-1, GrpcUtils.getPort(address));
	}

	@Test
	void testGetStaticSchema() {
		String address = "static://localhost";
		assertEquals(9090, GrpcUtils.getPort(address));
	}

	@Test
	void testGetInvalidAddress() {
		String address = "invalid:broken";
		assertEquals(9090, GrpcUtils.getPort(address)); // -1?
	}

}