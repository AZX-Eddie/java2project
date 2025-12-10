package com.example.demo.service;

import com.example.demo.model.Question;
import com.example.demo.model.dto.CoOccurrenceDTO;
import com.example.demo.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CoOccurrenceService {

    private final QuestionRepository questionRepository;

    public CoOccurrenceService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /**
     * 获取Top N共现标签对
     */
    public List<CoOccurrenceDTO> getTopCoOccurrences(int topN) {
        List<Question> questions = questionRepository.findAll();
        long totalQuestions = questions.size();

        // 统计标签对出现频率
        Map<String, Long> pairCounts = new HashMap<>();

        for (Question question : questions) {
            Set<String> tags = question.getTags();
            if (tags == null || tags.size() < 2) continue;

            // 过滤掉java标签，获取更有意义的组合
            List<String> filteredTags = tags.stream()
                    .filter(tag -> !tag.equalsIgnoreCase("java"))
                    .sorted()
                    .collect(Collectors.toList());

            // 生成所有标签对
            for (int i = 0; i < filteredTags.size(); i++) {
                for (int j = i + 1; j < filteredTags.size(); j++) {
                    String pair = filteredTags.get(i) + " & " + filteredTags.get(j);
                    pairCounts.merge(pair, 1L, Long::sum);
                }
            }
        }

        // 排序并取Top N
        return pairCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .map(entry -> {
                    String[] parts = entry.getKey().split(" & ");
                    double percentage = (entry.getValue() * 100.0) / totalQuestions;
                    return new CoOccurrenceDTO(
                            parts[0],
                            parts[1],
                            entry.getValue(),
                            Math.round(percentage * 100.0) / 100.0
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取指定标签的共现标签
     */
    public List<CoOccurrenceDTO> getCoOccurrencesForTag(String tag, int topN) {
        List<Question> questions = questionRepository.findAll();

        // 统计与指定标签共现的其他标签
        Map<String, Long> coOccurrences = new HashMap<>();
        long tagCount = 0;

        for (Question question : questions) {
            Set<String> tags = question.getTags();
            if (tags == null || !tags.contains(tag)) continue;

            tagCount++;
            for (String t : tags) {
                if (!t.equals(tag) && !t.equalsIgnoreCase("java")) {
                    coOccurrences.merge(t, 1L, Long::sum);
                }
            }
        }

        final long finalTagCount = tagCount;

        return coOccurrences.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .map(entry -> {
                    double percentage = (entry.getValue() * 100.0) / finalTagCount;
                    return new CoOccurrenceDTO(
                            tag,
                            entry.getKey(),
                            entry.getValue(),
                            Math.round(percentage * 100.0) / 100.0
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取共现矩阵数据
     */
    public Map<String, Object> getCoOccurrenceMatrix(List<String> topics) {
        List<Question> questions = questionRepository.findAll();

        int size = topics.size();
        int[][] matrix = new int[size][size];

        for (Question question : questions) {
            Set<String> tags = question.getTags();
            if (tags == null) continue;

            for (int i = 0; i < size; i++) {
                for (int j = i; j < size; j++) {
                    int finalI = i;
                    boolean hasTopic1 = tags.stream()
                            .anyMatch(t -> t.toLowerCase().contains(topics.get(finalI).toLowerCase()));
                    int finalJ = j;
                    boolean hasTopic2 = tags.stream()
                            .anyMatch(t -> t.toLowerCase().contains(topics.get(finalJ).toLowerCase()));

                    if (hasTopic1 && hasTopic2) {
                        matrix[i][j]++;
                        if (i != j) matrix[j][i]++;
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("topics", topics);
        result.put("matrix", matrix);
        return result;
    }
}