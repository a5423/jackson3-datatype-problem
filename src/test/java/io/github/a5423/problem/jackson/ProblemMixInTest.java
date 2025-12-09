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
import java.util.List;
import java.util.Objects;

import static com.jayway.jsonassert.JsonAssert.with;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
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

        with(json)
                .assertThat("$.*", hasSize(2))
                .assertThat("$.title", is("Not Found"))
                .assertThat("$.status", is(404));
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

        with(json)
                .assertThat("$.*", hasSize(5))
                .assertThat("$.product", is("B00027Y5QG"));
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

        with(json)
                .assertThat("$.cause.type", is("https://example.org/expired-credit-card"))
                .assertThat("$.cause.title", is("Expired Credit Card"))
                .assertThat("$.cause.status", is(400))
                .assertThat("$.cause.detail", is("Credit card is expired as of 2015-09-16T00:00:00Z"))
                .assertThat("$.cause.since", is("2015-09-16T00:00:00Z"));
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

        with(json)
                .assertNotDefined("$.stacktrace")
                .assertNotDefined("$.stackTrace"); // default name, just in case our renaming didn't apply
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

        with(json)
                .assertThat("$.stacktrace", is(instanceOf(List.class)))
                .assertThat("$.stacktrace[0]", is(instanceOf(String.class)));
    }

    @Test
    void shouldDeserializeDefaultProblem() throws IOException {
        final URL resource = getResource("default.json");
        final Problem problem = mapper.readValue(resource.openStream(), Problem.class);

        assertThat(problem, instanceOf(DefaultProblem.class));
        assertThat(problem, hasProperty("type", hasToString("https://example.org/not-out-of-stock")));
        assertThat(problem, hasProperty("title", equalTo("Out of Stock")));
        assertThat(problem, hasProperty("status", equalTo(BAD_REQUEST)));
        assertThat(problem, hasProperty("detail",  equalTo("Item B00027Y5QG is no longer available")));
        assertThat(problem, hasProperty("parameters",  hasEntry("product", "B00027Y5QG")));
    }

    @Test
    void shouldDeserializeRegisteredExceptional() throws IOException {
        final URL resource = getResource("out-of-stock.json");
        final Exceptional problem = mapper.readValue(resource.openStream(), Exceptional.class);

        assertThat(problem, instanceOf(OutOfStockException.class));
        assertThat(problem, hasProperty("type", hasToString("https://example.org/out-of-stock")));
        assertThat(problem, hasProperty("title", equalTo("Out of Stock")));
        assertThat(problem, hasProperty("status", equalTo(BAD_REQUEST)));
        assertThat(problem, hasProperty("detail",   equalTo("Item B00027Y5QG is no longer available")));
    }

    @Test
    void shouldDeserializeUnregisteredExceptional() throws IOException {
        final URL resource = getResource("out-of-stock.json");
        final IOProblem problem = mapper.readValue(resource.openStream(), IOProblem.class);

        assertThat(problem, hasProperty("type", hasToString("https://example.org/out-of-stock")));
        assertThat(problem, hasProperty("title",  equalTo("Out of Stock")));
        assertThat(problem, hasProperty("status",equalTo(BAD_REQUEST)));
        assertThat(problem, hasProperty("detail",  equalTo("Item B00027Y5QG is no longer available")));
    }

    @Test
    void shouldDeserializeSpecificProblem() throws IOException {
        final URL resource = getResource("insufficient-funds.json");
        final Problem problem = mapper.readValue(resource.openStream(), Problem.class);

        assertThat(problem, instanceOf(InsufficientFundsProblem.class));
        assertThat(problem, hasProperty("balance",equalTo(10)));
        assertThat(problem, hasProperty("debit", equalTo(-20)));
    }

    @Test
    void shouldDeserializeUnknownStatus() throws IOException {
        final URL resource = getResource("unknown.json");
        final Problem problem = mapper.readValue(resource.openStream(), Problem.class);

        final StatusType status = problem.getStatus();
        assertThat(status, is(notNullValue()));
        assertThat(status, hasProperty("statusCode", equalTo(666)));
        assertThat(status, hasProperty("reasonPhrase", equalTo("Unknown")));
    }

    @Test
    void shouldDeserializeUntyped() throws IOException {
        final URL resource = getResource("untyped.json");
        final Problem problem = mapper.readValue(resource.openStream(), Problem.class);

        assertThat(problem,hasProperty("type", hasToString("about:blank")));
        assertThat(problem,hasProperty("title", equalTo("Something bad")));
        assertThat(problem.getStatus(), is(not(nullValue())));
        assertThat(problem.getStatus(), hasProperty("statusCode", equalTo(400)));
        assertThat(problem.getDetail(), is(nullValue()));
        assertThat(problem.getInstance(), is(nullValue()));
    }

    @Test
    void shouldDeserializeEmpty() throws IOException {
        final URL resource = getResource("empty.json");
        final Problem problem = mapper.readValue(resource.openStream(), Problem.class);

        assertThat(problem.getType(), hasToString("about:blank"));
        assertThat(problem.getTitle(), is(nullValue()));
        assertThat(problem.getStatus(), is(nullValue()));
        assertThat(problem.getDetail(), is(nullValue()));
        assertThat(problem.getInstance(), is(nullValue()));
    }

    @Test
    void shouldDeserializeCause() throws IOException {
        final URL resource = getResource("cause.json");
        final ThrowableProblem problem = mapper.readValue(resource.openStream(), ThrowableProblem.class);

        assertThat(problem, hasProperty("cause", is(notNullValue())));
        final ThrowableProblem cause = problem.getCause();

        assertThat(cause, instanceOf(DefaultProblem.class));
        assertThat(cause, is(notNullValue()));
        assertThat(cause, instanceOf(DefaultProblem.class));

        assertThat(cause, hasProperty("type", hasToString("https://example.org/expired-credit-card")));
        assertThat(cause, hasProperty("title", equalTo("Expired Credit Card")));
        assertThat(cause, hasProperty("status",equalTo(BAD_REQUEST)));
        assertThat(cause, hasProperty("detail",  equalTo("Credit card is expired as of 2015-09-16T00:00:00Z")));
        assertThat(cause, hasProperty("parameters", hasEntry("since", "2015-09-16T00:00:00Z")));
    }

    @Test
    void shouldDeserializeWithProcessedStackTrace() throws IOException {
        final URL resource = getResource("cause.json");
        final ThrowableProblem problem = mapper.readValue(resource.openStream(), ThrowableProblem.class);

        final String stackTrace = getStackTrace(problem);
        final String[] stackTraceElements = stackTrace.split("\n");

        assertThat(stackTraceElements[1], startsWith("\tat " + ProblemMixInTest.class.getName()));
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
