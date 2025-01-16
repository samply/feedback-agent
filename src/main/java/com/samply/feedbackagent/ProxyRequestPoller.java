package com.samply.feedbackagent;

import com.samply.feedbackagent.blaze.SpecimenExtensionUpdater;
import com.samply.feedbackagent.model.SpecimenFeedback;
import com.samply.feedbackagent.service.SpecimenFeedbackService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    private static final String BLAZE_BASE_URL = System.getenv("BLAZE_BASE_URL");
    private static final String FEEDBACK_AGENT_SECRET = System.getenv("FEEDBACK_AGENT_SECRET");
    private static final String FEEDBACK_AGENT_BEAM_ID = System.getenv("FEEDBACK_AGENT_BEAM_ID");
    private Beam beam = new Beam(BEAM_PROXY_URI, FEEDBACK_AGENT_BEAM_ID, FEEDBACK_AGENT_SECRET);
    
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
        logger.info("pollProxyWithRequest: entered");
        try {
            JSONObject task = beam.listenForTask();
            if (task == null) {
                logger.info("pollProxyWithRequest: No tasks found");
                return;
            }
    
            // Assuming that the body is a valid Feedback body, try to
            // extract the parameters needed for propagation, and
            // then perform the propagation.
            JSONObject bodyObject = new JSONObject(task.getString("body"));
            logger.info("pollProxyWithRequest: propagating publication reference, requestId: " + bodyObject.getString("requestId") + ", accessCode: " + bodyObject.getString("accessCode") + ", key: " + bodyObject.getString("key"));
            propagatePublicationReference(bodyObject.getString("requestId"), bodyObject.getString("accessCode"), bodyObject.getString("key"));

            // Build the body object that will be sent back to Beam
            logger.info("pollProxyWithRequest: build Beam result");
            BeamResult result = buildBeamResult(task.getString("from"), task.getString("id"));

            // Let the Beam broker know that the task has been completed.
            logger.info("pollProxyWithRequest: send Beam result");
            if (!beam.returnResult(result.getTask().toString(), result.buildMap()))
                logger.warn("pollProxyWithRequest: Failed to send beam result");
        } catch (Exception e) {
            logger.warn("pollProxyWithRequest: Exception occurred: " + Util.traceFromException(e));
        }
        logger.info("pollProxyWithRequest: done");
    }

    private BeamResult buildBeamResult(String from, String id) {
        BeamResult result = new BeamResult();
        result.setFrom(FEEDBACK_AGENT_BEAM_ID);
        List<String> to = new LinkedList<String>();
        to.add(from);
        result.setTo(to);
        result.setTask(UUID.fromString(id));
        result.setStatus("succeeded");
        result.setBody("PublicationReference obtained");
        // If metadata is missing, the proxy will throw an invalid body error,
        // so insert an empty list
        result.setMetadata("[]");

        return result;
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
}
