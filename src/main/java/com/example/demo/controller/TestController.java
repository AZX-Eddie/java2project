package com.example.demo.controller;

import com.example.demo.model.Question;
import com.example.demo.repository.QuestionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    private final QuestionRepository repo;

    public TestController(QuestionRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/test-insert")
    public String testInsert() {
        Question q = new Question();
        q.setQuestionId(123456L);
        q.setTitle("Hello StackOverflow");
        q.setScore(10);
        q.setCreationDate(String.valueOf(System.currentTimeMillis())); // ← 转成 String

        repo.save(q);

        return "Inserted test data!";
    }
}
