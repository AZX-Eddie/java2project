package com.example.demo.controller;

import com.example.demo.model.dto.*;
import com.example.demo.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AnalysisController {

    private final TopicTrendService topicTrendService;
    private final CoOccurrenceService coOccurrenceService;
    private final MultithreadingAnalysisService multithreadingService;
    private final QuestionSolvabilityService solvabilityService;

    public AnalysisController(TopicTrendService topicTrendService,
                              CoOccurrenceService coOccurrenceService,
                              MultithreadingAnalysisService multithreadingService,
                              QuestionSolvabilityService solvabilityService) {
        this.topicTrendService = topicTrendService;
        this.coOccurrenceService = coOccurrenceService;
        this.multithreadingService = multithreadingService;
        this.solvabilityService = solvabilityService;
    }

    // ==================== 问题1: 主题趋势 ====================

    /**
     * 获取所有主题趋势
     * GET http://localhost:8080/api/trends?years=3
     */
    @GetMapping("/trends")
    public ResponseEntity<Map<String, Object>> getTopicTrends(
            @RequestParam(defaultValue = "3") int years) {

        List<TopicTrendDTO> trends = topicTrendService.getAllTopicTrends(years);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("years", years);
        response.put("count", trends.size());
        response.put("data", trends);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取单个主题趋势
     * GET http://localhost:8080/api/trends/lambda?years=3
     */
    @GetMapping("/trends/{topic}")
    public ResponseEntity<Map<String, Object>> getTopicTrend(
            @PathVariable String topic,
            @RequestParam(defaultValue = "3") int years) {

        TopicTrendDTO trend = topicTrendService.getTopicTrend(topic, years);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", trend);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取可用主题列表
     * GET http://localhost:8080/api/topics
     */
    @GetMapping("/topics")
    public ResponseEntity<List<String>> getAvailableTopics() {
        return ResponseEntity.ok(topicTrendService.getAvailableTopics());
    }

    // ==================== 问题2: 主题共现 ====================

    /**
     * 获取Top N共现标签对
     * GET http://localhost:8080/api/cooccurrence?topN=10
     */
    @GetMapping("/cooccurrence")
    public ResponseEntity<Map<String, Object>> getCoOccurrences(
            @RequestParam(defaultValue = "10") int topN) {

        List<CoOccurrenceDTO> coOccurrences = coOccurrenceService.getTopCoOccurrences(topN);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("topN", topN);
        response.put("count", coOccurrences.size());
        response.put("data", coOccurrences);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取指定标签的共现标签
     * GET http://localhost:8080/api/cooccurrence/spring-boot?topN=10
     */
    @GetMapping("/cooccurrence/{tag}")
    public ResponseEntity<Map<String, Object>> getCoOccurrencesForTag(
            @PathVariable String tag,
            @RequestParam(defaultValue = "10") int topN) {

        List<CoOccurrenceDTO> coOccurrences = coOccurrenceService.getCoOccurrencesForTag(tag, topN);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("tag", tag);
        response.put("data", coOccurrences);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取共现矩阵
     * GET http://localhost:8080/api/cooccurrence/matrix?topics=spring,hibernate,jdbc
     */
    @GetMapping("/cooccurrence/matrix")
    public ResponseEntity<Map<String, Object>> getCoOccurrenceMatrix(
            @RequestParam List<String> topics) {

        Map<String, Object> matrix = coOccurrenceService.getCoOccurrenceMatrix(topics);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", matrix);

        return ResponseEntity.ok(response);
    }

    // ==================== 问题3: 多线程陷阱 ====================

    /**
     * 获取Top N多线程陷阱
     * GET http://localhost:8080/api/multithreading/pitfalls?topN=10
     */
    @GetMapping("/multithreading/pitfalls")
    public ResponseEntity<Map<String, Object>> getMultithreadingPitfalls(
            @RequestParam(defaultValue = "10") int topN) {

        List<MultithreadingPitfallDTO> pitfalls = multithreadingService.getTopPitfalls(topN);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("topN", topN);
        response.put("count", pitfalls.size());
        response.put("data", pitfalls);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取多线程异常分布
     * GET http://localhost:8080/api/multithreading/exceptions
     */
    @GetMapping("/multithreading/exceptions")
    public ResponseEntity<Map<String, Object>> getExceptionDistribution() {

        Map<String, Long> exceptions = multithreadingService.getExceptionDistribution();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", exceptions);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取多线程问题统计
     * GET http://localhost:8080/api/multithreading/stats
     */
    @GetMapping("/multithreading/stats")
    public ResponseEntity<Map<String, Object>> getMultithreadingStats() {

        Map<String, Object> stats = multithreadingService.getMultithreadingStats();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", stats);

        return ResponseEntity.ok(response);
    }

    // ==================== 问题4: 可解决 vs 难解决 ====================

    /**
     * 获取问题特征对比
     * GET http://localhost:8080/api/solvability/compare
     */
    @GetMapping("/solvability/compare")
    public ResponseEntity<Map<String, Object>> compareQuestionCharacteristics() {

        Map<String, QuestionCharacteristicDTO> comparison = solvabilityService.compareCharacteristics();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", comparison);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取因素对比数据
     * GET http://localhost:8080/api/solvability/factors
     */
    @GetMapping("/solvability/factors")
    public ResponseEntity<Map<String, Object>> getFactorComparison() {

        Map<String, Object> factors = solvabilityService.getFactorComparison();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", factors);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取时间分布对比
     * GET http://localhost:8080/api/solvability/timing
     */
    @GetMapping("/solvability/timing")
    public ResponseEntity<Map<String, Object>> getTimingAnalysis() {

        Map<String, Object> timing = solvabilityService.getTimingAnalysis();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", timing);

        return ResponseEntity.ok(response);
    }
}