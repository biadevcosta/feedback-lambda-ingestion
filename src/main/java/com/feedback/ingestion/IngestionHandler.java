package com.feedback.ingestion;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.feedback.ingestion.adapter.in.apigateway.SubmitEvaluationController;
import com.feedback.ingestion.config.DependencyFactory;

/**
 * Lambda entry point (referenced by Terraform). Bootstrap only: the runtime creates one instance
 * per execution environment, so the dependencies built here are reused across invocations.
 */
public class IngestionHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final SubmitEvaluationController controller;

    public IngestionHandler() {
        this(DependencyFactory.fromEnvironment().submitEvaluationController());
    }

    IngestionHandler(SubmitEvaluationController controller) {
        this.controller = controller;
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        return controller.handle(event, context.getLogger());
    }
}
