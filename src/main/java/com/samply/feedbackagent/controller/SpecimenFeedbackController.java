package com.samply.feedbackagent.controller;
import com.samply.feedbackagent.model.SpecimenFeedbackDto;
import com.samply.feedbackagent.repository.SpecimenFeedbackRepository;
import com.samply.feedbackagent.Util;
import com.samply.feedbackagent.exception.SpecimenFeedbackNotFoundException;
import com.samply.feedbackagent.model.SpecimenFeedback;
import com.samply.feedbackagent.service.Configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@CrossOrigin(origins = "${cors.allowed.origins}") // get from environment variable CORS_ALLOWED_ORIGINS
public class SpecimenFeedbackController {
    private static final Logger logger = LogManager.getLogger(SpecimenFeedbackController.class);

    @Autowired
    SpecimenFeedbackRepository specimenFeedbackRepository;
    @Autowired
    Configuration configuration;
  
    // Check endpoint
    @GetMapping("/info")
    public String info() {
        logger.info("info: Info endpoint called");
        return "OK";
    }
  
    // Get Exporter API key
    @GetMapping("/exporter-api-key")
    public String getExporterApiKey() {
        logger.info("getExporterApiKey: GET exporter-api-key endpoint called");
        String exporterApiKey = System.getenv("EXPORTER_API_KEY");
        return exporterApiKey;
    }

    // Get all SpecimenFeedback
    @GetMapping("/specimen-feedback")
    public List<SpecimenFeedback> getAllSpecimenFeedback() {
        logger.info("getAllSpecimenFeedback: GET specimen-feedback endpoint called");
        return specimenFeedbackRepository.findAll();
    }

    // Create a new SpecimenFeedback
    @PostMapping("/specimen-feedback")
    public SpecimenFeedback createSpecimenFeedback(@Valid @RequestBody SpecimenFeedback specimenFeedback) {
        logger.info("createSpecimenFeedback: POST specimen-feedback endpoint called");
        return specimenFeedbackRepository.save(specimenFeedback);
    }
    // Create multiple SpecimenFeedback
    @PostMapping("/multiple-specimen-feedback")
    public List<SpecimenFeedback> createSpecimenFeedback(@Valid @RequestBody SpecimenFeedbackDto specimenFeedbackDto) {
        logger.info("createSpecimenFeedback: POST multiple-specimen-feedback endpoint called");
        List<SpecimenFeedback> feedbackList = specimenFeedbackDto.getFeedbackList();
        logger.info("createSpecimenFeedback: feedbackList length: " + feedbackList.size());
        logger.info("createSpecimenFeedback: feedbackList: " + Util.jsonStringFomObject(feedbackList));
        List<SpecimenFeedback> specimenFeedback = specimenFeedbackRepository.saveAll(feedbackList);
        logger.info("createSpecimenFeedback: returning");
        return specimenFeedback;
    }

    // Get a Single SpecimenFeedback
    @GetMapping("/specimen-feedback/{id}")
    public SpecimenFeedback getSpecimenFeedbackById(@PathVariable(value = "id") Long specimenFeedbackId) throws SpecimenFeedbackNotFoundException {
        logger.info("getSpecimenFeedbackById: GET specimen-feedback/{id} endpoint called");
        return specimenFeedbackRepository.findById(specimenFeedbackId)
                .orElseThrow(() -> new SpecimenFeedbackNotFoundException(specimenFeedbackId));
    }
}
