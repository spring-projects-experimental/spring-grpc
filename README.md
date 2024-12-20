# Spring gRPC
!["Build Status"](https://github.com/spring-projects-experimental/spring-grpc/actions/workflows/deploy.yml/badge.svg)

Welcome to the Spring gRPC project!

The Spring gRPC project provides a Spring-friendly API and abstractions for developing gRPC applications. There is a core library that makes it easy to work with gRPC and dependency injection, and a Spring Boot starter that makes it easy to get started with gRPC in a Spring Boot application (with autoconfiguration and configuration properties, for instance).

For further information go to our [Spring gRPC reference documentation](https://docs.spring.io/spring-grpc/reference/).

# Getting Started

This section offers jumping off points for how to get started using Spring gRPC. There is a simple sample project in the `samples` directory (e.g. [`grpc-server`](https://github.com/spring-projects-experimental/spring-grpc/tree/main/samples/grpc-server)). You can run it with `mvn spring-boot:run` or `gradle bootRun`. You will see the following code in that sample.

## TL;DR 

Want to get started? Let's speedrun a working service. Go to the [Spring Initializr](https://start.spring.io), select `gRPC`, `Web`, and `GraalVM`. I'll be using Apache Maven in this tutorial but Gradle works, too.

If you want to use GraalVM native images, make sure you have the GraalVM distribution of OpenJDK installed. You can do so with [sdkman.io](https://sdkman.io). e.g.: `sdk install java 23.0.1-graalce  && sdk default java 23.0.1-graalce`.

Download the `.zip` file and unzip it. Open it in your IDE in the usual way. If you're using IntelliJ IDEA: `idea pom.xml`. 

Define a `.proto` service definition:

```proto
syntax = "proto3";

option java_multiple_files = true;
option java_package = "org.springframework.grpc.sample.proto";
option java_outer_classname = "HelloWorldProto";

// The greeting service definition.
service Simple {
  // Sends a greeting
  rpc SayHello (HelloRequest) returns (HelloReply) {
  }
  rpc StreamHello(HelloRequest) returns (stream HelloReply) {}
}

// The request message containing the user's name.
message HelloRequest {
  string name = 1;
}

// The response message containing the greetings
message HelloReply {
  string message = 1;
}
```

We'll want to define the stubs for a Java service based on this definition:

```shell
./mvnw clean package
```

You'll get two new folders in the `target` directory: `target/target/generated-sources/protobuf/grpc-java`  and `target/target/generated-sources/protobuf/java`. You may need to instruct your IDE to mark them as  source roots. In IntelliJ IDEA, you'd right click the folder, choose `Mark Directory As` -> `Generated Source Root`. 

Now you can implement a service based on the generated stubs:

```java

@Service
class GrpcServerService extends SimpleGrpc.SimpleImplBase {

    private static Log log = LogFactory.getLog(GrpcServerService.class);

    @Override
    public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
        log.info("Hello " + req.getName());
        if (req.getName().startsWith("error")) {
            throw new IllegalArgumentException("Bad name: " + req.getName());
        }
        if (req.getName().startsWith("internal")) {
            throw new RuntimeException();
        }
        HelloReply reply = HelloReply.newBuilder().setMessage("Hello ==> " + req.getName()).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void streamHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
        log.info("Hello " + req.getName());
        int count = 0;
        while (count < 10) {
            HelloReply reply = HelloReply.newBuilder().setMessage("Hello(" + count + ") ==> " + req.getName()).build();
            responseObserver.onNext(reply);
            count++;
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                responseObserver.onError(e);
                return;
            }
        }
        responseObserver.onCompleted();
    }
}
```

Run the program in the usual way: 

```shell
./mvnw spring-boot:run
```

You can try it out using a gRPC client like `grpcurl`:

```
grpcurl -d '{"name":"Hi"}' -plaintext localhost:9090 Simple.SayHello
```

You should get a response like: 

```shell 
{
  "message": "Hello ==\u003e Hi"
}
```

And you went through all that work to get GraalVM native image, so let';s set that up, too:

`./mvnw -DskipTests -Pnative native:compile`

You can then run it from `target/demo`. On my machine:

```
2024-12-20T02:43:13.656-08:00  INFO 60176 --- [demo] [           main] com.example.demo.DemoApplication         : Started DemoApplication in 0.027 seconds (process running for 0.032)

``` 

Congrats! You've got yourslef a lightweight, very quick network service built with Spring gRPC!

Want some more details on what's happening? Read on!

## Details

You should follow the steps in each of the following section according to your needs.

**📌 NOTE**\
Spring gRPC supports Spring Boot 3.4.x

### Add Milestone and Snapshot Repositories

If you prefer to add the dependency snippets by hand, follow the directions in the following sections.

To use the Milestone and Snapshot version, you need to add references to the Spring Milestone and/or Snapshot repositories in your build file.

For Maven, add the following repository definitions as needed (if you are using snapshots or milestones):

```xml
  <repositories>
    <repository>
      <id>spring-milestones</id>
      <name>Spring Milestones</name>
      <url>https://repo.spring.io/milestone</url>
      <snapshots>
        <enabled>false</enabled>
      </snapshots>
    </repository>
    <repository>
      <id>spring-snapshots</id>
      <name>Spring Snapshots</name>
      <url>https://repo.spring.io/snapshot</url>
      <releases>
        <enabled>false</enabled>
      </releases>
    </repository>
  </repositories>
```

For Gradle, add the following repository definitions as needed:

```groovy
repositories {
  mavenCentral()
  maven { url 'https://repo.spring.io/milestone' }
  maven { url 'https://repo.spring.io/snapshot' }
}
```

### Dependency Management

The Spring gRPC Dependencies declares the recommended versions of all the dependencies used by a given release of Spring gRPC.
Using the dependencies from your application’s build script avoids the need for you to specify and maintain the dependency versions yourself.
Instead, the version you’re using determines the utilized dependency versions.
It also ensures that you’re using supported and tested versions of the dependencies by default, unless you choose to override them.

If you’re a Maven user, you can use the dependencies by adding the following to your pom.xml file -

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.grpc</groupId>
            <artifactId>spring-grpc-dependencies</artifactId>
            <version>0.3.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Gradle users can also use the Spring gRPC Dependencies by leveraging Gradle (5.0+) native support for declaring dependency constraints using a Maven BOM.
This is implemented by adding a 'platform' dependency handler method to the dependencies section of your Gradle build script.
As shown in the snippet below this can then be followed by version-less declarations of the Starter Dependencies for the one or more spring-grpc modules you wish to use, e.g. spring-grpc-openai.

```gradle
dependencies {
  implementation platform("org.springframework.grpc:spring-grpc-dependencies:0.3.0-SNAPSHOT")
}
```

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

Then, you can just run your application and the gRPC server will be started on the default port (9090). Here’s a simple example (standard Spring Boot application):

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

To create a simple gRPC client, you can use the Spring Boot starter (see above - it’s the same as for the server). Then you can inject a bean of type `GrpcChannelFactory` and use it to create a gRPC channel. The most common usage of a channel is to create a client that binds to a service, such as the one above. The Protobuf-generated sources in your project will contain the stub classes, and they just need to be bound to a channel. For example, to bind to the `SimpleGrpc` service on a local server:

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
