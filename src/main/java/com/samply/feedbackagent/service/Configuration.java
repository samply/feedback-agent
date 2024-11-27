package com.samply.feedbackagent.service;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Environment configuration parameters. */
@Data
@Component
public class Configuration {
    @Value("${fa.cors.origin}")
    private String corsOrigin;
}

