package com.example.demo.model.dto;

import java.util.Map;

public class TopicTrendDTO {
    private String topic;
    private Map<String, Long> monthlyQuestionCount;
    private Map<String, Long> monthlyAnswerCount;
    private Long totalQuestions;
    private Double overallTrend;

    // 活动指标
    private Double avgScore;              // 平均分数（upvotes）
    private Double avgViewCount;          // 平均浏览量
    private Double acceptedAnswerRate;    // 接受答案比例
    private Long totalAnswers;            // 总答案数
    private Map<String, Double> monthlyAvgScore;  // 每月平均分数

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

    // getter/setter
    public Double getAvgScore() { return avgScore; }
    public void setAvgScore(Double avgScore) { this.avgScore = avgScore; }

    public Double getAvgViewCount() { return avgViewCount; }
    public void setAvgViewCount(Double avgViewCount) { this.avgViewCount = avgViewCount; }

    public Double getAcceptedAnswerRate() { return acceptedAnswerRate; }
    public void setAcceptedAnswerRate(Double acceptedAnswerRate) { this.acceptedAnswerRate = acceptedAnswerRate; }

    public Long getTotalAnswers() { return totalAnswers; }
    public void setTotalAnswers(Long totalAnswers) { this.totalAnswers = totalAnswers; }

    public Map<String, Double> getMonthlyAvgScore() { return monthlyAvgScore; }
    public void setMonthlyAvgScore(Map<String, Double> monthlyAvgScore) { this.monthlyAvgScore = monthlyAvgScore; }
}