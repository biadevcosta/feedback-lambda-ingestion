package com.feedback.ingestion.adapter.in.apigateway;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.feedback.ingestion.application.usecase.SubmitEvaluationUseCase;
import com.feedback.ingestion.domain.exception.DomainValidationException;
import java.util.Map;

/** POST /evaluations: runs the use case and maps its outcome to an HTTP response. */
public final class SubmitEvaluationController {

    private final SubmitEvaluationUseCase useCase;
    private final SubmitEvaluationRequestMapper mapper;

    public SubmitEvaluationController(SubmitEvaluationUseCase useCase, SubmitEvaluationRequestMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    public APIGatewayV2HTTPResponse handle(APIGatewayV2HTTPEvent event, LambdaLogger logger) {
        try {
            String evaluationId = useCase.execute(mapper.toCommand(event));
            logger.log("Saved evaluation " + evaluationId);
            return HttpResponses.created(Map.of("evaluationId", evaluationId));
        } catch (UnauthenticatedException e) {
            return HttpResponses.unauthorized();
        } catch (InvalidRequestException | DomainValidationException e) {
            return HttpResponses.badRequest(e.getMessage());
        } catch (RuntimeException e) {
            // LGPD: only the error type, never the description or the user
            logger.log("Failed to save evaluation: " + e.getClass().getSimpleName());
            return HttpResponses.internalError();
        }
    }
}
