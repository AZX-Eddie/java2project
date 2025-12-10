package com.example.demo.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    private Long questionId;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    // 标签集合（用于主题分析）
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "question_tags", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    private String ownerName;
    private Integer ownerReputation;
    private Long ownerUserId;

    private Integer score;
    private Integer viewCount;
    private Integer answerCount;
    private Boolean isAnswered;
    private Long acceptedAnswerId;

    private String creationDate;
    private String lastActivityDate;
    private String link;

    // 关联答案
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Answer> answers = new ArrayList<>();

    // 关联评论
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    public Question() {
    }

    // ======= Getter/Setter ========
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public Integer getOwnerReputation() { return ownerReputation; }
    public void setOwnerReputation(Integer ownerReputation) { this.ownerReputation = ownerReputation; }

    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }

    public Integer getAnswerCount() { return answerCount; }
    public void setAnswerCount(Integer answerCount) { this.answerCount = answerCount; }

    public Boolean getIsAnswered() { return isAnswered; }
    public void setIsAnswered(Boolean isAnswered) { this.isAnswered = isAnswered; }

    public Long getAcceptedAnswerId() { return acceptedAnswerId; }
    public void setAcceptedAnswerId(Long acceptedAnswerId) { this.acceptedAnswerId = acceptedAnswerId; }

    public String getCreationDate() { return creationDate; }
    public void setCreationDate(String creationDate) { this.creationDate = creationDate; }

    public String getLastActivityDate() { return lastActivityDate; }
    public void setLastActivityDate(String lastActivityDate) { this.lastActivityDate = lastActivityDate; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public List<Answer> getAnswers() { return answers; }
    public void setAnswers(List<Answer> answers) { this.answers = answers; }

    public List<Comment> getComments() { return comments; }
    public void setComments(List<Comment> comments) { this.comments = comments; }
}