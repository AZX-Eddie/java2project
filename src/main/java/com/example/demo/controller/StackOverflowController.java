package com.example.demo.controller;

import com.example.demo.service.StackOverflowService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StackOverflowController {

    private final StackOverflowService service;

    public StackOverflowController(StackOverflowService service) {
        this.service = service;
    }

    @GetMapping("/fetch")
    public String fetch() {
        service.fetchQuestions();
        return "数据采集完成！";
    }
}
