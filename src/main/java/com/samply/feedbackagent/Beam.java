package com.samply.feedbackagent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Methods for communicating with a Beam proxy.
 */
public class Beam {
    private static final Logger logger = LogManager.getLogger(Beam.class);
    private String proxyUri;
    private String appId;
    private String authorization;

    public Beam(String proxyUri, String appId, String appSecret) {
        this.proxyUri = proxyUri;
        this.appId = appId;
        authorization = "ApiKey " + appId + " " + appSecret;
    }

    public JSONObject listenForTask() {
        final String beamTodoUri = proxyUri + "/v1/tasks?to=" + appId + "&wait_count=1&wait_time=10s&filter=todo";
        logger.info("listenForTask: beamTodoUri: " + beamTodoUri);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authorization);
            HttpEntity<String> request = new HttpEntity<>(null, headers);

            // Pull any pending tasks from Beam
            ResponseEntity<String> response = (new RestTemplate()).exchange(beamTodoUri, HttpMethod.GET, request, String.class);
            logger.info("listenForTask: response: " + response);
            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.PARTIAL_CONTENT) {
                String responseBody = response.getBody();
                // Extract list of tasks from response body
                JSONArray responseArray = new JSONArray(responseBody);
                if (responseArray.length() > 0) {
                    JSONObject responseObject = responseArray.getJSONObject(0);
                    return responseObject;
                } else {
                    logger.info("listenForTask: No tasks found");
                    return null;
                }
            } else
                logger.warn("listenForTask: Request failed with status code: " + response.getStatusCode());
        } catch (Exception e) {
            logger.warn("listenForTask: Exception occurred: " + Util.traceFromException(e));
        }

        logger.warn("listenForTask: proxyUri: " + proxyUri);
        logger.warn("listenForTask: appId: " + appId);
        logger.warn("listenForTask: authorization: " + authorization);
        logger.warn("listenForTask: beamTodoUri: " + beamTodoUri);

        return null;
    }

    public boolean returnResult(String taskId, Map<String, Object> body) {
        logger.info("returnResult: entered");
        String requestUri = proxyUri + "/v1/tasks/" + taskId + "/results/" + appId;
        try {
            logger.info("returnResult: requestUri: " + requestUri);
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authorization);
            headers.setContentType(MediaType.APPLICATION_JSON);
        
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            logger.info("returnResult: request: " + request);

            ResponseEntity<String> response = restTemplate.exchange(requestUri, HttpMethod.PUT, request, String.class);
            if (response.getStatusCode() == HttpStatus.CREATED) {
                logger.info("returnResult: request successful");
                return true;
            } else
                logger.warn("returnResult: Could not update beam task, status code: " + response.getStatusCode());
        } catch (Exception e) {
            logger.warn("returnResult: Exception occurred: " + Util.traceFromException(e));
        }

        logger.warn("returnResult: requestUri: " + requestUri);
        logger.warn("returnResult: authorization: " + authorization);
        logger.warn("returnResult: taskId: " + taskId);
        logger.warn("returnResult: body: " + Util.jsonStringFomObject(body));

        return false;
}
}
