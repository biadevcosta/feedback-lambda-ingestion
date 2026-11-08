package com.feedback.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.feedback.ingestion.adapter.in.apigateway.SubmitEvaluationController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IngestionHandlerTest {

    @Mock
    private SubmitEvaluationController controller;

    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    @Test
    void should_delegateToController() {
        // given
        var event = new APIGatewayV2HTTPEvent();
        var expected = APIGatewayV2HTTPResponse.builder().withStatusCode(201).build();
        when(context.getLogger()).thenReturn(logger);
        when(controller.handle(event, logger)).thenReturn(expected);

        // when
        APIGatewayV2HTTPResponse response = new IngestionHandler(controller).handleRequest(event, context);

        // then
        assertThat(response).isSameAs(expected);
    }
}
