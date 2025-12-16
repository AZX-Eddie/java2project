package com.example.demo.controller;

import com.example.demo.service.DataCollectionService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class DataCollectionController {

    private final DataCollectionService dataCollectionService;

    public DataCollectionController(DataCollectionService dataCollectionService) {
        this.dataCollectionService = dataCollectionService;
    }

    /**
     * 按月收集固定数量的问题（推荐使用）
     * GET http://localhost:8080/admin/collect-by-month?months=12&perMonth=100
     */
    @GetMapping("/collect-by-month")
    public Map<String, Object> collectQuestionsByMonth(
            @RequestParam(defaultValue = "12") int months,
            @RequestParam(defaultValue = "100") int perMonth) {

        Map<String, Object> response = new HashMap<>();
        response.put("status", "started");
        response.put("months", months);
        response.put("questionsPerMonth", perMonth);
        response.put("totalTarget", months * perMonth);
        response.put("message", "正在按月收集数据：" + months + " 个月，每月 " + perMonth + " 条");

        // 异步执行收集任务
        new Thread(() -> {
            String result = dataCollectionService.fetchQuestionsByMonth(months, perMonth);
            System.out.println(result);
        }).start();

        return response;
    }

    /**
     * 收集Java问题数据（按页）
     * GET http://localhost:8080/admin/collect?pages=15
     */
    @GetMapping("/collect")
    public Map<String, Object> collectQuestions(@RequestParam(defaultValue = "10") int pages) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "started");
        response.put("pages", pages);
        response.put("message", "正在收集 " + pages + " 页数据（每页100条）...");

        new Thread(() -> {
            String result = dataCollectionService.fetchJavaQuestions(pages);
            System.out.println(result);
        }).start();

        return response;
    }

    /**
     * 收集答案数据
     * GET http://localhost:8080/admin/collect-answers
     */
    @GetMapping("/collect-answers")
    public Map<String, Object> collectAnswers() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "started");
        response.put("message", "正在收集答案数据...");

        new Thread(() -> {
            String result = dataCollectionService.fetchAnswersForQuestions();
            System.out.println(result);
        }).start();

        return response;
    }

    /**
     * 收集评论数据
     * GET http://localhost:8080/admin/collect-comments
     */
    @GetMapping("/collect-comments")
    public Map<String, Object> collectComments() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "started");
        response.put("message", "正在收集评论数据...");

        new Thread(() -> {
            String result = dataCollectionService.fetchCommentsForQuestions();
            System.out.println(result);
        }).start();

        return response;
    }

    /**
     * 批量收集评论
     * GET http://localhost:8080/admin/collect-comments-batch
     */
    @GetMapping("/collect-comments-batch")
    public Map<String, Object> collectCommentsBatch() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "started");
        response.put("message", "正在批量收集评论数据...");

        new Thread(() -> {
            String result = dataCollectionService.fetchCommentsBatch();
            System.out.println(result);
        }).start();

        return response;
    }

    /**
     * 查看数据统计
     * GET http://localhost:8080/admin/stats
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", dataCollectionService.getDataStats());
        return response;
    }

    /**
     * 清空所有数据
     * GET http://localhost:8080/admin/clear-all?confirm=true
     */
    @GetMapping("/clear-all")
    public Map<String, Object> clearAllData(
            @RequestParam(defaultValue = "false") boolean confirm) {

        Map<String, Object> response = new HashMap<>();

        if (!confirm) {
            response.put("status", "warning");
            response.put("message", "请确认删除操作，添加参数 ?confirm=true");
            return response;
        }

        try {
            dataCollectionService.clearAllData();
            response.put("status", "success");
            response.put("message", "所有数据已清空");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "清空失败: " + e.getMessage());
        }

        return response;
    }
}