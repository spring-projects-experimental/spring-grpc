# Spring gRPC [![build status](https://github.com/spring-projects-experimental/spring-grpc/actions/workflows/deploy.yml/badge.svg)](https://github.com/spring-projects/spring-grpc/actions/workflows/deploy.yml)

Welcome to the Spring gRPC project!

The Spring gRPC project provides a Spring-friendly API and abstractions for developing gRPC applications. There is a core library that makes it easy to work with gRPC and dependency injection, and a Spring Boot starter that makes it easy to get started with gRPC in a Spring Boot application (with autoconfiguration and configuration properties, for instance).

For further information go to our [Spring gRPC reference documentation](https://docs.spring.io/spring-grpc/reference/).

## Quick Start

There is a simple sample project in the `samples` directory (e.g. [`grpc-server`](https://github.com/spring-projects-experimental/spring-grpc/tree/main/samples/grpc-server)). You can run it with `mvn spring-boot:run` or `gradle bootRun`. You will see the following code in that sample.

To create a simple gRPC server, you can use the Spring Boot starter. For Maven:

```xml
<dependency>
	<groupId>org.springframework.grpc</groupId>
	<artifactId>spring-grpc-spring-boot-starter</artifactId>
	<version>0.1.0-SNAPSHOT</version>
</dependency>
```

or for Gradle:

```groovy
implementation 'org.springframework.grpc:spring-grpc-spring-boot-starter:0.1.0-SNAPSHOT'
```

For convenience, you can use the Spring gRPC BOM to manage dependencies. With Maven:

```xml
<dependencyManagement>
	<dependencies>
		<dependency>
			<groupId>org.springframework.grpc</groupId>
			<artifactId>spring-grpc-dependencies</artifactId>
			<version>0.1.0-SNAPSHOT</version>
			<type>pom</type>
			<scope>import</scope>
		</dependency>
	</dependencies>
</dependencyManagement>
```

or Gradle:

```groovy
dependencyManagement {
	imports {
		mavenBom 'org.springframework.grpc:spring-grpc-dependencies:0.1.0-SNAPSHOT'
	}
}
```

Then you can omit the version from the dependencies.

You need a Protobuf file that defines your service and messages, and you will need to configure your build tools to compile it into Java sources. This is a standard part of gRPC development (i.e. nothing to do with Spring). We now come to the Spring gRPC features.

### gPRC Server

Create a `@Bean` of type `BindableService`. For example:

```java
@Service
public class GrpcServerService extends SimpleGrpc.SimpleImplBase {
...
}
```

(`BindableService` is the interface that gRPC uses to bind services to the server and `SimpleImplBase` was created for you from your Protobuf file.)

Then, you can just run your application and the gRPC server will be started on the default port (9090). Here's a simple example (standard Spring Boot application):

```java
@SpringBootApplication
public class GrpcServerApplication {
	public static void main(String[] args) {
		SpringApplication.run(GrpcServerApplication.class, args);
	}
}
```

Run it from your IDE, or on the command line with `mvn spring-boot:run` or `gradle bootRun`.

### gRPC Client

To create a simple gRPC client, you can use the Spring Boot starter (see above - it's the same as for the server). Then you can inject a bean of type `GrpcChannelFactory` and use it to create a gRPC channel. The most common usage of a channel is to create a client that binds to a service, such as the one above. The Protobuf-generated sources in your project will contain the stub classes, and they just need to be bound to a channel. For example, to bind to the `SimpleGrpc` service on a local server:

```java
@Bean
SimpleGrpc.SimpleBlockingStub stub(GrpcChannelFactory channels) {
	return SimpleGrpc.newBlockingStub(channels.createChannel("0.0.0.0:9090").build());
}
```

Then you can inject the stub and use it in your application.

The default `GrpcChannelFactory` implementation can also create a "named" channel, which you can then use to extract the configuration to connect to the server. For example:

```java
@Bean
SimpleGrpc.SimpleBlockingStub stub(GrpcChannelFactory channels) {
	return SimpleGrpc.newBlockingStub(channels.createChannel("local").build());
}
```

then in `application.properties`:

```properties
spring.grpc.client.channels.local.address=0.0.0.0:9090
```

There is a default named channel (named "default") that you can configure in the same way, and then it will be used by default if there is no channel with the name specified in the channel creation.

## Building

To build with unit tests

```shell
./mvnw clean package
```

```
To build the docs
```shell
./mvnw -pl spring-grpc-docs antora
```

The docs are then in the directory `spring-grpc-docs/target/antora/site/index.html`

To reformat using the [java-format plugin](https://github.com/spring-io/spring-javaformat)
```shell
./mvnw spring-javaformat:apply
```
