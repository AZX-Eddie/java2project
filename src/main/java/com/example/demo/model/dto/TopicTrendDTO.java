package com.example.demo.model.dto;

import java.util.Map;

public class TopicTrendDTO {

    private String topic;
    private Map<String, Long> monthlyQuestionCount;  // "2023-01" -> 数量
    private Map<String, Long> monthlyAnswerCount;
    private Long totalQuestions;
    private Double overallTrend;  // 正数=增长，负数=下降

    public TopicTrendDTO() {}

    // ======= Getter/Setter ========
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public Map<String, Long> getMonthlyQuestionCount() { return monthlyQuestionCount; }
    public void setMonthlyQuestionCount(Map<String, Long> monthlyQuestionCount) { this.monthlyQuestionCount = monthlyQuestionCount; }

    public Map<String, Long> getMonthlyAnswerCount() { return monthlyAnswerCount; }
    public void setMonthlyAnswerCount(Map<String, Long> monthlyAnswerCount) { this.monthlyAnswerCount = monthlyAnswerCount; }

    public Long getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(Long totalQuestions) { this.totalQuestions = totalQuestions; }

    public Double getOverallTrend() { return overallTrend; }
    public void setOverallTrend(Double overallTrend) { this.overallTrend = overallTrend; }
}