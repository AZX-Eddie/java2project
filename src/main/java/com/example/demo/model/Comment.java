package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    private Long commentId;

    @Column(columnDefinition = "TEXT")
    private String body;

    private Integer score;
    private String creationDate;

    private String ownerName;
    private Long ownerUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id")
    private Answer answer;

    public Comment() {
    }

    // ======= Getter/Setter ========
    public Long getCommentId() { return commentId; }
    public void setCommentId(Long commentId) { this.commentId = commentId; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getCreationDate() { return creationDate; }
    public void setCreationDate(String creationDate) { this.creationDate = creationDate; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }

    public Answer getAnswer() { return answer; }
    public void setAnswer(Answer answer) { this.answer = answer; }
}