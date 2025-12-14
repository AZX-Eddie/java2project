package com.example.demo.service;

import com.example.demo.model.Answer;
import com.example.demo.model.Comment;
import com.example.demo.model.Question;
import com.example.demo.repository.AnswerRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.QuestionRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DataCollectionService {
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final CommentRepository commentRepository;
    private final RestTemplate restTemplate;

    private static final String BASE_URL = "https://api.stackexchange.com/2.3";
    private static final String SITE = "stackoverflow";

    public DataCollectionService(QuestionRepository questionRepository,
                                 AnswerRepository answerRepository,
                                 CommentRepository commentRepository) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.commentRepository = commentRepository;
        this.restTemplate = new RestTemplate();
    }

    /**
     * 🔥 新方法：按月收集固定数量的问题
     * @param monthsBack 往前追溯的月份数
     * @param questionsPerMonth 每月收集的问题数量
     */
    public String fetchQuestionsByMonth(int monthsBack, int questionsPerMonth) {
        int totalSaved = 0;
        int failedMonths = 0;

        LocalDate now = LocalDate.now();

        for (int i = 0; i < monthsBack; i++) {
            YearMonth targetMonth = YearMonth.from(now.minusMonths(i));

            // 计算该月的时间范围（Unix timestamp）
            long monthStart = targetMonth.atDay(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toEpochSecond();

            long monthEnd = targetMonth.atEndOfMonth()
                    .atTime(23, 59, 59)
                    .atZone(ZoneId.systemDefault())
                    .toEpochSecond();

            System.out.println("\n📅 开始收集 " + targetMonth + " 的数据...");

            try {
                int savedThisMonth = fetchQuestionsInDateRange(
                        monthStart,
                        monthEnd,
                        questionsPerMonth,
                        targetMonth.toString()
                );
                totalSaved += savedThisMonth;

                System.out.println("✅ " + targetMonth + " 完成，保存 " + savedThisMonth + " 条");

            } catch (Exception e) {
                failedMonths++;
                System.err.println("❌ " + targetMonth + " 失败: " + e.getMessage());
            }

            // 月份之间暂停
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return String.format("📊 按月收集完成！共保存 %d 条问题，%d 个月失败",
                totalSaved, failedMonths);
    }

    /**
     * 收集指定时间范围内的问题
     */
    private int fetchQuestionsInDateRange(long fromDate, long toDate,
                                          int targetCount, String monthLabel) {
        int saved = 0;
        int page = 1;
        int maxPages = (targetCount / 100) + 2; // 预留缓冲

        while (saved < targetCount && page <= maxPages) {
            int retryCount = 0;
            int maxRetries = 3;
            boolean success = false;

            while (!success && retryCount < maxRetries) {
                try {
                    String url = BASE_URL + "/questions?" +
                            "page=" + page +
                            "&pagesize=100" +
                            "&fromdate=" + fromDate +
                            "&todate=" + toDate +
                            "&order=desc" +
                            "&sort=votes" +  // 按投票排序，获取有代表性的问题
                            "&tagged=java" +
                            "&site=" + SITE +
                            "&filter=withbody";

                    String response = restTemplate.getForObject(url, String.class);
                    JSONObject json = new JSONObject(response);
                    JSONArray items = json.getJSONArray("items");

                    if (items.length() == 0) {
                        System.out.println("  ⚠️ " + monthLabel + " 第 " + page + " 页无数据，停止");
                        return saved;
                    }

                    int savedThisPage = 0;
                    for (int i = 0; i < items.length() && saved < targetCount; i++) {
                        JSONObject q = items.getJSONObject(i);
                        Question question = parseQuestion(q);

                        if (!questionRepository.existsById(question.getQuestionId())) {
                            questionRepository.save(question);
                            saved++;
                            savedThisPage++;
                        }
                    }

                    System.out.println("  📄 " + monthLabel + " 第 " + page + " 页: +"
                            + savedThisPage + " (总计 " + saved + "/" + targetCount + ")");

                    success = true;
                    Thread.sleep(2000);

                } catch (Exception e) {
                    retryCount++;
                    handleRequestError(e, retryCount, maxRetries, monthLabel, page);
                }
            }

            if (!success) {
                break; // 该页失败，跳过
            }
            page++;
        }

        return saved;
    }

    /**
     * 处理请求错误（重试逻辑）
     */
    private void handleRequestError(Exception e, int retryCount, int maxRetries,
                                    String context, int page) {
        System.err.println("  ⚠️ " + context + " 第 " + page + " 页出错 (尝试 "
                + retryCount + "/" + maxRetries + "): " + e.getMessage());

        if (e.getMessage() != null && e.getMessage().contains("429")) {
            int waitTime = 60 * retryCount;
            System.out.println("  ⏳ 触发限流，等待 " + waitTime + " 秒...");
            try {
                Thread.sleep(waitTime * 1000L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        } else {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 原有方法保留：收集Java相关问题（按活跃度）
     */
    public String fetchJavaQuestions(int pages) {
        // ... 保持原有代码不变 ...
        int totalSaved = 0;
        int failedPages = 0;

        for (int page = 1; page <= pages; page++) {
            int retryCount = 0;
            int maxRetries = 3;
            boolean success = false;

            while (!success && retryCount < maxRetries) {
                try {
                    String url = BASE_URL + "/questions?" +
                            "page=" + page +
                            "&pagesize=100" +
                            "&order=desc" +
                            "&sort=activity" +
                            "&tagged=java" +
                            "&site=" + SITE +
                            "&filter=withbody";

                    String response = restTemplate.getForObject(url, String.class);
                    JSONObject json = new JSONObject(response);
                    JSONArray items = json.getJSONArray("items");

                    int savedThisPage = 0;
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject q = items.getJSONObject(i);
                        Question question = parseQuestion(q);

                        if (!questionRepository.existsById(question.getQuestionId())) {
                            questionRepository.save(question);
                            totalSaved++;
                            savedThisPage++;
                        }
                    }

                    System.out.println("第 " + page + "/" + pages + " 页完成，本页保存 "
                            + savedThisPage + " 条，总计: " + totalSaved);

                    success = true;
                    Thread.sleep(2000);

                } catch (Exception e) {
                    retryCount++;
                    System.err.println("第 " + page + " 页出错 (尝试 " + retryCount + "/"
                            + maxRetries + "): " + e.getMessage());

                    if (e.getMessage() != null && e.getMessage().contains("429")) {
                        int waitTime = 60 * retryCount;
                        System.out.println("触发限流，等待 " + waitTime + " 秒后重试...");
                        try {
                            Thread.sleep(waitTime * 1000L);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }

            if (!success) {
                failedPages++;
                System.err.println("第 " + page + " 页最终失败，跳过");
            }
        }

        return String.format("数据收集完成！共保存 %d 条问题，%d 页失败", totalSaved, failedPages);
    }

    // ... 其他方法保持不变（fetchAnswersForQuestions, fetchCommentsForQuestions等）...

    /**
     * 为已有问题收集答案（智能跳过 + 限流处理）
     */
    public String fetchAnswersForQuestions() {
        int totalAnswers = 0;
        int skipped = 0;
        int failed = 0;
        int processed = 0;

        var questions = questionRepository.findAll();
        int total = questions.size();

        for (Question question : questions) {
            processed++;

            if (question.getAnswerCount() != null && question.getAnswerCount() == 0) {
                skipped++;
                continue;
            }

            long existingAnswers = answerRepository.countByQuestionId(question.getQuestionId());
            if (existingAnswers > 0) {
                skipped++;
                continue;
            }

            int retryCount = 0;
            int maxRetries = 3;
            boolean success = false;

            while (!success && retryCount < maxRetries) {
                try {
                    String url = BASE_URL + "/questions/" + question.getQuestionId() + "/answers?" +
                            "&order=desc" +
                            "&sort=votes" +
                            "&site=" + SITE +
                            "&filter=withbody";

                    String response = restTemplate.getForObject(url, String.class);
                    JSONObject json = new JSONObject(response);
                    JSONArray items = json.getJSONArray("items");

                    int answersForThisQuestion = 0;
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject a = items.getJSONObject(i);
                        Answer answer = parseAnswer(a, question);
                        answerRepository.save(answer);
                        totalAnswers++;
                        answersForThisQuestion++;
                    }

                    System.out.println("[" + processed + "/" + total + "] 问题 " + question.getQuestionId() +
                            " 获取了 " + answersForThisQuestion + " 个答案，总计: " + totalAnswers);

                    success = true;
                    Thread.sleep(2000);

                } catch (Exception e) {
                    retryCount++;
                    System.err.println("[" + processed + "/" + total + "] 问题 " + question.getQuestionId() +
                            " 失败 (尝试 " + retryCount + "/" + maxRetries + "): " + e.getMessage());

                    if (e.getMessage() != null && e.getMessage().contains("429")) {
                        int waitTime = 60 * retryCount;
                        System.out.println("触发限流，等待 " + waitTime + " 秒后重试...");
                        try {
                            Thread.sleep(waitTime * 1000L);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }

            if (!success) {
                failed++;
            }
        }

        return String.format("答案收集完成！新保存 %d 条答案，跳过 %d 个问题，失败 %d 个",
                totalAnswers, skipped, failed);
    }

    /**
     * 收集评论
     */
    public String fetchCommentsForQuestions() {
        int totalComments = 0;
        int skipped = 0;
        int failed = 0;
        int processed = 0;

        var questions = questionRepository.findAll();
        int total = questions.size();

        for (Question question : questions) {
            processed++;

            long existingComments = commentRepository.countByQuestionId(question.getQuestionId());
            if (existingComments > 0) {
                skipped++;
                continue;
            }

            int retryCount = 0;
            int maxRetries = 3;
            boolean success = false;

            while (!success && retryCount < maxRetries) {
                try {
                    String url = BASE_URL + "/questions/" + question.getQuestionId() + "/comments?" +
                            "&order=desc" +
                            "&sort=creation" +
                            "&site=" + SITE +
                            "&filter=withbody";

                    String response = restTemplate.getForObject(url, String.class);
                    JSONObject json = new JSONObject(response);
                    JSONArray items = json.getJSONArray("items");

                    int commentsForThisQuestion = 0;
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject c = items.getJSONObject(i);
                        Comment comment = parseQuestionComment(c, question);
                        commentRepository.save(comment);
                        totalComments++;
                        commentsForThisQuestion++;
                    }

                    if (commentsForThisQuestion > 0) {
                        System.out.println("[" + processed + "/" + total + "] 问题 " + question.getQuestionId() +
                                " 获取了 " + commentsForThisQuestion + " 条评论，总计: " + totalComments);
                    }

                    success = true;
                    Thread.sleep(2000);

                } catch (Exception e) {
                    retryCount++;

                    if (e.getMessage() != null && e.getMessage().contains("429")) {
                        int waitTime = 60 * retryCount;
                        System.out.println("触发限流，等待 " + waitTime + " 秒后重试...");
                        try {
                            Thread.sleep(waitTime * 1000L);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }

            if (!success) {
                failed++;
            }
        }

        return String.format("评论收集完成！新保存 %d 条评论，跳过 %d 个问题，失败 %d 个",
                totalComments, skipped, failed);
    }

    /**
     * 批量收集评论（每次请求获取多个问题的评论）
     */
    public String fetchCommentsBatch() {
        int totalComments = 0;
        int processed = 0;

        var questions = questionRepository.findAll();
        int total = questions.size();
        int batchSize = 100;  // 每批100个问题

        List<Long> questionIds = questions.stream()
                .map(Question::getQuestionId)
                .collect(Collectors.toList());

        for (int i = 0; i < questionIds.size(); i += batchSize) {
            int end = Math.min(i + batchSize, questionIds.size());
            List<Long> batch = questionIds.subList(i, end);

            // 用分号连接多个ID
            String ids = batch.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(";"));

            try {
                String url = BASE_URL + "/questions/" + ids + "/comments?" +
                        "&order=desc" +
                        "&sort=creation" +
                        "&site=" + SITE +
                        "&filter=withbody" +
                        "&pagesize=100";

                String response = restTemplate.getForObject(url, String.class);
                JSONObject json = new JSONObject(response);
                JSONArray items = json.getJSONArray("items");

                for (int j = 0; j < items.length(); j++) {
                    JSONObject c = items.getJSONObject(j);
                    Long postId = c.getLong("post_id");

                    // 查找对应的问题
                    Question question = questionRepository.findById(postId).orElse(null);
                    if (question != null) {
                        Comment comment = new Comment();
                        comment.setCommentId(c.getLong("comment_id"));
                        comment.setBody(c.optString("body", ""));
                        comment.setScore(c.optInt("score", 0));
                        comment.setCreationDate(String.valueOf(c.getLong("creation_date")));

                        if (c.has("owner")) {
                            JSONObject owner = c.getJSONObject("owner");
                            comment.setOwnerName(owner.optString("display_name", "unknown"));
                            if (owner.has("user_id")) {
                                comment.setOwnerUserId(owner.getLong("user_id"));
                            }
                        }

                        comment.setQuestion(question);
                        commentRepository.save(comment);
                        totalComments++;
                    }
                }

                processed += batch.size();
                System.out.println("批量处理 " + processed + "/" + total + " 个问题，已收集 " + totalComments + " 条评论");

                // 等待3秒
                Thread.sleep(3000);

            } catch (Exception e) {
                System.err.println("批量收集评论失败: " + e.getMessage());

                if (e.getMessage() != null && e.getMessage().contains("429")) {
                    System.out.println("触发限流，等待120秒...");
                    try {
                        Thread.sleep(120000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        return "评论收集完成！共保存 " + totalComments + " 条评论";
    }

    /**
     * 清空所有数据
     */
    public void clearAllData() {
        commentRepository.deleteAll();
        answerRepository.deleteAll();
        questionRepository.deleteAll();
        System.out.println("所有数据已清空！");
    }

    /**
     * 获取数据统计
     */
    public String getDataStats() {
        long questions = questionRepository.count();
        long answers = answerRepository.count();
        long comments = commentRepository.count();

        return String.format("当前数据库: %d 个问题, %d 个答案, %d 条评论",
                questions, answers, comments);
    }

    // ==================== 解析方法 ====================

    private Question parseQuestion(JSONObject q) {
        Question question = new Question();

        question.setQuestionId(q.getLong("question_id"));
        question.setTitle(q.optString("title", ""));
        question.setBody(q.optString("body", ""));
        question.setScore(q.optInt("score", 0));
        question.setViewCount(q.optInt("view_count", 0));
        question.setAnswerCount(q.optInt("answer_count", 0));
        question.setIsAnswered(q.optBoolean("is_answered", false));

        if (q.has("accepted_answer_id")) {
            question.setAcceptedAnswerId(q.getLong("accepted_answer_id"));
        }

        question.setCreationDate(String.valueOf(q.getLong("creation_date")));

        if (q.has("last_activity_date")) {
            question.setLastActivityDate(String.valueOf(q.getLong("last_activity_date")));
        }

        question.setLink(q.optString("link", ""));

        if (q.has("tags")) {
            Set<String> tags = new HashSet<>();
            JSONArray tagsArray = q.getJSONArray("tags");
            for (int j = 0; j < tagsArray.length(); j++) {
                tags.add(tagsArray.getString(j));
            }
            question.setTags(tags);
        }

        if (q.has("owner")) {
            JSONObject owner = q.getJSONObject("owner");
            question.setOwnerName(owner.optString("display_name", "unknown"));
            question.setOwnerReputation(owner.optInt("reputation", 0));
            if (owner.has("user_id")) {
                question.setOwnerUserId(owner.getLong("user_id"));
            }
        }

        return question;
    }

    private Answer parseAnswer(JSONObject a, Question question) {
        Answer answer = new Answer();

        answer.setAnswerId(a.getLong("answer_id"));
        answer.setBody(a.optString("body", ""));
        answer.setScore(a.optInt("score", 0));
        answer.setIsAccepted(a.optBoolean("is_accepted", false));
        answer.setCreationDate(String.valueOf(a.getLong("creation_date")));

        if (a.has("last_activity_date")) {
            answer.setLastActivityDate(String.valueOf(a.getLong("last_activity_date")));
        }

        if (a.has("owner")) {
            JSONObject owner = a.getJSONObject("owner");
            answer.setOwnerName(owner.optString("display_name", "unknown"));
            answer.setOwnerReputation(owner.optInt("reputation", 0));
            if (owner.has("user_id")) {
                answer.setOwnerUserId(owner.getLong("user_id"));
            }
        }

        answer.setQuestion(question);
        return answer;
    }

    private Comment parseQuestionComment(JSONObject c, Question question) {
        Comment comment = new Comment();

        comment.setCommentId(c.getLong("comment_id"));
        comment.setBody(c.optString("body", ""));
        comment.setScore(c.optInt("score", 0));
        comment.setCreationDate(String.valueOf(c.getLong("creation_date")));

        if (c.has("owner")) {
            JSONObject owner = c.getJSONObject("owner");
            comment.setOwnerName(owner.optString("display_name", "unknown"));
            if (owner.has("user_id")) {
                comment.setOwnerUserId(owner.getLong("user_id"));
            }
        }

        comment.setQuestion(question);
        return comment;
    }
}