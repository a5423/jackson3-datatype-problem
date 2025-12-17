/**
 * MIT License
 * Copyright (c) 2015-2025 Willi Schönborn <w.schoenborn@gmail.com>
 * Copyright (c) 2025 a5423
 */
package io.github.a5423.problem.jackson;

import org.junit.jupiter.api.Test;
import org.zalando.problem.*;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.util.Objects;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.zalando.problem.Status.BAD_REQUEST;

final class ProblemMixInTest {

    private final ObjectMapper mapper = JsonMapper.builder()
            .registerSubtypes(InsufficientFundsProblem.class)
            .registerSubtypes(OutOfStockException.class)
            .addModule(new ProblemModule())
            .build();

    @Test
    void shouldSerializeDefaultProblem() throws JacksonException {
        final Problem problem = Problem.valueOf(Status.NOT_FOUND);
        final String json = mapper.writeValueAsString(problem);

        assertThatJson(json)
                .isObject()
                .hasSize(2)
                .containsEntry("title", "Not Found")
                .containsEntry("status", 404);
    }

    @Test
    void shouldSerializeCustomProperties() throws JacksonException {
        final Problem problem = Problem.builder()
                .withType(URI.create("https://example.org/out-of-stock"))
                .withTitle("Out of Stock")
                .withStatus(BAD_REQUEST)
                .withDetail("Item B00027Y5QG is no longer available")
                .with("product", "B00027Y5QG")
                .build();

        final String json = mapper.writeValueAsString(problem);

        assertThatJson(json)
                .isObject()
                .hasSize(5)
                .containsEntry("product", "B00027Y5QG");
    }

    @Test
    void shouldSerializeProblemCause() throws JacksonException {
        final Problem problem = Problem.builder()
                .withType(URI.create("https://example.org/preauthorization-failed"))
                .withTitle("Preauthorization Failed")
                .withStatus(BAD_REQUEST)
                .withCause(Problem.builder()
                        .withType(URI.create("https://example.org/expired-credit-card"))
                        .withTitle("Expired Credit Card")
                        .withStatus(BAD_REQUEST)
                        .withDetail("Credit card is expired as of 2015-09-16T00:00:00Z")
                        .with("since", "2015-09-16T00:00:00Z")
                        .build())
                .build();

        final String json = mapper.writeValueAsString(problem);

        assertThatJson(json)
                .node("cause")
                .isObject()
                .containsEntry("type", "https://example.org/expired-credit-card")
                .containsEntry("title", "Expired Credit Card")
                .containsEntry("status", 400)
                .containsEntry("detail", "Credit card is expired as of 2015-09-16T00:00:00Z")
                .containsEntry("since", "2015-09-16T00:00:00Z");
    }

    @Test
    void shouldNotSerializeStacktraceByDefault() throws JacksonException {
        final Problem problem = Problem.builder()
                .withType(URI.create("about:blank"))
                .withTitle("Foo")
                .withStatus(BAD_REQUEST)
                .withCause(Problem.builder()
                        .withType(URI.create("about:blank"))
                        .withTitle("Bar")
                        .withStatus(BAD_REQUEST)
                        .build())
                .build();

        final String json = mapper.writeValueAsString(problem);

        assertThatJson(json)
                .node("stacktrace")
                .isAbsent();
    }

    @Test
    void shouldSerializeStacktrace() throws JacksonException {
        final Problem problem = Problem.builder()
                .withType(URI.create("about:blank"))
                .withTitle("Foo")
                .withStatus(BAD_REQUEST)
                .withCause(Problem.builder()
                        .withType(URI.create("about:blank"))
                        .withTitle("Bar")
                        .withStatus(BAD_REQUEST)
                        .build())
                .build();

        final ObjectMapper mapper = JsonMapper.builder()
                .addModule(new ProblemModule().withStackTraces())
                .build();

        final String json = mapper.writeValueAsString(problem);

        assertThatJson(json)
                .node("stacktrace")
                .isArray()
                .element(0)
                .isString();
    }

    @Test
    void shouldDeserializeDefaultProblem() throws IOException {
        final URL resource = getResource("default.json");
        final Problem problem = mapper.readValue(resource.openStream(), Problem.class);

        assertThat(problem)
                .isInstanceOf(DefaultProblem.class)
                .asInstanceOf(type(DefaultProblem.class))
                .satisfies(p -> {
                    assertThat(p.getType()).hasToString("https://example.org/not-out-of-stock");
                    assertThat(p.getTitle()).isEqualTo("Out of Stock");
                    assertThat(p.getStatus()).isEqualTo(BAD_REQUEST);
                    assertThat(p.getDetail()).isEqualTo("Item B00027Y5QG is no longer available");
                    assertThat(p.getParameters()).containsEntry("product", "B00027Y5QG");
                });
    }

