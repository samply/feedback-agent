package com.samply.feedbackagent.controller;
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

    // Get All Notes
    @GetMapping("/specimen-feedback")
    public List<SpecimenFeedback> getAllNotes() {
        return specimenFeedbackRepository.findAll();
    }

    // Create a new Note
    @PostMapping("/specimen-feedback")
    public SpecimenFeedback createNote(@Valid @RequestBody SpecimenFeedback specimenFeedback) {
        return specimenFeedbackRepository.save(specimenFeedback);
    }

    // Get a Single Note
    @GetMapping("/specimen-feedback/{id}")
    public SpecimenFeedback getNoteById(@PathVariable(value = "id") Long specimenFeedbackId) throws SpecimenFeedbackNotFoundException {
        return specimenFeedbackRepository.findById(specimenFeedbackId)
                .orElseThrow(() -> new SpecimenFeedbackNotFoundException(specimenFeedbackId));
    }

    // Update a Note
    @PutMapping("/specimen-feedback/{id}")
    public SpecimenFeedback updateNote(@PathVariable(value = "id") Long specimenFeedbackId,
                           @Valid @RequestBody SpecimenFeedback specimenFeedbackDetails) throws SpecimenFeedbackNotFoundException {

        SpecimenFeedback specimenFeedback = specimenFeedbackRepository.findById(specimenFeedbackId)
                .orElseThrow(() -> new SpecimenFeedbackNotFoundException(specimenFeedbackId));

        specimenFeedback.setSpecimenID(specimenFeedbackDetails.getSpecimenID());
        specimenFeedback.setType(specimenFeedbackDetails.getType());
        specimenFeedback.setPublicationReference(specimenFeedbackDetails.getPublicationReference());

        SpecimenFeedback updatedSpecimenFeedback = specimenFeedbackRepository.save(specimenFeedback);

        return updatedSpecimenFeedback;
    }

    // Delete a Note
    @DeleteMapping("/specimen-feedback/{id}")
    public ResponseEntity<?> deleteSpecimenFeedback(@PathVariable(value = "id") Long specimenFeedbackId) throws SpecimenFeedbackNotFoundException {
        SpecimenFeedback specimenFeedback = specimenFeedbackRepository.findById(specimenFeedbackId)
                .orElseThrow(() -> new SpecimenFeedbackNotFoundException(specimenFeedbackId));

        specimenFeedbackRepository.delete(specimenFeedback);

        return ResponseEntity.ok().build();
    }
}