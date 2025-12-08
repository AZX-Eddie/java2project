package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    private Long questionId;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    private String ownerName;
    private Integer score;
    private String creationDate;

    public Question() {
    }

    public Question(Long questionId, String title, String body, String ownerName, Integer score, String creationDate) {
        this.questionId = questionId;
        this.title = title;
        this.body = body;
        this.ownerName = ownerName;
        this.score = score;
        this.creationDate = creationDate;
    }

    // ======= Getter/Setter ========
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getCreationDate() { return creationDate; }
    public void setCreationDate(String creationDate) { this.creationDate = creationDate; }
}
