package com.example.demo.service;

import com.example.demo.model.Question;
import com.example.demo.model.dto.TopicTrendDTO;
import com.example.demo.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TopicTrendService {
    private final QuestionRepository questionRepository;

    // 热度计算权重配置
    private static final double WEIGHT_QUESTION_COUNT = 0.3;
    private static final double WEIGHT_AVG_SCORE = 0.25;
    private static final double WEIGHT_AVG_VIEWS = 0.20;
    private static final double WEIGHT_ANSWER_RATE = 0.15;
    private static final double WEIGHT_ACCEPTED_RATE = 0.10;

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
     *  获取当前未完成的月份（用于过滤）
     */
    private String getCurrentIncompleteMonth() {
        return YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    /**
     * 判断是否应该排除某个月份
     */
    private boolean shouldExcludeMonth(String month) {
        if (month == null) return true;

        try {
            YearMonth monthToCheck = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
            YearMonth currentMonth = YearMonth.now();

            // 排除当前月份（未完成）
            return monthToCheck.equals(currentMonth) || monthToCheck.isAfter(currentMonth);
        } catch (Exception e) {
            return true;
        }
    }

    /**
     *  生成完整的月份列表（从 cutoffDate 到上个月）
     */
    private List<String> generateCompleteMonthRange(int years) {
        List<String> months = new ArrayList<>();

        YearMonth current = YearMonth.now().minusMonths(1); // 从上个月开始（排除当前未完成月份）
        YearMonth start = YearMonth.now().minusYears(years);

        while (!start.isAfter(current)) {
            months.add(start.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            start = start.plusMonths(1);
        }

        return months;
    }

    /**
     *  修改后的方法：获取所有主题的趋势（包含完整时间范围）
     */
    public List<TopicTrendDTO> getAllTopicTrendsWithHeatIndex(int years) {
        List<Question> allQuestions = questionRepository.findAll();

        LocalDate cutoffDate = LocalDate.now().minusYears(years);

        //  生成完整的月份列表
        List<String> completeMonths = generateCompleteMonthRange(years);
        System.out.println("📅 完整月份范围: " + completeMonths.get(0) + " 至 " + completeMonths.get(completeMonths.size() - 1));
        System.out.println("📅 总月份数: " + completeMonths.size());

        // 过滤问题
        List<Question> filteredQuestions = allQuestions.stream()
                .filter(q -> {
                    if (!isWithinTimeRange(q.getCreationDate(), cutoffDate)) {
                        return false;
                    }
                    String month = parseToMonth(q.getCreationDate());
                    return !shouldExcludeMonth(month);
                })
                .collect(Collectors.toList());

        System.out.println("📊 时间范围: " + years + " 年");
        System.out.println("✅ 筛选后问题数: " + filteredQuestions.size());

        // 计算全局统计
        GlobalStats globalStats = calculateGlobalStats(filteredQuestions);

        //  传入完整月份列表
        return JAVA_TOPICS.stream()
                .map(topic -> calculateTopicTrendWithHeat(topic, filteredQuestions, globalStats, completeMonths))
                .filter(dto -> dto.getTotalQuestions() > 0)
                .sorted((a, b) -> Double.compare(b.getOverallHeatIndex(), a.getOverallHeatIndex()))
                .collect(Collectors.toList());
    }

    /**
     *  修改后：计算单个主题的趋势（填充完整月份）
     */
    private TopicTrendDTO calculateTopicTrendWithHeat(String topic,
                                                      List<Question> allQuestions,
                                                      GlobalStats globalStats,
                                                      List<String> completeMonths) {
        // 筛选该主题的问题
        List<Question> topicQuestions = allQuestions.stream()
                .filter(q -> q.getTags() != null &&
                        q.getTags().stream().anyMatch(tag ->
                                tag.toLowerCase().contains(topic.toLowerCase())))
                .collect(Collectors.toList());

        TopicTrendDTO dto = new TopicTrendDTO();
        dto.setTopic(topic);
        dto.setTotalQuestions((long) topicQuestions.size());

        // 按月分组统计（只有有数据的月份）
        Map<String, MonthlyMetrics> monthlyMetricsMap = calculateMonthlyMetrics(topicQuestions);

        //  使用完整月份列表，填充缺失月份为0
        Map<String, Double> monthlyHeatIndex = new LinkedHashMap<>();  // 保持插入顺序
        Map<String, Long> monthlyQuestionCount = new LinkedHashMap<>();
        Map<String, Long> monthlyAnswerCount = new LinkedHashMap<>();
        Map<String, Double> monthlyAvgScore = new LinkedHashMap<>();

        for (String month : completeMonths) {
            MonthlyMetrics metrics = monthlyMetricsMap.get(month);

            if (metrics != null) {
                // 有数据的月份
                double heatIndex = calculateHeatIndex(metrics, globalStats);
                monthlyHeatIndex.put(month, Math.round(heatIndex * 100.0) / 100.0);
                monthlyQuestionCount.put(month, metrics.questionCount);
                monthlyAnswerCount.put(month, metrics.totalAnswers);
                monthlyAvgScore.put(month, Math.round(metrics.avgScore * 100.0) / 100.0);
            } else {
                //  没有数据的月份，填充0
                monthlyHeatIndex.put(month, 0.0);
                monthlyQuestionCount.put(month, 0L);
                monthlyAnswerCount.put(month, 0L);
                monthlyAvgScore.put(month, 0.0);
            }
        }

        dto.setMonthlyHeatIndex(monthlyHeatIndex);
        dto.setMonthlyQuestionCount(monthlyQuestionCount);
        dto.setMonthlyAnswerCount(monthlyAnswerCount);
        dto.setMonthlyAvgScore(monthlyAvgScore);

        // 计算趋势
        dto.setOverallTrend(calculateTrendSlope(monthlyHeatIndex));

        // 计算整体热度
        double overallHeat = calculateRecentAverageHeat(monthlyHeatIndex, 3);
        dto.setOverallHeatIndex(Math.round(overallHeat * 100.0) / 100.0);

        // 设置累计统计（保持不变）
        int totalQ = topicQuestions.size();
        long totalScore = topicQuestions.stream().mapToInt(q -> q.getScore() != null ? q.getScore() : 0).sum();
        long totalViews = topicQuestions.stream().mapToInt(q -> q.getViewCount() != null ? q.getViewCount() : 0).sum();
        long totalAnswers = topicQuestions.stream().mapToInt(q -> q.getAnswerCount() != null ? q.getAnswerCount() : 0).sum();
        long acceptedCount = topicQuestions.stream().filter(q -> q.getAcceptedAnswerId() != null).count();

        dto.setAvgScore(totalQ > 0 ? Math.round(totalScore * 100.0 / totalQ) / 100.0 : 0.0);
        dto.setAvgViewCount(totalQ > 0 ? Math.round(totalViews * 100.0 / totalQ) / 100.0 : 0.0);
        dto.setAcceptedAnswerRate(totalQ > 0 ? Math.round(acceptedCount * 10000.0 / totalQ) / 100.0 : 0.0);
        dto.setTotalAnswers(totalAnswers);

        return dto;
    }

    /**
     * 计算每月指标（ 自动过滤当前未完成月份）
     */
    private Map<String, MonthlyMetrics> calculateMonthlyMetrics(List<Question> questions) {
        Map<String, MonthlyMetrics> metricsMap = new TreeMap<>();

        for (Question q : questions) {
            String month = parseToMonth(q.getCreationDate());

            //  跳过当前未完成月份
            if (shouldExcludeMonth(month)) {
                continue;
            }

            MonthlyMetrics metrics = metricsMap.computeIfAbsent(month, k -> new MonthlyMetrics());

            metrics.questionCount++;
            metrics.totalScore += q.getScore() != null ? q.getScore() : 0;
            metrics.totalViews += q.getViewCount() != null ? q.getViewCount() : 0;
            metrics.totalAnswers += q.getAnswerCount() != null ? q.getAnswerCount() : 0;

            if (q.getAnswerCount() != null && q.getAnswerCount() > 0) {
                metrics.answeredCount++;
            }

            if (q.getAcceptedAnswerId() != null) {
                metrics.acceptedCount++;
            }
        }

        // 计算平均值
        for (MonthlyMetrics metrics : metricsMap.values()) {
            if (metrics.questionCount > 0) {
                metrics.avgScore = (double) metrics.totalScore / metrics.questionCount;
                metrics.avgViews = (double) metrics.totalViews / metrics.questionCount;
                metrics.answerRate = (double) metrics.answeredCount / metrics.questionCount;
                metrics.acceptedRate = (double) metrics.acceptedCount / metrics.questionCount;
            }
        }

        return metricsMap;
    }

    /**
     * 计算热度指数（0-100）
     */
    private double calculateHeatIndex(MonthlyMetrics metrics, GlobalStats globalStats) {
        // 归一化各项指标（0-1之间）
        double normQuestionCount = normalizeValue(metrics.questionCount, 0, globalStats.maxMonthlyQuestions);
        double normAvgScore = normalizeValue(metrics.avgScore, 0, globalStats.maxAvgScore);
        double normAvgViews = normalizeValue(metrics.avgViews, 0, globalStats.maxAvgViews);
        double normAnswerRate = metrics.answerRate; // 已经是0-1
        double normAcceptedRate = metrics.acceptedRate; // 已经是0-1

        // 加权计算热度指数
        double heatIndex =
                normQuestionCount * WEIGHT_QUESTION_COUNT +
                        normAvgScore * WEIGHT_AVG_SCORE +
                        normAvgViews * WEIGHT_AVG_VIEWS +
                        normAnswerRate * WEIGHT_ANSWER_RATE +
                        normAcceptedRate * WEIGHT_ACCEPTED_RATE;

        return heatIndex * 100; // 转换为0-100范围
    }

    /**
     * 归一化数值到0-1范围
     */
    private double normalizeValue(double value, double min, double max) {
        if (max == min) return 0;
        return Math.min(1.0, Math.max(0.0, (value - min) / (max - min)));
    }

    /**
     * 计算全局统计（用于归一化） 排除当前未完成月份
     */
    private GlobalStats calculateGlobalStats(List<Question> questions) {
        GlobalStats stats = new GlobalStats();

        Map<String, Long> monthlyCount = new HashMap<>();
        Map<String, List<Integer>> monthlyScores = new HashMap<>();
        Map<String, List<Integer>> monthlyViews = new HashMap<>();

        for (Question q : questions) {
            String month = parseToMonth(q.getCreationDate());

            //  跳过当前未完成月份
            if (shouldExcludeMonth(month)) {
                continue;
            }

            monthlyCount.merge(month, 1L, Long::sum);

            monthlyScores.computeIfAbsent(month, k -> new ArrayList<>())
                    .add(q.getScore() != null ? q.getScore() : 0);

            monthlyViews.computeIfAbsent(month, k -> new ArrayList<>())
                    .add(q.getViewCount() != null ? q.getViewCount() : 0);
        }

        stats.maxMonthlyQuestions = monthlyCount.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(1);

        stats.maxAvgScore = monthlyScores.values().stream()
                .mapToDouble(list -> list.stream().mapToInt(Integer::intValue).average().orElse(0))
                .max()
                .orElse(1.0);

        stats.maxAvgViews = monthlyViews.values().stream()
                .mapToDouble(list -> list.stream().mapToInt(Integer::intValue).average().orElse(0))
                .max()
                .orElse(1.0);

        return stats;
    }

    /**
     * 计算最近N个月的平均热度
     */
    private double calculateRecentAverageHeat(Map<String, Double> monthlyHeat, int recentMonths) {
        if (monthlyHeat.isEmpty()) return 0.0;

        List<Double> values = new ArrayList<>(monthlyHeat.values());
        int start = Math.max(0, values.size() - recentMonths);

        return values.subList(start, values.size()).stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    /**
     * 计算趋势斜率（基于热度或任何指标）
     */
    private Double calculateTrendSlope(Map<String, ? extends Number> monthlyData) {
        if (monthlyData.size() < 2) return 0.0;

        List<Double> values = monthlyData.values().stream()
                .map(Number::doubleValue)
                .collect(Collectors.toList());

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
     * 获取可用的主题列表
     */
    public List<String> getAvailableTopics() {
        return JAVA_TOPICS;
    }

    /**
     * 保留原有方法：获取所有主题的趋势（兼容性）
     */
    public List<TopicTrendDTO> getAllTopicTrends(int years) {
        return getAllTopicTrendsWithHeatIndex(years);
    }

    /**
     *  同样修改单个主题的方法
     */
    public TopicTrendDTO getTopicTrend(String topic, int years) {
        List<Question> allQuestions = questionRepository.findAll();
        LocalDate cutoffDate = LocalDate.now().minusYears(years);

        //  生成完整月份列表
        List<String> completeMonths = generateCompleteMonthRange(years);

        List<Question> filteredQuestions = allQuestions.stream()
                .filter(q -> {
                    if (!isWithinTimeRange(q.getCreationDate(), cutoffDate)) {
                        return false;
                    }
                    String month = parseToMonth(q.getCreationDate());
                    return !shouldExcludeMonth(month);
                })
                .collect(Collectors.toList());

        GlobalStats globalStats = calculateGlobalStats(filteredQuestions);

        //  传入完整月份列表
        return calculateTopicTrendWithHeat(topic, filteredQuestions, globalStats, completeMonths);
    }

    // ==================== 内部类 ====================

    /**
     * 月度指标
     */
    private static class MonthlyMetrics {
        long questionCount = 0;
        long totalScore = 0;
        long totalViews = 0;
        long totalAnswers = 0;
        long answeredCount = 0;
        long acceptedCount = 0;

        double avgScore = 0.0;
        double avgViews = 0.0;
        double answerRate = 0.0;
        double acceptedRate = 0.0;
    }

    /**
     * 全局统计
     */
    private static class GlobalStats {
        long maxMonthlyQuestions = 1;
        double maxAvgScore = 1.0;
        double maxAvgViews = 1.0;
    }
}