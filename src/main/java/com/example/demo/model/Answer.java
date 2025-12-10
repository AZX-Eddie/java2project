package com.example.demo.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "answers")
public class Answer {

    @Id
    private Long answerId;

    @Column(columnDefinition = "TEXT")
    private String body;

    private Integer score;
    private Boolean isAccepted;

    private String ownerName;
    private Integer ownerReputation;
    private Long ownerUserId;

    private String creationDate;
    private String lastActivityDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @OneToMany(mappedBy = "answer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    public Answer() {
    }

    // ======= Getter/Setter ========
    public Long getAnswerId() { return answerId; }
    public void setAnswerId(Long answerId) { this.answerId = answerId; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public Boolean getIsAccepted() { return isAccepted; }
    public void setIsAccepted(Boolean isAccepted) { this.isAccepted = isAccepted; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public Integer getOwnerReputation() { return ownerReputation; }
    public void setOwnerReputation(Integer ownerReputation) { this.ownerReputation = ownerReputation; }

    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }

    public String getCreationDate() { return creationDate; }
    public void setCreationDate(String creationDate) { this.creationDate = creationDate; }

    public String getLastActivityDate() { return lastActivityDate; }
    public void setLastActivityDate(String lastActivityDate) { this.lastActivityDate = lastActivityDate; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }

    public List<Comment> getComments() { return comments; }
    public void setComments(List<Comment> comments) { this.comments = comments; }
}