# Spring gRPC Docs

## Configuration Properties
The Spring gRPC configuration properties are automatically documented as follows:

1. This module contains a Java class (`org.springframework.grpc.internal.ConfigurationPropertiesAsciidocGenerator`) that is compiled when the module is built.
1. This class is then used during the Maven `package` phase to generate an asciidoc page containing each of the configuration properties.
1. The asciidoc is then included in the Antora reference documentation.

## Antora Site

To build the Antora site locally run the following command from the project root directory:
```
./mvnw -pl spring-grpc-docs antora
```
You can then view the output by opening `spring-grpc-docs/target/antora/site/index.html`. 
