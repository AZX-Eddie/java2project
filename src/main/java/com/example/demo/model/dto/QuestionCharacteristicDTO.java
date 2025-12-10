package com.example.demo.model.dto;

import java.util.Map;

public class QuestionCharacteristicDTO {

    private String category;  // "solvable" 或 "hard-to-solve"
    private Long questionCount;
    private Double avgTitleLength;
    private Double avgBodyLength;
    private Double avgCodeSnippetCount;
    private Double avgOwnerReputation;
    private Double avgTagCount;
    private Map<String, Long> topTags;
    private Map<Integer, Long> hourDistribution;

    public QuestionCharacteristicDTO() {}

    // ======= Getter/Setter ========
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Long getQuestionCount() { return questionCount; }
    public void setQuestionCount(Long questionCount) { this.questionCount = questionCount; }

    public Double getAvgTitleLength() { return avgTitleLength; }
    public void setAvgTitleLength(Double avgTitleLength) { this.avgTitleLength = avgTitleLength; }

    public Double getAvgBodyLength() { return avgBodyLength; }
    public void setAvgBodyLength(Double avgBodyLength) { this.avgBodyLength = avgBodyLength; }

    public Double getAvgCodeSnippetCount() { return avgCodeSnippetCount; }
    public void setAvgCodeSnippetCount(Double avgCodeSnippetCount) { this.avgCodeSnippetCount = avgCodeSnippetCount; }

    public Double getAvgOwnerReputation() { return avgOwnerReputation; }
    public void setAvgOwnerReputation(Double avgOwnerReputation) { this.avgOwnerReputation = avgOwnerReputation; }

    public Double getAvgTagCount() { return avgTagCount; }
    public void setAvgTagCount(Double avgTagCount) { this.avgTagCount = avgTagCount; }

    public Map<String, Long> getTopTags() { return topTags; }
    public void setTopTags(Map<String, Long> topTags) { this.topTags = topTags; }

    public Map<Integer, Long> getHourDistribution() { return hourDistribution; }
    public void setHourDistribution(Map<Integer, Long> hourDistribution) { this.hourDistribution = hourDistribution; }
}