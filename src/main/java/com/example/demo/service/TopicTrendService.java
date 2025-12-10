package com.example.demo.service;

import com.example.demo.model.Question;
import com.example.demo.model.dto.TopicTrendDTO;
import com.example.demo.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TopicTrendService {

    private final QuestionRepository questionRepository;

    // Java课程相关主题
    private static final List<String> JAVA_TOPICS = Arrays.asList(
            "generics", "collections", "stream", "lambda", "multithreading",
            "concurrency", "io", "nio", "socket", "reflection", "spring-boot",
            "spring", "hibernate", "jdbc", "maven", "gradle", "junit",
            "exception", "annotation", "interface", "thread", "executor",
            "synchronized", "volatile", "atomic"
    );

    public TopicTrendService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /**
     * 获取所有主题的趋势
     */
    public List<TopicTrendDTO> getAllTopicTrends(int years) {
        List<Question> allQuestions = questionRepository.findAll();

        return JAVA_TOPICS.stream()
                .map(topic -> calculateTopicTrend(topic, allQuestions, years))
                .filter(dto -> dto.getTotalQuestions() > 0)
                .sorted((a, b) -> Long.compare(b.getTotalQuestions(), a.getTotalQuestions()))
                .collect(Collectors.toList());
    }

    /**
     * 获取单个主题的趋势
     */
    public TopicTrendDTO getTopicTrend(String topic, int years) {
        List<Question> allQuestions = questionRepository.findAll();
        return calculateTopicTrend(topic, allQuestions, years);
    }

    /**
     * 计算主题趋势
     */
    private TopicTrendDTO calculateTopicTrend(String topic, List<Question> allQuestions, int years) {
        // 筛选包含该主题标签的问题
        List<Question> topicQuestions = allQuestions.stream()
                .filter(q -> q.getTags() != null &&
                        q.getTags().stream().anyMatch(tag ->
                                tag.toLowerCase().contains(topic.toLowerCase())))
                .collect(Collectors.toList());

        TopicTrendDTO dto = new TopicTrendDTO();
        dto.setTopic(topic);
        dto.setTotalQuestions((long) topicQuestions.size());

        // 按月统计问题数量
        Map<String, Long> monthlyCount = new TreeMap<>();
        Map<String, Long> monthlyAnswerCount = new TreeMap<>();

        for (Question q : topicQuestions) {
            String month = parseToMonth(q.getCreationDate());
            if (month != null) {
                monthlyCount.merge(month, 1L, Long::sum);

                int answerCount = q.getAnswerCount() != null ? q.getAnswerCount() : 0;
                monthlyAnswerCount.merge(month, (long) answerCount, Long::sum);
            }
        }

        dto.setMonthlyQuestionCount(monthlyCount);
        dto.setMonthlyAnswerCount(monthlyAnswerCount);

        // 计算趋势（简单线性回归斜率）
        dto.setOverallTrend(calculateTrendSlope(monthlyCount));

        return dto;
    }

    /**
     * 将时间戳转换为月份字符串
     */
    private String parseToMonth(String creationDate) {
        try {
            long timestamp = Long.parseLong(creationDate);
            LocalDate date = Instant.ofEpochSecond(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            return date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 计算趋势斜率
     */
    private Double calculateTrendSlope(Map<String, Long> monthlyData) {
        if (monthlyData.size() < 2) return 0.0;

        List<Long> values = new ArrayList<>(monthlyData.values());
        int n = values.size();

        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += values.get(i);
            sumXY += i * values.get(i);
            sumX2 += i * i;
        }

        double denominator = n * sumX2 - sumX * sumX;
        if (denominator == 0) return 0.0;

        double slope = (n * sumXY - sumX * sumY) / denominator;
        return Math.round(slope * 100.0) / 100.0;
    }

    /**
     * 获取可用的主题列表
     */
    public List<String> getAvailableTopics() {
        return JAVA_TOPICS;
    }
}