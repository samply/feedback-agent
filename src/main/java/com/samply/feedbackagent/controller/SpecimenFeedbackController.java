package com.samply.feedbackagent.controller;
import com.samply.feedbackagent.model.SpecimenFeedbackDto;
import com.samply.feedbackagent.repository.SpecimenFeedbackRepository;
import com.samply.feedbackagent.exception.SpecimenFeedbackNotFoundException;
import com.samply.feedbackagent.model.SpecimenFeedback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import java.util.List;

@RestController
public class SpecimenFeedbackController {
    @Autowired
    SpecimenFeedbackRepository specimenFeedbackRepository;

    // Get all SpecimenFeedback
    @CrossOrigin(origins = "http://localhost:9000")
    @GetMapping("/specimen-feedback")
    public List<SpecimenFeedback> getAllSpecimenFeedback() {
        return specimenFeedbackRepository.findAll();
    }

    // Create a new SpecimenFeedback
    @CrossOrigin(origins = "http://localhost:9000")
    @PostMapping("/specimen-feedback")
    public SpecimenFeedback createSpecimenFeedback(@Valid @RequestBody SpecimenFeedback specimenFeedback) {
        return specimenFeedbackRepository.save(specimenFeedback);
    }
    // Create multiple SpecimenFeedback
    @CrossOrigin(origins = "http://localhost:9000")
    @PostMapping("/multiple-specimen-feedback")
    public List<SpecimenFeedback> createSpecimenFeedback(@Valid @RequestBody SpecimenFeedbackDto specimenFeedbackDto) {
        return specimenFeedbackRepository.saveAll(specimenFeedbackDto.getFeedbackList());
    }

    // Get a Single SpecimenFeedback
    @CrossOrigin(origins = "http://localhost:9000")
    @GetMapping("/specimen-feedback/{id}")
    public SpecimenFeedback getSpecimenFeedbackById(@PathVariable(value = "id") Long specimenFeedbackId) throws SpecimenFeedbackNotFoundException {
        return specimenFeedbackRepository.findById(specimenFeedbackId)
                .orElseThrow(() -> new SpecimenFeedbackNotFoundException(specimenFeedbackId));
    }

    // Update a SpecimenFeedback
    @CrossOrigin(origins = "http://localhost:9000")
    @PutMapping("/specimen-feedback/{id}")
    public SpecimenFeedback updateSpecimenFeedback(@PathVariable(value = "id") Long specimenFeedbackId,
                           @Valid @RequestBody SpecimenFeedback specimenFeedbackDetails) throws SpecimenFeedbackNotFoundException {

        SpecimenFeedback specimenFeedback = specimenFeedbackRepository.findById(specimenFeedbackId)
                .orElseThrow(() -> new SpecimenFeedbackNotFoundException(specimenFeedbackId));

        specimenFeedback.setSampleID(specimenFeedbackDetails.getSampleID());
        specimenFeedback.setPatientID(specimenFeedbackDetails.getPatientID());
        specimenFeedback.setCollectionDate(specimenFeedbackDetails.getCollectionDate());
        specimenFeedback.setRequestID(specimenFeedbackDetails.getRequestID());
        specimenFeedback.setType(specimenFeedbackDetails.getType());
        specimenFeedback.setPublicationReference(specimenFeedbackDetails.getPublicationReference());

        return specimenFeedbackRepository.save(specimenFeedback);
    }
    // Update a SpecimenFeedback
    @CrossOrigin(origins = "http://localhost:9000")
    @PutMapping("/specimen-feedback/add-publication/{id}")
    public SpecimenFeedback addPublicationReferenceSpecimenFeedback(@PathVariable(value = "id") Long specimenFeedbackId,
                                                   @Valid @RequestBody SpecimenFeedback specimenFeedbackDetails) throws SpecimenFeedbackNotFoundException {

        SpecimenFeedback specimenFeedback = specimenFeedbackRepository.findById(specimenFeedbackId)
                .orElseThrow(() -> new SpecimenFeedbackNotFoundException(specimenFeedbackId));

        specimenFeedback.setPublicationReference(specimenFeedbackDetails.getPublicationReference());

        return specimenFeedbackRepository.save(specimenFeedback);
    }

    // Delete a SpecimenFeedback
    @CrossOrigin(origins = "http://localhost:9000")
    @DeleteMapping("/specimen-feedback/{id}")
    public ResponseEntity<?> deleteSpecimenFeedback(@PathVariable(value = "id") Long specimenFeedbackId) throws SpecimenFeedbackNotFoundException {
        SpecimenFeedback specimenFeedback = specimenFeedbackRepository.findById(specimenFeedbackId)
                .orElseThrow(() -> new SpecimenFeedbackNotFoundException(specimenFeedbackId));

        specimenFeedbackRepository.delete(specimenFeedback);

        return ResponseEntity.ok().build();
    }
}