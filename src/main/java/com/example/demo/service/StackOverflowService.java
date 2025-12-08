package com.example.demo.service;

import com.example.demo.model.Question;
import com.example.demo.repository.QuestionRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class StackOverflowService {

    private final QuestionRepository questionRepository;

    public StackOverflowService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public void fetchQuestions() {

        String url = "https://api.stackexchange.com/2.3/questions?" +
                "order=desc&sort=activity&site=stackoverflow&filter=withbody";

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);

        JSONObject json = new JSONObject(response);
        JSONArray items = json.getJSONArray("items");

        for (int i = 0; i < items.length(); i++) {
            JSONObject q = items.getJSONObject(i);

            Question question = new Question(
                    q.getLong("question_id"),
                    q.getString("title"),
                    q.getString("body"),
                    q.getJSONObject("owner").optString("display_name", "unknown"),
                    q.optInt("score", 0),
                    q.get("creation_date").toString()
            );

            questionRepository.save(question);
        }

        System.out.println("保存成功！共保存：" + items.length() + " 条记录");
    }
}
