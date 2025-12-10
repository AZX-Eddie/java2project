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
     * 收集Java问题数据
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
     * GET http://localhost:8080/admin/clear-all
     */
    @GetMapping("/clear-all")
    public Map<String, Object> clearAllData() {
        Map<String, Object> response = new HashMap<>();

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