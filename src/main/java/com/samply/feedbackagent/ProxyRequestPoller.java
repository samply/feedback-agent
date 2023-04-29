package com.samply.feedbackagent;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProxyRequestPoller extends Thread {
    private final HttpClient httpClient;
    private final HttpGet httpGet;
    private final int sleepTime;

    public ProxyRequestPoller(int sleepTime) {
        this.httpClient = HttpClients.createDefault();
        this.httpGet = new HttpGet(System.getenv("BEAM_PROXY_URI") + "/v1/tasks?to=app1.proxy2.broker&wait_count=1&filter=todo");
        this.httpGet.setHeader("Authorization", "ApiKey app1.proxy2.broker App1Secret");
        this.sleepTime = sleepTime;
    }

    public void run() {
        boolean keepRunning = true;
        while (keepRunning) {
            try {
                HttpResponse response = httpClient.execute(httpGet);
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    System.out.println("status code: " + statusCode);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                    StringBuilder responseBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                    String responseBody = responseBuilder.toString();
                    // todo something with the response
                    System.out.println(responseBody);
                } else {
                    System.out.println("Unexpected status code: " + statusCode);
                }
                Thread.sleep(sleepTime);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
