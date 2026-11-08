package com.feedback.ingestion.adapter.in.apigateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.feedback.ingestion.application.usecase.SubmitEvaluationCommand;
import com.feedback.ingestion.application.usecase.SubmitEvaluationUseCase;
import com.feedback.ingestion.domain.exception.DomainValidationException;
import com.feedback.ingestion.fixtures.TestEvents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubmitEvaluationControllerTest {

    @Mock
    private SubmitEvaluationUseCase useCase;

    @Mock
    private LambdaLogger logger;

    @Captor
    private ArgumentCaptor<String> logged;

    private SubmitEvaluationController controller;

    @BeforeEach
    void setUp() {
        controller = new SubmitEvaluationController(useCase, new SubmitEvaluationRequestMapper());
    }

    @Test
    void should_return201WithId_when_evaluationIsSaved() {
        // given
        when(useCase.execute(any(SubmitEvaluationCommand.class))).thenReturn("eval-1");

        // when
        APIGatewayV2HTTPResponse response = controller.handle(validEvent(), logger);

        // then
        assertThat(response.getStatusCode()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo("{\"evaluationId\":\"eval-1\"}");
        assertThat(response.getHeaders()).containsEntry("Content-Type", "application/json");
    }

    @Test
    void should_return401_when_userIsMissing() {
        APIGatewayV2HTTPResponse response =
                controller.handle(TestEvents.apiGateway("post-evaluation-without-sub.json"), logger);

        assertThat(response.getStatusCode()).isEqualTo(401);
        verifyNoInteractions(useCase);
    }

    @Test
    void should_return400_when_requestIsMalformed() {
        APIGatewayV2HTTPEvent event = validEvent();
        event.setBody("{\"description\":\"Great class\",\"score\":\"7\"}");

        APIGatewayV2HTTPResponse response = controller.handle(event, logger);

        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.getBody()).contains("score is required and must be an integer");
        verifyNoInteractions(useCase);
    }

    @Test
    void should_return400WithDomainMessage_when_domainRejectsTheEvaluation() {
        when(useCase.execute(any(SubmitEvaluationCommand.class)))
                .thenThrow(new DomainValidationException("score must be an integer from 0 to 10"));

        APIGatewayV2HTTPResponse response = controller.handle(validEvent(), logger);

        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.getBody()).contains("score must be an integer from 0 to 10");
    }

    @Test
    void should_return500WithoutDetails_when_unexpectedErrorHappens() {
        when(useCase.execute(any(SubmitEvaluationCommand.class)))
                .thenThrow(new IllegalStateException("internal detail"));

        APIGatewayV2HTTPResponse response = controller.handle(validEvent(), logger);

        assertThat(response.getStatusCode()).isEqualTo(500);
        assertThat(response.getBody()).doesNotContain("internal detail", "Exception");
    }

    @Test
    void should_logOnlyTheId_when_evaluationIsSaved() {
        when(useCase.execute(any(SubmitEvaluationCommand.class))).thenReturn("eval-1");

        controller.handle(validEvent(), logger);

        verify(logger).log(logged.capture());
        assertThat(logged.getValue())
                .contains("eval-1")
                .doesNotContain("Great class", "11111111-2222-3333-4444-555555555555");
    }

    private static APIGatewayV2HTTPEvent validEvent() {
        return TestEvents.apiGateway("post-evaluation-valid.json");
    }
}
