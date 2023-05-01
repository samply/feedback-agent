package com.samply.feedbackagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samply.feedbackagent.model.SpecimenFeedback;
import com.samply.feedbackagent.service.SpecimenFeedbackService;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.macasaet.fernet.Key;
import com.macasaet.fernet.StringValidator;
import com.macasaet.fernet.Token;
import com.macasaet.fernet.Validator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

@Service
public class ProxyRequestPoller extends Thread {
    private final SpecimenFeedbackService specimenFeedbackService;

    /*public ProxyRequestPoller(int sleepTime) {
        this.httpClient = HttpClients.createDefault();
        this.httpGet = new HttpGet(System.getenv("BEAM_PROXY_URI") + "/v1/tasks?to=app1.proxy2.broker&wait_count=1&wait_time=10s&filter=todo");
        this.httpGet.setHeader("Authorization", "ApiKey app1.proxy2.broker App1Secret");
        this.sleepTime = sleepTime;
        this.objectMapper = new ObjectMapper();

        this.specimenFeedbackService = new SpecimenFeedbackService();
    }*/
    public ProxyRequestPoller(SpecimenFeedbackService specimenFeedbackService) {
        this.specimenFeedbackService = specimenFeedbackService;
    }
    public void run() {
        while (true) {
            final String request_uri = System.getenv("BEAM_PROXY_URI") + "/v1/tasks?to=app1.proxy2.broker&wait_count=1&wait_time=10s&filter=todo";
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "ApiKey app1.proxy2.broker App1Secret");
            HttpEntity<String> request = new HttpEntity<>(null, headers);

            ResponseEntity<String> response = restTemplate.exchange(request_uri, HttpMethod.GET, request, String.class);

            System.out.println(response);

            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                JSONArray responseArray = new JSONArray(responseBody);
                if (responseArray.length() > 0) {
                    JSONObject responseObject = responseArray.getJSONObject(0);
                    String body = responseObject.getString("body");
                    JSONObject bodyObject = new JSONObject(body);

                    String requestId = bodyObject.getString("requestId");
                    String accessCode = bodyObject.getString("accessCode");
                    String key = bodyObject.getString("key");

                    List<SpecimenFeedback> specimenFeedbacks = specimenFeedbackService.getSpecimenFeedbackByRequestID(requestId);
                    if (!specimenFeedbacks.isEmpty()) {
                        String token = getReferenceToken(accessCode); //todo 1
                        System.out.println(token);
                        /*Key keyObj = new Key(key); //todo 2
                        final Validator<String> validator = new StringValidator() {};
                        Token tokenObj = Token.fromString(token);
                        tokenObj.validateAndDecrypt(keyObj, validator);*/

                        //todo 3 let hub know to delete the entry for this agent
                    } else {
                        //todo 3 let hub know to delete the entry for this agent
                        //log something
                    }
                }
            }
        }
    }
    private String getReferenceToken(String referenceCode) {
        final String request_uri = System.getenv("FEEDBACK_HUB_URL") + "/reference-token/" + referenceCode; //todo 1 aj v hube treba
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        //todo 4? //headers.set("x-api-key", "secretKey");
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<String> response = restTemplate.exchange(request_uri, HttpMethod.GET, request, String.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            String responseBody = response.getBody();
            return responseBody;
        } else {
            //log something
            return null;
        }
    }
    /*private void sendResult(String from, List<String> to, String taskId, String status, String body) throws IOException {
        HttpPut httpPut = new HttpPut(System.getenv("BEAM_PROXY_URI") + "/v1/tasks/" + taskId + "/results/app1.proxy2.broker");
        httpPut.setHeader("Authorization", "ApiKey app1.proxy2.broker App1Secret");

        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("from", from);
        jsonMap.put("to", to);
        jsonMap.put("task", taskId);
        jsonMap.put("status", status);
        jsonMap.put("metadata", null);
        if (body != null) {
            jsonMap.put("body", body);
        }

        String requestBody = new ObjectMapper().writeValueAsString(jsonMap);
        httpPut.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));

        HttpResponse response = httpClient.execute(httpPut);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 201) {
            System.out.println("Task " + taskId + " claimed");
        } else {
            System.out.println("Error claiming task " + taskId + ". Status code: " + statusCode + " " + response);
        }
    }*/
}
