package com.samply.feedbackagent.model;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.samply.feedbackagent.controller.SpecimenFeedbackController;

public class SpecimenFeedbackDto {
    private static final Logger logger = LogManager.getLogger(SpecimenFeedbackDto.class);

    private List<SpecimenFeedback> feedbackList;

    public SpecimenFeedbackDto() {
        super();
    }

    public SpecimenFeedbackDto(List<SpecimenFeedback> feedbackList) {
        this.feedbackList = feedbackList;
    }

    public List<SpecimenFeedback> getFeedbackList() {
        logger.info("getFeedbackList: entered, this.feedbackList: " + this.feedbackList);
        return this.feedbackList;
    }

    public void setFeedbackList(List<SpecimenFeedback> feedbackList) {
        this.feedbackList = feedbackList;
    }
}
