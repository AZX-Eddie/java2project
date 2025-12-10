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

        // 根据时间范围过滤问题
        LocalDate cutoffDate = LocalDate.now().minusYears(years);
        List<Question> filteredQuestions = allQuestions.stream()
                .filter(q -> isWithinTimeRange(q.getCreationDate(), cutoffDate))
                .collect(Collectors.toList());

        System.out.println("时间范围: " + years + " 年，筛选后问题数: " + filteredQuestions.size());

        return JAVA_TOPICS.stream()
                .map(topic -> calculateTopicTrend(topic, filteredQuestions))
                .filter(dto -> dto.getTotalQuestions() > 0)
                .sorted((a, b) -> Long.compare(b.getTotalQuestions(), a.getTotalQuestions()))
                .collect(Collectors.toList());
    }

    /**
     * 获取单个主题的趋势
     */
    public TopicTrendDTO getTopicTrend(String topic, int years) {
        List<Question> allQuestions = questionRepository.findAll();

        // 根据时间范围过滤问题
        LocalDate cutoffDate = LocalDate.now().minusYears(years);
        List<Question> filteredQuestions = allQuestions.stream()
                .filter(q -> isWithinTimeRange(q.getCreationDate(), cutoffDate))
                .collect(Collectors.toList());

        return calculateTopicTrend(topic, filteredQuestions);
    }

    /**
     * 判断问题是否在时间范围内
     */
    private boolean isWithinTimeRange(String creationDate, LocalDate cutoffDate) {
        if (creationDate == null) return false;

        try {
            long timestamp = Long.parseLong(creationDate);
            LocalDate questionDate = Instant.ofEpochSecond(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            return !questionDate.isBefore(cutoffDate);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 计算主题趋势
     */
    private TopicTrendDTO calculateTopicTrend(String topic, List<Question> questions) {
        // 筛选包含该主题标签的问题
        List<Question> topicQuestions = questions.stream()
                .filter(q -> q.getTags() != null &&
                        q.getTags().stream().anyMatch(tag ->
                                tag.toLowerCase().contains(topic.toLowerCase())))
                .collect(Collectors.toList());

        TopicTrendDTO dto = new TopicTrendDTO();
        dto.setTopic(topic);
        dto.setTotalQuestions((long) topicQuestions.size());

        // 按月统计
        Map<String, Long> monthlyCount = new TreeMap<>();
        Map<String, Long> monthlyAnswerCount = new TreeMap<>();
        Map<String, List<Integer>> monthlyScores = new TreeMap<>();  // 🔥 用于计算月均分

        // 累计统计指标
        long totalScore = 0;
        long totalViewCount = 0;
        long totalAnswers = 0;
        long acceptedCount = 0;

        for (Question q : topicQuestions) {
            String month = parseToMonth(q.getCreationDate());
            if (month != null) {
                monthlyCount.merge(month, 1L, Long::sum);

                int answerCount = q.getAnswerCount() != null ? q.getAnswerCount() : 0;
                monthlyAnswerCount.merge(month, (long) answerCount, Long::sum);

                // 收集分数用于计算月均
                int score = q.getScore() != null ? q.getScore() : 0;
                monthlyScores.computeIfAbsent(month, k -> new ArrayList<>()).add(score);
            }

            // 累计统计
            totalScore += q.getScore() != null ? q.getScore() : 0;
            totalViewCount += q.getViewCount() != null ? q.getViewCount() : 0;
            totalAnswers += q.getAnswerCount() != null ? q.getAnswerCount() : 0;
            if (q.getAcceptedAnswerId() != null) {
                acceptedCount++;
            }
        }

        dto.setMonthlyQuestionCount(monthlyCount);
        dto.setMonthlyAnswerCount(monthlyAnswerCount);
        dto.setOverallTrend(calculateTrendSlope(monthlyCount));

        // 设置指标
        int totalQ = topicQuestions.size();
        dto.setAvgScore(totalQ > 0 ? Math.round(totalScore * 100.0 / totalQ) / 100.0 : 0.0);
        dto.setAvgViewCount(totalQ > 0 ? Math.round(totalViewCount * 100.0 / totalQ) / 100.0 : 0.0);
        dto.setAcceptedAnswerRate(totalQ > 0 ? Math.round(acceptedCount * 10000.0 / totalQ) / 100.0 : 0.0);
        dto.setTotalAnswers(totalAnswers);

        // 计算每月平均分数
        Map<String, Double> monthlyAvgScore = new TreeMap<>();
        for (Map.Entry<String, List<Integer>> entry : monthlyScores.entrySet()) {
            List<Integer> scores = entry.getValue();
            double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            monthlyAvgScore.put(entry.getKey(), Math.round(avg * 100.0) / 100.0);
        }
        dto.setMonthlyAvgScore(monthlyAvgScore);

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