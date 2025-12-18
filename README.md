[![Maven Package](https://github.com/a5423/jackson3-datatype-problem/actions/workflows/maven-publish.yml/badge.svg)](https://github.com/a5423/jackson3-datatype-problem/actions/workflows/maven-publish.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![codecov](https://codecov.io/github/a5423/jackson3-datatype-problem/graph/badge.svg?token=BQJ6RICWNF)](https://codecov.io/github/a5423/jackson3-datatype-problem)

# jackson3-datatype-problem

> Fork of [jackson-datatype-problem](https://github.com/zalando/problem) by Willi Schönborn

A Jackson 3 module built on top of `org.zalando:jackson-datatype-problem:0.28.0-SNAPSHOT`, providing
`application/problem+json` support for Jackson 3.x.

This project adds a `problem-datatype` implementation for Jackson 3 with minimal interference in existing codebases.

## Features

- JSON serialization and deserialization of `application/problem+json` (RFC 7807)
- Full compatibility with Jackson 3.x, including immutable `ObjectMapper`
- Optional stack trace serialization for debugging
- Support for nested problems via `cause`
- SPI-based auto-discovery through `ServiceLoader`

> ⚠️ Version 1.0.0 does **not** use the Java Module System (JPMS).

## Dependencies

- Java 17+
- Jackson 3.x
- Build tool that supports Maven Central (Maven, Gradle, etc.)

## Installation

Add the dependency to your `pom.xml`:

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

### Handling problems

Reading problems is very specific to the JSON parser in use. This section assumes you're using Jackson, in which case
reading/parsing problems usually boils down to this:

```java
Problem problem = mapper.readValue(json, Problem.class);
```

Jackson is now able to deserialize specific problems into their respective types. By default, e.g. if a type is not
associated with a class, it will fallback to a `DefaultProblem`.

If you used the `Exceptional` interface rather than `ThrowableProblem` you have to adjust your code a little bit:

```java
try{
    throw mapper.readValue(json, Exceptional.class).propagate();
} catch (OutOfStockProblem e) {
    // some code
}
```

Important aspect of exceptions are stack traces, but since they leak implementation details to the outside
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

There is currently, by design, no way to deserialize stack trace from JSON.
Nevertheless, the runtime will fill in the stack trace when the problem instance is created. That stack trace is usually
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

By default no stack trace processing takes place.
