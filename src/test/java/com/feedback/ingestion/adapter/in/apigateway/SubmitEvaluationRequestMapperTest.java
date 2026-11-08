package com.feedback.ingestion.adapter.in.apigateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.feedback.ingestion.application.usecase.SubmitEvaluationCommand;
import com.feedback.ingestion.fixtures.TestEvents;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SubmitEvaluationRequestMapperTest {

    private static final String SUB = "11111111-2222-3333-4444-555555555555";

    private final SubmitEvaluationRequestMapper mapper = new SubmitEvaluationRequestMapper();

    @Test
    void should_buildCommand_when_eventIsValid() {
        SubmitEvaluationCommand command = mapper.toCommand(TestEvents.apiGateway("post-evaluation-valid.json"));

        assertThat(command).isEqualTo(new SubmitEvaluationCommand(SUB, "Great class", 7));
    }

    @Test
    void should_decodeBody_when_base64Encoded() {
        APIGatewayV2HTTPEvent event = validEventWithBody(Base64.getEncoder().encodeToString(
                "{\"description\":\"Great class\",\"score\":2}".getBytes(StandardCharsets.UTF_8)));
        event.setIsBase64Encoded(true);

        assertThat(mapper.toCommand(event).score()).isEqualTo(2);
    }

    @Test
    void should_keepOutOfRangeScore_when_itIsAnInteger() {
        // the range is a business rule, checked by the domain, not by the mapper
        assertThat(mapper.toCommand(validEventWithBody("{\"description\":\"x\",\"score\":11}")).score()).isEqualTo(11);
    }

    @Test
    void should_throwUnauthenticated_when_subIsMissing() {
        APIGatewayV2HTTPEvent event = TestEvents.apiGateway("post-evaluation-without-sub.json");

        assertThatThrownBy(() -> mapper.toCommand(event)).isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void should_throwUnauthenticated_when_requestContextIsMissing() {
        APIGatewayV2HTTPEvent event = validEventWithBody("{\"description\":\"x\",\"score\":5}");
        event.setRequestContext(null);

        assertThatThrownBy(() -> mapper.toCommand(event)).isInstanceOf(UnauthenticatedException.class);
    }

    @ParameterizedTest(name = "score in {0}")
    @ValueSource(strings = {
        "{\"description\":\"x\",\"score\":\"7\"}",
        "{\"description\":\"x\",\"score\":true}",
        "{\"description\":\"x\",\"score\":7.5}",
        "{\"description\":\"x\",\"score\":7.0}",
        "{\"description\":\"x\",\"score\":99999999999}",
        "{\"description\":\"x\",\"score\":null}",
        "{\"description\":\"x\"}"
    })
    void should_rejectScore_when_itIsNotAJsonInteger(String body) {
        assertThatThrownBy(() -> mapper.toCommand(validEventWithBody(body)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("score is required and must be an integer");
    }

    @ParameterizedTest(name = "description in {0}")
    @ValueSource(strings = {"{\"description\":42,\"score\":5}", "{\"description\":null,\"score\":5}", "{\"score\":5}"})
    void should_rejectDescription_when_itIsNotAString(String body) {
        assertThatThrownBy(() -> mapper.toCommand(validEventWithBody(body)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("description is required and must be a string");
    }

    @ParameterizedTest(name = "body {0}")
    @ValueSource(strings = {"", "   ", "not json", "[1, 2]", "\"text\"", "{\"description\":"})
    void should_rejectBody_when_itIsNotAJsonObject(String body) {
        assertThatThrownBy(() -> mapper.toCommand(validEventWithBody(body)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void should_rejectBody_when_itIsNull() {
        assertThatThrownBy(() -> mapper.toCommand(validEventWithBody(null)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Request body is required");
    }

    private static APIGatewayV2HTTPEvent validEventWithBody(String body) {
        APIGatewayV2HTTPEvent event = TestEvents.apiGateway("post-evaluation-valid.json");
        event.setBody(body);
        return event;
    }
}
