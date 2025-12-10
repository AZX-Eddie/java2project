package com.example.demo.service;

import com.example.demo.model.Question;
import com.example.demo.model.dto.QuestionCharacteristicDTO;
import com.example.demo.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class QuestionSolvabilityService {

    private final QuestionRepository questionRepository;

    public QuestionSolvabilityService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /**
     * 比较可解决与难解决问题的特征
     */
    public Map<String, QuestionCharacteristicDTO> compareCharacteristics() {
        List<Question> allQuestions = questionRepository.findAll();

        // 可解决问题：有被接受的答案 或 有高分答案
        List<Question> solvable = allQuestions.stream()
                .filter(q -> q.getAcceptedAnswerId() != null ||
                        (q.getIsAnswered() != null && q.getIsAnswered() && q.getScore() >= 1))
                .collect(Collectors.toList());

        // 难解决问题：没有被接受的答案 且 没有答案或低参与度
        List<Question> hardToSolve = allQuestions.stream()
                .filter(q -> q.getAcceptedAnswerId() == null &&
                        (q.getAnswerCount() == null || q.getAnswerCount() == 0 ||
                                (q.getIsAnswered() == null || !q.getIsAnswered())))
                .collect(Collectors.toList());

        Map<String, QuestionCharacteristicDTO> result = new HashMap<>();
        result.put("solvable", analyzeCharacteristics("solvable", solvable));
        result.put("hardToSolve", analyzeCharacteristics("hard-to-solve", hardToSolve));

        return result;
    }

    /**
     * 分析问题特征
     */
    private QuestionCharacteristicDTO analyzeCharacteristics(String category, List<Question> questions) {
        QuestionCharacteristicDTO dto = new QuestionCharacteristicDTO();
        dto.setCategory(category);
        dto.setQuestionCount((long) questions.size());

        if (questions.isEmpty()) return dto;

        // 因素1: 标题长度
        double avgTitleLength = questions.stream()
                .filter(q -> q.getTitle() != null)
                .mapToInt(q -> q.getTitle().length())
                .average()
                .orElse(0);
        dto.setAvgTitleLength(Math.round(avgTitleLength * 100.0) / 100.0);

        // 因素2: 正文长度
        double avgBodyLength = questions.stream()
                .filter(q -> q.getBody() != null)
                .mapToInt(q -> q.getBody().length())
                .average()
                .orElse(0);
        dto.setAvgBodyLength(Math.round(avgBodyLength * 100.0) / 100.0);

        // 因素3: 代码片段数量
        Pattern codePattern = Pattern.compile("<code>|```");
        double avgCodeCount = questions.stream()
                .filter(q -> q.getBody() != null)
                .mapToInt(q -> {
                    Matcher matcher = codePattern.matcher(q.getBody());
                    int count = 0;
                    while (matcher.find()) count++;
                    return count / 2; // 开闭标签成对
                })
                .average()
                .orElse(0);
        dto.setAvgCodeSnippetCount(Math.round(avgCodeCount * 100.0) / 100.0);

        // 因素4: 用户声望
        double avgReputation = questions.stream()
                .filter(q -> q.getOwnerReputation() != null)
                .mapToInt(Question::getOwnerReputation)
                .average()
                .orElse(0);
        dto.setAvgOwnerReputation(Math.round(avgReputation * 100.0) / 100.0);

        // 因素5: 标签数量
        double avgTagCount = questions.stream()
                .filter(q -> q.getTags() != null)
                .mapToInt(q -> q.getTags().size())
                .average()
                .orElse(0);
        dto.setAvgTagCount(Math.round(avgTagCount * 100.0) / 100.0);

        // Top标签统计
        Map<String, Long> tagCounts = questions.stream()
                .filter(q -> q.getTags() != null)
                .flatMap(q -> q.getTags().stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        Map<String, Long> topTags = tagCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
        dto.setTopTags(topTags);

        // 发布时间（小时）分布
        Map<Integer, Long> hourDistribution = questions.stream()
                .filter(q -> q.getCreationDate() != null)
                .collect(Collectors.groupingBy(
                        q -> parseToHour(q.getCreationDate()),
                        TreeMap::new,
                        Collectors.counting()
                ));
        dto.setHourDistribution(hourDistribution);

        return dto;
    }

    /**
     * 将时间戳转换为小时
     */
    private Integer parseToHour(String creationDate) {
        try {
            long timestamp = Long.parseLong(creationDate);
            return Instant.ofEpochSecond(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .getHour();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 获取因素对比数据（用于图表）
     */
    public Map<String, Object> getFactorComparison() {
        Map<String, QuestionCharacteristicDTO> characteristics = compareCharacteristics();

        QuestionCharacteristicDTO solvable = characteristics.get("solvable");
        QuestionCharacteristicDTO hardToSolve = characteristics.get("hardToSolve");

        Map<String, Object> comparison = new LinkedHashMap<>();

        // 标题长度对比
        Map<String, Double> titleLength = new LinkedHashMap<>();
        titleLength.put("solvable", solvable.getAvgTitleLength());
        titleLength.put("hardToSolve", hardToSolve.getAvgTitleLength());
        comparison.put("titleLength", titleLength);

        // 正文长度对比
        Map<String, Double> bodyLength = new LinkedHashMap<>();
        bodyLength.put("solvable", solvable.getAvgBodyLength());
        bodyLength.put("hardToSolve", hardToSolve.getAvgBodyLength());
        comparison.put("bodyLength", bodyLength);

        // 代码片段对比
        Map<String, Double> codeSnippets = new LinkedHashMap<>();
        codeSnippets.put("solvable", solvable.getAvgCodeSnippetCount());
        codeSnippets.put("hardToSolve", hardToSolve.getAvgCodeSnippetCount());
        comparison.put("codeSnippets", codeSnippets);

        // 用户声望对比
        Map<String, Double> reputation = new LinkedHashMap<>();
        reputation.put("solvable", solvable.getAvgOwnerReputation());
        reputation.put("hardToSolve", hardToSolve.getAvgOwnerReputation());
        comparison.put("userReputation", reputation);

        // 标签数量对比
        Map<String, Double> tagCount = new LinkedHashMap<>();
        tagCount.put("solvable", solvable.getAvgTagCount());
        tagCount.put("hardToSolve", hardToSolve.getAvgTagCount());
        comparison.put("tagCount", tagCount);

        // 问题数量
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("solvable", solvable.getQuestionCount());
        counts.put("hardToSolve", hardToSolve.getQuestionCount());
        comparison.put("questionCounts", counts);

        return comparison;
    }

    /**
     * 获取时间分布对比
     */
    public Map<String, Object> getTimingAnalysis() {
        Map<String, QuestionCharacteristicDTO> characteristics = compareCharacteristics();

        Map<String, Object> result = new HashMap<>();
        result.put("solvableHourDistribution", characteristics.get("solvable").getHourDistribution());
        result.put("hardToSolveHourDistribution", characteristics.get("hardToSolve").getHourDistribution());

        return result;
    }
}