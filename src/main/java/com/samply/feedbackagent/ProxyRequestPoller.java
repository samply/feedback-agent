package com.samply.feedbackagent;

import com.samply.feedbackagent.blaze.SpecimenExtensionUpdater;
import com.samply.feedbackagent.model.SpecimenFeedback;
import com.samply.feedbackagent.service.SpecimenFeedbackService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.macasaet.fernet.Key;
import com.macasaet.fernet.StringValidator;
import com.macasaet.fernet.Token;
import com.macasaet.fernet.Validator;

import java.util.*;

/**
 * This class represents a thread that continuously polls for proxy requests,
 * processes them, and updates the relevant data.
 */
@Service
public class ProxyRequestPoller extends Thread {
    private static final Logger logger = LogManager.getLogger(ProxyRequestPoller.class);
    private final SpecimenFeedbackService specimenFeedbackService;
    private final SpecimenExtensionUpdater extensionUpdater;

    private static final String FEEDBACK_HUB_URL = System.getenv("FEEDBACK_HUB_URL");
    private static final String BEAM_PROXY_URI = System.getenv("BEAM_PROXY_URI");
    private static final String FEEDBACK_HUB_BEAM_ID = System.getenv("FEEDBACK_HUB_BEAM_ID");
    private static final String BLAZE_BASE_URL = System.getenv("BLAZE_BASE_URL");
    private static final String FEEDBACK_AGENT_SECRET = System.getenv("FEEDBACK_AGENT_SECRET");
    private static final String FEEDBACK_AGENT_BEAM_ID = System.getenv("FEEDBACK_AGENT_BEAM_ID");
    private static final String feedbackAgentBeamAuthorization = "ApiKey " + FEEDBACK_AGENT_BEAM_ID + " " + FEEDBACK_AGENT_SECRET;

    /**
     * Constructs a new ProxyRequestPoller with the provided SpecimenFeedbackService.
     *
     * @param specimenFeedbackService The service for handling specimen feedback.
     */
    public ProxyRequestPoller(SpecimenFeedbackService specimenFeedbackService) {
        this.specimenFeedbackService = specimenFeedbackService;
        this.extensionUpdater = new SpecimenExtensionUpdater(BLAZE_BASE_URL);
    }

