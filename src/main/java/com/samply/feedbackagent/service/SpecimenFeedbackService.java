package com.samply.feedbackagent.service;

import com.samply.feedbackagent.model.SpecimenFeedback;
import com.samply.feedbackagent.repository.SpecimenFeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpecimenFeedbackService {

    /*@Autowired
    private SpecimenFeedbackRepository specimenFeedbackRepository;*/
    private final SpecimenFeedbackRepository specimenFeedbackRepository;

    public SpecimenFeedbackService(SpecimenFeedbackRepository specimenFeedbackRepository) {
        this.specimenFeedbackRepository = specimenFeedbackRepository;
    }

    public List<SpecimenFeedback> getSpecimenFeedbackByRequestID(String requestId) {
        return specimenFeedbackRepository.findByRequest(requestId);
    }
}