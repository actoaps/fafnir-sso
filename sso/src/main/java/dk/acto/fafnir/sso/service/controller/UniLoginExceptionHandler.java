package dk.acto.fafnir.sso.service.controller;

import dk.acto.fafnir.sso.provider.unilogin.UniLoginFlowTrace;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

@ControllerAdvice(assignableTypes = UniLoginController.class)
@ConditionalOnProperty(name = {"UL_CLIENT_ID", "UL_SECRET", "FAFNIR_URL", "UL_WS_USER", "UL_WS_PASS"})
@Slf4j
public class UniLoginExceptionHandler {

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String uri = request != null ? request.getRequestURI() : "-";
        if (uri != null && uri.contains("/unilogin/org") && "user".equals(ex.getParameterName())) {
            UniLoginFlowTrace.log("org", UniLoginFlowTrace.Branch.org_missing_user, "-", null, Map.of(
                "param", ex.getParameterName(),
                "uri", uri,
                "query_string", request.getQueryString() != null ? request.getQueryString() : "-",
                "instance", UniLoginFlowTrace.instanceId()));
        } else {
            log.warn("UniLogin missing request parameter: {} for uri={}", ex.getParameterName(), uri);
        }
    }
}
