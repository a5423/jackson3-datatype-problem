[![Maven Package](https://github.com/a5423/jackson3-datatype-problem/actions/workflows/maven-publish.yml/badge.svg)](https://github.com/a5423/jackson3-datatype-problem/actions/workflows/maven-publish.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![codecov](https://codecov.io/github/a5423/jackson3-datatype-problem/graph/badge.svg?token=BQJ6RICWNF)](https://codecov.io/github/a5423/jackson3-datatype-problem)

# jackson3-datatype-problem

> Fork of [jackson-datatype-problem](https://github.com/zalando/problem) by Willi Schönborn

Jackson 3 datatype problem built on top of org.zalando:jackson-datatype-problem:0.28.0-SNAPSHOT

The project aims to add a `problem-data-type` implementation for Jackson3, avoiding serious interference with previously
developed code.

## Features

- tools for json serialization and deserialization of `application/problem+json`

The jackson3-datatype-problem library does not alter the functionality of the original org.zalando:
jackson-datatype-problem library. However, version 1.0.0 does not use the Java module system.

## Dependencies

- Java 17
- Any build tool using Maven Central, or direct download
- Jackson 3.x

## Installation

Add the following dependency to your project:

```xml

<dependency>
    <groupId>io.github.a5423</groupId>
    <artifactId>jackson3-datatype-problem</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Configuration

Please note that ObjectMapper in Jackson 3 has become immutable:

```java
JsonMapper mapper = JsonMapper.builder()
        .addModule(new ProblemModule())
        .build();
```

Alternatively, you can use the SPI capabilities:

```java
JsonMapper mapper = JsonMapper.builder()
        .findAndAddModules()
        .build();
```

## Usage

### Handling problems

Reading problems is very specific to the JSON parser in use. This section assumes you're using Jackson, in which case
reading/parsing problems usually boils down to this:

```java
Problem problem = mapper.readValue(..,Problem .class);
```

Jackson is now able to deserialize specific problems into their respective types. By default, e.g. if a type is not
associated with a class, it will fallback to a `DefaultProblem`.

If you used the `Exceptional` interface rather than `ThrowableProblem` you have to adjust your code a little bit:

```java
try{
        throw mapper.readValue(..,Exceptional .class).

propagate();
}catch(
OutOfStockProblem e){
        ...
```

### Stack traces and causal chains

Exceptions in Java can be chained/nested using *causes*. `ThrowableProblem` adapts the pattern seamlessly to problems:

```java
ThrowableProblem problem = Problem.builder()
        .withType(URI.create("https://example.org/order-failed"))
        .withTitle("Order failed")
        .withStatus(BAD_REQUEST)
        .withCause(Problem.builder()
                .withType(URI.create("https://example.org/out-of-stock"))
                .withTitle("Out of Stock")
                .withStatus(BAD_REQUEST)
                .build())
        .build();
    
problem.

getCause(); // standard API of java.lang.Throwable
```

Will produce this:

```json
{
  "type": "https://example.org/order-failed",
  "title": "Order failed",
  "status": 400,
  "cause": {
    "type": "https://example.org/out-of-stock",
    "title": "Out of Stock",
    "status": 400,
    "detail": "Item B00027Y5QG is no longer available"
  }
}
```

Another important aspect of exceptions are stack traces, but since they leak implementation details to the outside
world, **we strongly advise against exposing them** in problems. That being said, there is a legitimate use case when
you're debugging an issue on an integration environment and you don't have direct access to the log files. Serialization
of stack traces can be enabled on the problem module:

```java
JsonMapper mapper = JsonMapper.builder()
        .addModule(new ProblemModule().withStackTraces())
        .build();
```

After enabling stack traces all problems will contain a `stacktrace` property:

```json
{
  "type": "about:blank",
  "title": "Unprocessable Entity",
  "status": 400,
  "stacktrace": [
    "org.example.Example.execute(Example.java:17)",
    "org.example.Example.main(Example.java:11)"
  ]
}
```

Since we discourage the serialization of them, there is currently, by design, no way to deserialize them from JSON.
Nevertheless the runtime will fill in the stack trace when the problem instance is created. That stack trace is usually
not 100% correct, since it looks like the exception originated inside your deserialization framework. *Problem* comes
with a special service provider interface `StackTraceProcessor` that can be registered using the
[
`ServiceLoader` capabilities](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/ServiceLoader.html).
It can be used
to modify the stack trace, e.g. remove all lines before your own client code, e.g. Jackson/HTTP client/etc.

```java
public interface StackTraceProcessor {

    Collection<StackTraceElement> process(final Collection<StackTraceElement> elements);

}
```

By default no processing takes place.