    /**
     * The main run method of the thread that continuously polls for proxy requests,
     * processes them, and updates the relevant data.
     */
    public void run() {
        // final String beamTodoUri = BEAM_PROXY_URI + "/v1/tasks?to=" + FEEDBACK_AGENT_BEAM_ID + "&wait_count=1&wait_time=10s&filter=todo";
        // logger.info("run: beamTodoUri: " + beamTodoUri);
        while (!Thread.currentThread().isInterrupted()) {
            pollProxyWithRequest();

            // Sleep for 60 seconds before polling again
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                logger.warn("run: InterruptedException occurred: " + Util.traceFromException(e));
            }
        }
    }

    /**
     * Poll the proxy once with the request.
     */
    public void pollProxyWithRequest() {
        final String beamTodoUri = BEAM_PROXY_URI + "/v1/tasks?to=" + FEEDBACK_AGENT_BEAM_ID + "&wait_count=1&wait_time=10s&filter=todo";
        logger.info("pollProxyWithRequest: beamTodoUri: " + beamTodoUri);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", feedbackAgentBeamAuthorization);
            HttpEntity<String> request = new HttpEntity<>(null, headers);

            // Pull any pending tasks from Beam
            ResponseEntity<String> response = (new RestTemplate()).exchange(beamTodoUri, HttpMethod.GET, request, String.class);
            logger.info("pollProxyWithRequest: response: " + response);
            if (response.getStatusCode() != HttpStatus.OK && response.getStatusCode() != HttpStatus.PARTIAL_CONTENT) {
                logger.warn("pollProxyWithRequest: Request failed with status code: " + response.getStatusCode());
                return;
            }

            // If a task is found, extract the body from it
            JSONObject bodyObject = extractBeamResponseBody(response);
            if (bodyObject == null) {
                logger.info("pollProxyWithRequest: No tasks found");
                return;
            }

            // Assume that the body is a valid Feedback body, try to
            // extract the parameters needed for propagation, and
            // then perform the propagation.
            String requestId = bodyObject.getString("requestId");
            String accessCode = bodyObject.getString("accessCode");
            String key = bodyObject.getString("key");
            propagatePublicationReference(requestId, accessCode, key);

            // Let the Beam broker know that the task has been completed.
            logger.info("pollProxyWithRequest: build Beam result");
            BeamResult result = buildBeamResult(extractBeamResponseFrom(response), extractBeamResponseId(response));
            logger.info("pollProxyWithRequest: send Beam result");
            ResponseEntity<String> claimResponse = sendBeamResult(result);
            if (claimResponse == null)
                logger.warn("pollProxyWithRequest: Failed to send beam result");
            logger.info("pollProxyWithRequest: claimResponse: " + claimResponse);
        } catch (Exception e) {
            logger.warn("pollProxyWithRequest: Exception occurred: " + Util.traceFromException(e));
            logger.warn("pollProxyWithRequest: BEAM_PROXY_URI: " + BEAM_PROXY_URI);
            logger.warn("pollProxyWithRequest: FEEDBACK_AGENT_BEAM_ID: " + FEEDBACK_AGENT_BEAM_ID);
            logger.warn("pollProxyWithRequest: feedbackAgentBeamAuthorization: " + feedbackAgentBeamAuthorization);
            logger.warn("pollProxyWithRequest: beamTodoUri: " + beamTodoUri);
        }
    }

    private BeamResult buildBeamResult(String from, String id) {
        BeamResult result = new BeamResult();
        // result.setFrom("app1.proxy2.broker");
        result.setFrom(FEEDBACK_HUB_BEAM_ID);
        List<String> to = new LinkedList<String>();
        to.add(from);
        result.setTo(to);
        result.setTask(UUID.fromString(id));
        result.setStatus("succeeded");
        result.setBody("PublicationReference obtained");

        return result;
    }

    /**
     * Extract the body of a Beam task from a response body and return it as a JSONObject.
     * The body is expected to be a JSON string.
     * Return null if no task was found.
     * 
     * @param responseBody The response body from Beam
     * @return The body of the task as a JSONObject, or null if no task was found
     */
    private JSONObject extractBeamResponseBody(ResponseEntity<String> response) {
        JSONObject responseObject = extractResponseObjectFromBeamResponse(response);
        if (responseObject == null)
            return null;
        String body = responseObject.getString("body");
        logger.info("extractBeamResponseBody: body: " + body);
        JSONObject bodyObject = new JSONObject(body);

        return bodyObject;
    }

    /**
     * Extract the "from" field of a Beam task from a response body and return it as a JSONObject.
     * The body is expected to be a JSON string.
     * Return null if no task was found.
     * 
     * @param responseBody The response body from Beam
     * @return The "from" field of the task as a JSONObject, or null if no task was found
     */
    private String extractBeamResponseFrom(ResponseEntity<String> response) {
        JSONObject responseObject = extractResponseObjectFromBeamResponse(response);
        if (responseObject == null)
            return null;
        String from = responseObject.getString("from");
        logger.info("extractBeamResponseFrom: from: " + from);

        return from;
    }

    /**
     * Extract the "id" field of a Beam task from a response body and return it as a JSONObject.
     * The body is expected to be a JSON string.
     * Return null if no task was found.
     * 
     * @param responseBody The response body from Beam
     * @return The "id" field of the task as a JSONObject, or null if no task was found
     */
    private String extractBeamResponseId(ResponseEntity<String> response) {
        JSONObject responseObject = extractResponseObjectFromBeamResponse(response);
        if (responseObject == null)
            return null;
        String id = responseObject.getString("id");
        logger.info("extractBeamResponseFrom: id: " + id);

        return id;
    }

    /**
     * Extract and return the first response object from a Beam response body.
     * Return null if no objects were found.
     * 
     * @param responseBody Beam response body
     * @return The first response object as a JSONObject, or null if no objects were found
     */
    private JSONObject extractResponseObjectFromBeamResponse(ResponseEntity<String> response) {
        String responseBody = response.getBody();
        // Extract list of tasks from response body
        logger.info("extractResponseObjectFromBeamResponse: responseBody: " + responseBody);
        JSONArray responseArray = new JSONArray(responseBody);
        if (responseArray.length() <= 0) {
            logger.info("extractResponseObjectFromBeamResponse: No tasks found");
            return null;
        }
        JSONObject responseObject = responseArray.getJSONObject(0);

        return responseObject;
    }

    /**
     * Retrieves a publication reference token for a given reference code,
     * decrypts it to obtain the reference, and adds the reference to
     * all SpecimenFeedback records with the given request ID.
     * If the request ID does not match any of the saved Specimen RequestIds,
     * nothing is done.
     * 
     * @param requestId The request ID of the SpecimenFeedback records to update
     * @param accessCode The reference code to use to obtain the reference token
     * @param key The key to use to decrypt the reference token
     */
    private void propagatePublicationReference(String requestId, String accessCode, String key) {
        try {
            List<SpecimenFeedback> specimenFeedbacks = specimenFeedbackService.getSpecimenFeedbackByRequestID(requestId);
            if (!specimenFeedbacks.isEmpty()) {
                String token = getReferenceToken(accessCode);
                logger.info("propagatPublicationReference: token: " + token);
                if (token != null) {
                    Key keyObj = new Key(key);
                    final Validator<String> validator = new StringValidator() {
                    };
                    Token tokenObj = Token.fromString(token);
                    String reference = tokenObj.validateAndDecrypt(keyObj, validator);
                    specimenFeedbackService.addPublicationReferenceToSpecimenFeedback(specimenFeedbacks, reference);
                    logger.info("propagatPublicationReference: Successfully updated publication reference");

                    // propagate publication reference into blaze store
                    List<String> sampleIds = new ArrayList<>();
                    for (SpecimenFeedback specimenFeedback : specimenFeedbacks) {
                        sampleIds.add(specimenFeedback.getSampleID());
                    }
                    extensionUpdater.updateSpecimenWithExtension(sampleIds, reference);
                } else
                    logger.warn("propagatPublicationReference: token is null");
            } else
                logger.warn("propagatPublicationReference: RequestId (" + requestId + ") of publication reference does not match any of saved Specimen RequestIds");
        } catch (Exception e) {
            logger.warn("propagatPublicationReference: Exception propagating publication reference", Util.traceFromException(e));
        }
    }

    /**
     * Retrieves a reference token for a given reference code.
     * 
     * Note: this implementation talks directly with the Feedback Hub via
     * it's URL, rather than via Beam.
     * 
     * TODO: make this work via Beam.
     *
     * @param referenceCode The reference code for which to obtain a token.
     * @return The reference token.
     */
    private String getReferenceToken(String referenceCode) {
        final String request_uri = FEEDBACK_HUB_URL + "/reference-token/" + referenceCode;
        logger.info("getReferenceToken: request_uri: " + request_uri);

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> request = new HttpEntity<>(null, headers);
            ResponseEntity<String> response = restTemplate.exchange(request_uri, HttpMethod.GET, request, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                return responseBody;
            } else {
                logger.warn("getReferenceToken: Reference token not obtained");
                return null;
            }
        } catch(Exception e) {
            logger.warn("getReferenceToken: Exception: " + Util.traceFromException(e));
            return null;
        }
    }
    
    /**
     * Sends a BeamResult to update the status of a proxy request.
     *
     * @param result The BeamResult to send.
     * @return The ResponseEntity representing the response.
     */
    private ResponseEntity<String> sendBeamResult(BeamResult result) {
        final String request_uri = BEAM_PROXY_URI + "/v1/tasks/" + result.getTask() + "/results/" + FEEDBACK_AGENT_BEAM_ID;
        logger.info("sendBeamResult: request_uri: " + request_uri);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", feedbackAgentBeamAuthorization);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> resultMap = result.buildMap();
        logger.info("sendBeamResult: resultMap: " + Util.jsonStringFomObject(resultMap));
        if (resultMap.containsKey("metadata"))
            // If metadata is missing, the proxy will throw an invalid body error
            resultMap.put("metadata", new ArrayList<String>());
        if (resultMap.containsKey("from") && !FEEDBACK_AGENT_BEAM_ID.equals(resultMap.get("from").toString())) {
            logger.warn("sendBeamResult: from field is incorrect: " + resultMap.get("from") + ". Changing to: " + FEEDBACK_AGENT_BEAM_ID);
            resultMap.put("from", FEEDBACK_AGENT_BEAM_ID);
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(resultMap, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(request_uri, HttpMethod.PUT, request, String.class);
            if (response.getStatusCode() == HttpStatus.CREATED) {
                return response;
            } else {
                logger.warn("Could not update beam task, status code: " + response.getStatusCode());
                logger.warn("request_uri: " + request_uri);
                logger.warn("feedbackAgentBeamAuthorization: " + feedbackAgentBeamAuthorization);
                logger.warn("result: " + Util.jsonStringFomObject(result));
                return null;
            }
        } catch (Exception e) {
            logger.warn("Exception occurred: " + Util.traceFromException(e));
            logger.warn("request_uri: " + request_uri);
            logger.warn("feedbackAgentBeamAuthorization: " + feedbackAgentBeamAuthorization);
            logger.warn("result: " + Util.jsonStringFomObject(result));
            return null;
        }
    }
}
