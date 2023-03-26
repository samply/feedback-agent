package com.samply.feedbackagent.repository;

import com.samply.feedbackagent.model.SpecimenFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpecimenFeedbackRepository extends JpaRepository<SpecimenFeedback, Long> { }