    @Test
    void shouldDeserializeRegisteredExceptional() throws IOException {
        final URL resource = getResource("out-of-stock.json");
        final Exceptional problem = mapper.readValue(resource.openStream(), Exceptional.class);

        assertThat(problem)
                .isInstanceOf(OutOfStockException.class)
                .asInstanceOf(type(OutOfStockException.class))
                .satisfies(p -> {
                    assertThat(p.getType()).hasToString("https://example.org/out-of-stock");
                    assertThat(p.getTitle()).isEqualTo("Out of Stock");
                    assertThat(p.getStatus()).isEqualTo(BAD_REQUEST);
                    assertThat(p.getDetail()).isEqualTo("Item B00027Y5QG is no longer available");
                });
    }

    @Test
    void shouldDeserializeUnregisteredExceptional() throws IOException {
        final URL resource = getResource("out-of-stock.json");
        final IOProblem problem = mapper.readValue(resource.openStream(), IOProblem.class);

        assertThat(problem)
                .satisfies(p -> {
                    assertThat(p.getType()).hasToString("https://example.org/out-of-stock");
                    assertThat(p.getTitle()).isEqualTo("Out of Stock");
                    assertThat(p.getStatus()).isEqualTo(BAD_REQUEST);
                    assertThat(p.getDetail()).isEqualTo("Item B00027Y5QG is no longer available");
                });
    }

    @Test
    void shouldDeserializeSpecificProblem() throws IOException {
        final URL resource = getResource("insufficient-funds.json");
        final Problem problem = mapper.readValue(resource.openStream(), Problem.class);

        assertThat(problem)
                .isInstanceOf(InsufficientFundsProblem.class)
                .asInstanceOf(type(InsufficientFundsProblem.class))
                .satisfies(p -> {
                    assertThat(p.getBalance()).isEqualTo(10);
                    assertThat(p.getDebit()).isEqualTo(-20);
                });
    }

    @Test
    void shouldDeserializeUnknownStatus() throws IOException {
        final URL resource = getResource("unknown.json");
        final Problem problem = mapper.readValue(resource.openStream(), Problem.class);

        final StatusType status = problem.getStatus();
        assertThat(status).isNotNull();
        assertThat(status.getStatusCode()).isEqualTo(666);
        assertThat(status.getReasonPhrase()).isEqualTo("Unknown");
    }

    @Test
    void shouldDeserializeUntyped() throws IOException {
        final URL resource = getResource("untyped.json");
        final Problem problem = mapper.readValue(resource.openStream(), Problem.class);

        assertThat(problem)
                .satisfies(p -> {
                    assertThat(p.getType()).hasToString("about:blank");
                    assertThat(p.getTitle()).isEqualTo("Something bad");
                    assertThat(p.getStatus()).isNotNull();
                    assertThat(p.getStatus().getStatusCode()).isEqualTo(400);
                    assertThat(p.getDetail()).isNull();
                    assertThat(p.getInstance()).isNull();
                });
    }

    @Test
    void shouldDeserializeEmpty() throws IOException {
        final URL resource = getResource("empty.json");
        final Problem problem = mapper.readValue(resource.openStream(), Problem.class);

        assertThat(problem.getType()).hasToString("about:blank");
        assertThat(problem.getTitle()).isNull();
        assertThat(problem.getStatus()).isNull();
        assertThat(problem.getDetail()).isNull();
        assertThat(problem.getInstance()).isNull();
    }

    @Test
    void shouldDeserializeCause() throws IOException {
        final URL resource = getResource("cause.json");
        final ThrowableProblem problem = mapper.readValue(resource.openStream(), ThrowableProblem.class);

        assertThat(problem.getCause())
                .as("Check the 'cause' of the problem")
                .isNotNull()
                .isInstanceOf(DefaultProblem.class)
                .asInstanceOf(type(DefaultProblem.class))
                .satisfies(cause -> {
                    assertThat(cause.getType()).hasToString("https://example.org/expired-credit-card");
                    assertThat(cause.getTitle()).isEqualTo("Expired Credit Card");
                    assertThat(cause.getStatus()).isEqualTo(BAD_REQUEST);
                    assertThat(cause.getDetail()).isEqualTo("Credit card is expired as of 2015-09-16T00:00:00Z");
                    assertThat(cause.getParameters()).containsEntry("since", "2015-09-16T00:00:00Z");
                });
    }

    @Test
    void shouldDeserializeWithProcessedStackTrace() throws IOException {
        final URL resource = getResource("cause.json");
        final ThrowableProblem problem = mapper.readValue(resource.openStream(), ThrowableProblem.class);

        final String stackTrace = getStackTrace(problem);
        final String[] stackTraceElements = stackTrace.split("\n");

        assertThat(stackTraceElements[1])
                .startsWith("\tat " + ProblemMixInTest.class.getName());
    }

    private String getStackTrace(final Throwable throwable) {
        final StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private static URL getResource(final String name) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return Objects.requireNonNull(loader.getResource(name), () -> "resource " + name + " not found.");
    }

}
