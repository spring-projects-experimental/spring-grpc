# Spring gRPC Docs

## README

The top level README is generated from sources in this module.

```
$ cd spring-grpc-docs
$ asciidoctor-reducer src/main/antora/modules/ROOT/pages/README.adoc | downdoc - > ../README.md
```

where [`asciidoctor-reducer`](https://github.com/asciidoctor/asciidoctor-reducer) is a gem (so probably in `~/.gem/ruby/<version>/bin/asciidoctor-reducer`) and [`downdoc`](https://github.com/opendevise/downdoc) is an `npm` module, so `./node_modules/.bin/downdoc`.

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
