package com.samply.feedbackagent.controller;
import com.samply.feedbackagent.model.SpecimenFeedbackDto;
import com.samply.feedbackagent.repository.SpecimenFeedbackRepository;
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
public class SpecimenFeedbackController {
    private static final Logger logger = LogManager.getLogger(SpecimenFeedbackController.class);

    @Autowired
    SpecimenFeedbackRepository specimenFeedbackRepository;
    @Autowired
    Configuration configuration;
  
    // Check endpoint
    //@CrossOrigin(origins = "http://localhost:9000") // TODO: should be replaced by an environment variable
    @GetMapping("/info")
    public String info() {
        logger.info("info: Info endpoint called");
        return "OK";
    }

    // Get all SpecimenFeedback
    //@CrossOrigin(origins = "http://localhost:9000")
    @GetMapping("/specimen-feedback")
    public List<SpecimenFeedback> getAllSpecimenFeedback() {
        logger.info("getAllSpecimenFeedback: GET specimen-feedback endpoint called");
        return specimenFeedbackRepository.findAll();
    }

    // Create a new SpecimenFeedback
    //@CrossOrigin(origins = "http://localhost:9000")
    @PostMapping("/specimen-feedback")
    public SpecimenFeedback createSpecimenFeedback(@Valid @RequestBody SpecimenFeedback specimenFeedback) {
        logger.info("createSpecimenFeedback: POST specimen-feedback endpoint called");
        return specimenFeedbackRepository.save(specimenFeedback);
    }
    // Create multiple SpecimenFeedback
    //@CrossOrigin(origins = "http://localhost:9000")
    @PostMapping("/multiple-specimen-feedback")
    public List<SpecimenFeedback> createSpecimenFeedback(@Valid @RequestBody SpecimenFeedbackDto specimenFeedbackDto) {
        logger.info("createSpecimenFeedback: POST multiple-specimen-feedback endpoint called");
        List<SpecimenFeedback> feedbackList = specimenFeedbackDto.getFeedbackList();
        logger.info("createSpecimenFeedback: feedbackList length: " + feedbackList.size());
        List<SpecimenFeedback> specimenFeedback = specimenFeedbackRepository.saveAll(feedbackList);
        logger.info("createSpecimenFeedback: returning");
        return specimenFeedback;
    }

    // Get a Single SpecimenFeedback
    //@CrossOrigin(origins = "http://localhost:9000")
    @GetMapping("/specimen-feedback/{id}")
    public SpecimenFeedback getSpecimenFeedbackById(@PathVariable(value = "id") Long specimenFeedbackId) throws SpecimenFeedbackNotFoundException {
        logger.info("getSpecimenFeedbackById: GET specimen-feedback/{id} endpoint called");
        return specimenFeedbackRepository.findById(specimenFeedbackId)
                .orElseThrow(() -> new SpecimenFeedbackNotFoundException(specimenFeedbackId));
    }
}
