package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    /**
     * 首页
     * GET http://localhost:8080/
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }
}