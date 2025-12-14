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
     * 🔥 新增：按月收集固定数量的问题（推荐使用）
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
     * 原有方法：收集Java问题数据（按页）
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
     * 批量收集评论（更高效）
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
     * 🔥 新增：获取收集建议
     * GET http://localhost:8080/admin/collect-suggestions
     */
    @GetMapping("/collect-suggestions")
    public Map<String, Object> getCollectionSuggestions() {
        Map<String, Object> response = new HashMap<>();

        Map<String, Object> suggestions = new HashMap<>();

        // 快速测试方案
        Map<String, Object> quickTest = new HashMap<>();
        quickTest.put("description", "快速测试（3个月，每月50条）");
        quickTest.put("url", "/admin/collect-by-month?months=3&perMonth=50");
        quickTest.put("estimatedTime", "5-10分钟");
        quickTest.put("totalQuestions", 150);
        suggestions.put("quickTest", quickTest);

        // 标准方案（推荐）
        Map<String, Object> standard = new HashMap<>();
        standard.put("description", "标准收集（12个月，每月100条）⭐推荐");
        standard.put("url", "/admin/collect-by-month?months=12&perMonth=100");
        standard.put("estimatedTime", "30-45分钟");
        standard.put("totalQuestions", 1200);
        suggestions.put("standard", standard);

        // 全面方案
        Map<String, Object> comprehensive = new HashMap<>();
        comprehensive.put("description", "全面收集（24个月，每月150条）");
        comprehensive.put("url", "/admin/collect-by-month?months=24&perMonth=150");
        comprehensive.put("estimatedTime", "1-1.5小时");
        comprehensive.put("totalQuestions", 3600);
        suggestions.put("comprehensive", comprehensive);

        // 深度方案
        Map<String, Object> extended = new HashMap<>();
        extended.put("description", "深度收集（36个月，每月200条）");
        extended.put("url", "/admin/collect-by-month?months=36&perMonth=200");
        extended.put("estimatedTime", "2-3小时");
        extended.put("totalQuestions", 7200);
        suggestions.put("extended", extended);

        response.put("suggestions", suggestions);
        response.put("notes", java.util.List.of(
                "推荐使用按月收集，数据更均衡",
                "收集时间受网络和API限流影响",
                "建议在非高峰时段进行收集",
                "收集过程会自动重试失败的请求"
        ));

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