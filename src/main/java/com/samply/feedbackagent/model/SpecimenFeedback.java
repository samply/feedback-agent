package com.samply.feedbackagent.model;

import jakarta.persistence.*;
@Entity
@Table(name = "specimen_feedback")
public class SpecimenFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(name = "specimen_id")
    private long specimenID;

    @Column(name = "type")
    private String type;

    @Column(name = "publication_reference")
    private String publicationReference;

    public SpecimenFeedback() {
        super();
    }

    public SpecimenFeedback(long id, long specimenID, String type, String publicationReference) {
        this.id = id;
        this.specimenID = specimenID;
        this.type = type;
        this.publicationReference = publicationReference;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSpecimenID() {
        return specimenID;
    }

    public void setSpecimenID(long specimenID) {
        this.specimenID = specimenID;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPublicationReference() {
        return publicationReference;
    }

    public void setPublicationReference(String publicationReference) {
        this.publicationReference = publicationReference;
    }
}