package com.example.demo.model.dto;

import java.util.Map;

public class TopicTrendDTO {
    private String topic;
    private Long totalQuestions;
    private Map<String, Long> monthlyQuestionCount;
    private Map<String, Long> monthlyAnswerCount;
    private Map<String, Double> monthlyAvgScore;

    // 🔥 新增字段：每月热度指数
    private Map<String, Double> monthlyHeatIndex;

    // 🔥 新增字段：整体热度指数
    private Double overallHeatIndex;

    private Double overallTrend;
    private Double avgScore;
    private Double avgViewCount;
    private Double acceptedAnswerRate;
    private Long totalAnswers;

    // Getters and Setters

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Long getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Long totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public Map<String, Long> getMonthlyQuestionCount() {
        return monthlyQuestionCount;
    }

    public void setMonthlyQuestionCount(Map<String, Long> monthlyQuestionCount) {
        this.monthlyQuestionCount = monthlyQuestionCount;
    }

    public Map<String, Long> getMonthlyAnswerCount() {
        return monthlyAnswerCount;
    }

    public void setMonthlyAnswerCount(Map<String, Long> monthlyAnswerCount) {
        this.monthlyAnswerCount = monthlyAnswerCount;
    }

    public Map<String, Double> getMonthlyAvgScore() {
        return monthlyAvgScore;
    }

    public void setMonthlyAvgScore(Map<String, Double> monthlyAvgScore) {
        this.monthlyAvgScore = monthlyAvgScore;
    }

    public Map<String, Double> getMonthlyHeatIndex() {
        return monthlyHeatIndex;
    }

    public void setMonthlyHeatIndex(Map<String, Double> monthlyHeatIndex) {
        this.monthlyHeatIndex = monthlyHeatIndex;
    }

    public Double getOverallHeatIndex() {
        return overallHeatIndex != null ? overallHeatIndex : 0.0;
    }

    public void setOverallHeatIndex(Double overallHeatIndex) {
        this.overallHeatIndex = overallHeatIndex;
    }

    public Double getOverallTrend() {
        return overallTrend;
    }

    public void setOverallTrend(Double overallTrend) {
        this.overallTrend = overallTrend;
    }

    public Double getAvgScore() {
        return avgScore;
    }

    public void setAvgScore(Double avgScore) {
        this.avgScore = avgScore;
    }

    public Double getAvgViewCount() {
        return avgViewCount;
    }

    public void setAvgViewCount(Double avgViewCount) {
        this.avgViewCount = avgViewCount;
    }

    public Double getAcceptedAnswerRate() {
        return acceptedAnswerRate;
    }

    public void setAcceptedAnswerRate(Double acceptedAnswerRate) {
        this.acceptedAnswerRate = acceptedAnswerRate;
    }

    public Long getTotalAnswers() {
        return totalAnswers;
    }

    public void setTotalAnswers(Long totalAnswers) {
        this.totalAnswers = totalAnswers;
    }
}