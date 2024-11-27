package com.samply.feedbackagent;

import jakarta.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.samply.feedbackagent.controller.CorsConfig;

@SpringBootApplication
public class FeedbackAgentApplication {
    private static final Logger logger = LogManager.getLogger(FeedbackAgentApplication.class);

	@Autowired
	private ProxyRequestPoller proxyRequestPoller;

	public static void main(String[] args) {
		logger.info("Starting Feedback Agent");
		SpringApplication.run(FeedbackAgentApplication.class, args);
	}

	@PostConstruct
	public void startProxyRequestPoller() {
		proxyRequestPoller.start();
	}
}
