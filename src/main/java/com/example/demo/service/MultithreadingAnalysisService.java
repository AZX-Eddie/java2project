package com.example.demo.service;

import com.example.demo.model.Answer;
import com.example.demo.model.Question;
import com.example.demo.model.dto.MultithreadingPitfallDTO;
import com.example.demo.repository.AnswerRepository;
import com.example.demo.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MultithreadingAnalysisService {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    // 多线程陷阱的关键词和正则模式
    private static final Map<String, PitfallPattern> PITFALL_PATTERNS = new LinkedHashMap<>();

    // 代码中的多线程陷阱模式（针对代码片段）
    private static final Map<String, Pattern> CODE_PITFALL_PATTERNS = new LinkedHashMap<>();

    // 错误信息模式
    private static final Map<String, Pattern> ERROR_MESSAGE_PATTERNS = new LinkedHashMap<>();

    static {
        // 文本陷阱模式
        PITFALL_PATTERNS.put("Race Condition", new PitfallPattern(
                "多线程同时访问共享数据导致的竞态条件",
                Arrays.asList("race condition", "data race", "shared variable", "concurrent access"),
                Pattern.compile("(?i)(race\\s*condition|data\\s*race|concurrent.*access|shared.*(variable|state|data))")
        ));

        PITFALL_PATTERNS.put("Deadlock", new PitfallPattern(
                "两个或多个线程相互等待导致的死锁",
                Arrays.asList("deadlock", "dead lock", "circular wait", "thread waiting"),
                Pattern.compile("(?i)(deadlock|dead\\s*lock|circular.*wait|thread.*waiting.*forever)")
        ));

        PITFALL_PATTERNS.put("Synchronization Issues", new PitfallPattern(
                "同步机制使用不当",
                Arrays.asList("synchronized", "synchronization", "lock", "mutex", "monitor"),
                Pattern.compile("(?i)(synchronized.*problem|synchronization.*issue|lock.*problem|mutex)")
        ));

        PITFALL_PATTERNS.put("Thread Pool Problems", new PitfallPattern(
                "线程池配置或使用问题",
                Arrays.asList("thread pool", "executor", "executorservice", "threadpoolexecutor"),
                Pattern.compile("(?i)(thread\\s*pool|executor.*service|ThreadPoolExecutor)")
        ));

        PITFALL_PATTERNS.put("ConcurrentModificationException", new PitfallPattern(
                "并发修改集合时抛出的异常",
                Arrays.asList("concurrentmodificationexception", "concurrent modification", "iterator"),
                Pattern.compile("(?i)(ConcurrentModificationException|concurrent.*modification)")
        ));

        PITFALL_PATTERNS.put("Memory Visibility", new PitfallPattern(
                "线程间内存可见性问题",
                Arrays.asList("volatile", "visibility", "happens-before", "memory model"),
                Pattern.compile("(?i)(volatile|memory\\s*visibility|happens.?before|memory\\s*model)")
        ));

        PITFALL_PATTERNS.put("Thread Interruption", new PitfallPattern(
                "线程中断处理问题",
                Arrays.asList("interrupt", "interruptedexception", "thread stop"),
                Pattern.compile("(?i)(interrupt|InterruptedException|thread.*stop)")
        ));

        PITFALL_PATTERNS.put("Wait/Notify Issues", new PitfallPattern(
                "wait/notify机制使用问题",
                Arrays.asList("wait", "notify", "notifyall", "spurious wakeup"),
                Pattern.compile("(?i)(\\bwait\\s*\\(|notify\\s*\\(|notifyAll|spurious.*wakeup)")
        ));

        PITFALL_PATTERNS.put("Starvation", new PitfallPattern(
                "线程饥饿问题",
                Arrays.asList("starvation", "fairness", "priority"),
                Pattern.compile("(?i)(starvation|thread.*starv|fairness|priority.*inversion)")
        ));

        PITFALL_PATTERNS.put("Performance Issues", new PitfallPattern(
                "多线程性能问题",
                Arrays.asList("performance", "contention", "bottleneck", "scalability"),
                Pattern.compile("(?i)(performance.*thread|contention|bottleneck|scalab)")
        ));

        // 代码片段陷阱模式
        // 检测代码中的常见多线程问题
        CODE_PITFALL_PATTERNS.put("Unsafe Lazy Initialization",
                Pattern.compile("if\\s*\\(\\s*\\w+\\s*==\\s*null\\s*\\)\\s*\\{[^}]*new\\s+\\w+"));

        CODE_PITFALL_PATTERNS.put("Double-Checked Locking",
                Pattern.compile("(?i)if\\s*\\([^)]*null[^)]*\\)\\s*\\{\\s*synchronized"));

        CODE_PITFALL_PATTERNS.put("Synchronized on Non-Final",
                Pattern.compile("synchronized\\s*\\(\\s*(?!this|\\w+\\.class)[^)]+\\)"));

        CODE_PITFALL_PATTERNS.put("Thread.stop() Usage",
                Pattern.compile("\\bThread\\s*\\.\\s*stop\\s*\\("));

        CODE_PITFALL_PATTERNS.put("Busy Waiting",
                Pattern.compile("while\\s*\\([^)]*\\)\\s*\\{\\s*\\}|while\\s*\\(true\\)\\s*\\{[^}]*Thread\\.sleep"));

        CODE_PITFALL_PATTERNS.put("Missing Volatile",
                Pattern.compile("(?<!volatile\\s)(?:private|public|protected)?\\s*(?:static)?\\s*boolean\\s+\\w+\\s*=.*running|stop|flag"));

        CODE_PITFALL_PATTERNS.put("Improper Lock Release",
                Pattern.compile("lock\\s*\\(\\s*\\)[^}]*(?!finally)"));

        CODE_PITFALL_PATTERNS.put("Non-Atomic Operations",
                Pattern.compile("\\+\\+\\s*\\w+|\\w+\\s*\\+\\+|\\w+\\s*\\+=|\\w+\\s*-="));

        // 错误信息模式
        ERROR_MESSAGE_PATTERNS.put("Deadlock Error",
                Pattern.compile("(?i)(found\\s+one\\s+java.*deadlock|deadlock\\s+detected|waiting\\s+to\\s+lock)"));

        ERROR_MESSAGE_PATTERNS.put("Thread Starvation Error",
                Pattern.compile("(?i)(thread\\s+starvation|pool\\s+exhausted|rejected\\s+execution)"));

        ERROR_MESSAGE_PATTERNS.put("Memory Consistency Error",
                Pattern.compile("(?i)(visibility\\s+problem|stale\\s+data|memory\\s+inconsisten)"));

        ERROR_MESSAGE_PATTERNS.put("Lock Timeout Error",
                Pattern.compile("(?i)(lock\\s+timeout|failed\\s+to\\s+acquire\\s+lock|unable\\s+to\\s+obtain\\s+lock)"));

        ERROR_MESSAGE_PATTERNS.put("IllegalMonitorStateException",
                Pattern.compile("IllegalMonitorStateException"));

        ERROR_MESSAGE_PATTERNS.put("RejectedExecutionException",
                Pattern.compile("RejectedExecutionException"));

        ERROR_MESSAGE_PATTERNS.put("BrokenBarrierException",
                Pattern.compile("BrokenBarrierException"));

        ERROR_MESSAGE_PATTERNS.put("TimeoutException",
                Pattern.compile("TimeoutException"));
    }

    public MultithreadingAnalysisService(QuestionRepository questionRepository,
                                         AnswerRepository answerRepository) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
    }

    /**
     * 获取Top N多线程陷阱
     */
    public List<MultithreadingPitfallDTO> getTopPitfalls(int topN) {
        List<Question> allQuestions = questionRepository.findAll();
        List<Answer> allAnswers = answerRepository.findAll();

        // 筛选多线程相关问题
        List<Question> threadQuestions = allQuestions.stream()
                .filter(this::isMultithreadingRelated)
                .collect(Collectors.toList());

        // 获取多线程问题的ID集合
        Set<Long> threadQuestionIds = threadQuestions.stream()
                .map(Question::getQuestionId)
                .collect(Collectors.toSet());

        long totalThreadQuestions = threadQuestions.size();

        // 使用 Set<Long> 存储问题ID，避免重复计数
        Map<String, Set<Long>> pitfallQuestionIds = new HashMap<>();

        // 分析问题内容（文本 + 代码 + 错误信息）
        for (Question question : threadQuestions) {
            String body = question.getBody() != null ? question.getBody() : "";
            String title = question.getTitle() != null ? question.getTitle() : "";
            String fullContent = title + " " + body;

            // 分析文本内容
            analyzeTextContent(fullContent, question.getQuestionId(), pitfallQuestionIds);

            // 分析代码片段
            analyzeCodeSnippets(body, question.getQuestionId(), pitfallQuestionIds);

            // 分析错误信息
            analyzeErrorMessages(fullContent, question.getQuestionId(), pitfallQuestionIds);
        }

        // 分析答案内容
        for (Answer answer : allAnswers) {
            if (answer.getQuestion() != null &&
                    threadQuestionIds.contains(answer.getQuestion().getQuestionId())) {
                String body = answer.getBody() != null ? answer.getBody() : "";
                Long questionId = answer.getQuestion().getQuestionId();

                // 分析文本内容
                analyzeTextContent(body, questionId, pitfallQuestionIds);

                // 分析代码片段
                analyzeCodeSnippets(body, questionId, pitfallQuestionIds);

                // 分析错误信息
                analyzeErrorMessages(body, questionId, pitfallQuestionIds);
            }
        }

        // 构建返回结果
        return pitfallQuestionIds.entrySet().stream()
                .map(entry -> {
                    String pitfallName = entry.getKey();
                    Set<Long> questionIds = entry.getValue();

                    MultithreadingPitfallDTO dto = new MultithreadingPitfallDTO();
                    dto.setPitfallCategory(pitfallName);

                    // 获取描述（优先从 PITFALL_PATTERNS 获取）
                    if (PITFALL_PATTERNS.containsKey(pitfallName)) {
                        dto.setDescription(PITFALL_PATTERNS.get(pitfallName).description);
                        dto.setKeywords(PITFALL_PATTERNS.get(pitfallName).keywords);
                    } else {
                        dto.setDescription(getDescriptionForPitfall(pitfallName));
                        dto.setKeywords(Arrays.asList(pitfallName.toLowerCase().split("\\s+")));
                    }

                    dto.setOccurrenceCount((long) questionIds.size());
                    dto.setPercentage(totalThreadQuestions > 0 ?
                            Math.round(questionIds.size() * 10000.0 / totalThreadQuestions) / 100.0 : 0);

                    // 获取示例标题
                    List<String> exampleTitles = threadQuestions.stream()
                            .filter(q -> questionIds.contains(q.getQuestionId()))
                            .limit(3)
                            .map(Question::getTitle)
                            .collect(Collectors.toList());
                    dto.setExampleTitles(exampleTitles);

                    return dto;
                })
                .sorted((a, b) -> Long.compare(b.getOccurrenceCount(), a.getOccurrenceCount()))
                .limit(topN)
                .collect(Collectors.toList());
    }

    /**
     * 分析文本内容中的陷阱
     */
    private void analyzeTextContent(String content, Long questionId,
                                    Map<String, Set<Long>> pitfallQuestionIds) {
        for (Map.Entry<String, PitfallPattern> entry : PITFALL_PATTERNS.entrySet()) {
            if (entry.getValue().matches(content)) {
                pitfallQuestionIds.computeIfAbsent(entry.getKey(), k -> new HashSet<>())
                        .add(questionId);
            }
        }
    }

    /**
     * 分析代码片段中的陷阱
     */
    private void analyzeCodeSnippets(String body, Long questionId,
                                     Map<String, Set<Long>> pitfallQuestionIds) {
        // 提取代码片段
        List<String> codeSnippets = extractCodeSnippets(body);

        for (String code : codeSnippets) {
            // 使用代码专用模式匹配
            for (Map.Entry<String, Pattern> entry : CODE_PITFALL_PATTERNS.entrySet()) {
                if (entry.getValue().matcher(code).find()) {
                    pitfallQuestionIds.computeIfAbsent(entry.getKey(), k -> new HashSet<>())
                            .add(questionId);
                }
            }

            // 同时用文本模式检查代码
            for (Map.Entry<String, PitfallPattern> entry : PITFALL_PATTERNS.entrySet()) {
                if (entry.getValue().matches(code)) {
                    pitfallQuestionIds.computeIfAbsent(entry.getKey(), k -> new HashSet<>())
                            .add(questionId);
                }
            }
        }
    }

    /**
     * 分析错误信息中的陷阱
     */
    private void analyzeErrorMessages(String content, Long questionId,
                                      Map<String, Set<Long>> pitfallQuestionIds) {
        // 提取错误信息/堆栈跟踪
        List<String> errorMessages = extractErrorMessages(content);

        for (String error : errorMessages) {
            for (Map.Entry<String, Pattern> entry : ERROR_MESSAGE_PATTERNS.entrySet()) {
                if (entry.getValue().matcher(error).find()) {
                    pitfallQuestionIds.computeIfAbsent(entry.getKey(), k -> new HashSet<>())
                            .add(questionId);
                }
            }
        }

        // 直接在原内容中也搜索错误模式
        for (Map.Entry<String, Pattern> entry : ERROR_MESSAGE_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(content).find()) {
                pitfallQuestionIds.computeIfAbsent(entry.getKey(), k -> new HashSet<>())
                        .add(questionId);
            }
        }
    }

    /**
     * 提取代码片段
     */
    private List<String> extractCodeSnippets(String body) {
        List<String> codeSnippets = new ArrayList<>();
        if (body == null || body.isEmpty()) {
            return codeSnippets;
        }

        // 匹配 <code>...</code> 标签
        Pattern codePattern = Pattern.compile("<code>(.*?)</code>", Pattern.DOTALL);
        Matcher codeMatcher = codePattern.matcher(body);
        while (codeMatcher.find()) {
            String code = codeMatcher.group(1);
            if (code != null && code.trim().length() > 10) {  // 过滤太短的代码
                codeSnippets.add(code);
            }
        }

        // 匹配 <pre>...</pre> 标签
        Pattern prePattern = Pattern.compile("<pre>(.*?)</pre>", Pattern.DOTALL);
        Matcher preMatcher = prePattern.matcher(body);
        while (preMatcher.find()) {
            String code = preMatcher.group(1);
            if (code != null && code.trim().length() > 10) {
                codeSnippets.add(code);
            }
        }

        // 匹配 ``` 代码块
        Pattern markdownPattern = Pattern.compile("```(?:java)?\\s*(.*?)```", Pattern.DOTALL);
        Matcher mdMatcher = markdownPattern.matcher(body);
        while (mdMatcher.find()) {
            String code = mdMatcher.group(1);
            if (code != null && code.trim().length() > 10) {
                codeSnippets.add(code);
            }
        }

        return codeSnippets;
    }

    /**
     * 提取错误信息/堆栈跟踪
     */
    private List<String> extractErrorMessages(String content) {
        List<String> errors = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return errors;
        }

        // 匹配异常堆栈跟踪
        Pattern stackTracePattern = Pattern.compile(
                "((?:[a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[A-Z][a-zA-Z\\d_$]*(?:Exception|Error)(?::\\s*[^\\n]+)?(?:\\s+at\\s+[^\\n]+)*)",
                Pattern.MULTILINE
        );
        Matcher stMatcher = stackTracePattern.matcher(content);
        while (stMatcher.find()) {
            errors.add(stMatcher.group(1));
        }

        // 匹配 Error: 或 Exception: 开头的信息
        Pattern errorMsgPattern = Pattern.compile(
                "(?:Error|Exception|FATAL|SEVERE|WARNING)\\s*:?\\s*([^\\n]+)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher emMatcher = errorMsgPattern.matcher(content);
        while (emMatcher.find()) {
            errors.add(emMatcher.group(0));
        }

        return errors;
    }

    /**
     * 获取陷阱描述
     */
    private String getDescriptionForPitfall(String pitfallName) {
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("Unsafe Lazy Initialization", "非线程安全的延迟初始化");
        descriptions.put("Double-Checked Locking", "双重检查锁定模式问题");
        descriptions.put("Synchronized on Non-Final", "在非final对象上同步");
        descriptions.put("Thread.stop() Usage", "使用已废弃的Thread.stop()方法");
        descriptions.put("Busy Waiting", "忙等待消耗CPU资源");
        descriptions.put("Missing Volatile", "缺少volatile修饰符");
        descriptions.put("Improper Lock Release", "锁释放不当");
        descriptions.put("Non-Atomic Operations", "非原子操作（如i++）");
        descriptions.put("Deadlock Error", "检测到死锁错误");
        descriptions.put("Thread Starvation Error", "线程饥饿/线程池耗尽");
        descriptions.put("Memory Consistency Error", "内存一致性错误");
        descriptions.put("Lock Timeout Error", "锁超时错误");
        descriptions.put("IllegalMonitorStateException", "非法监视器状态异常");
        descriptions.put("RejectedExecutionException", "任务被拒绝执行异常");
        descriptions.put("BrokenBarrierException", "屏障损坏异常");
        descriptions.put("TimeoutException", "超时异常");

        return descriptions.getOrDefault(pitfallName, "多线程相关问题");
    }

    /**
     * 判断问题是否与多线程相关
     */
    private boolean isMultithreadingRelated(Question question) {
        // 检查标签
        Set<String> tags = question.getTags();
        if (tags != null) {
            for (String tag : tags) {
                String lowerTag = tag.toLowerCase();
                if (lowerTag.contains("thread") || lowerTag.contains("concurren") ||
                        lowerTag.contains("parallel") || lowerTag.contains("synchron") ||
                        lowerTag.contains("async") || lowerTag.contains("executor")) {
                    return true;
                }
            }
        }

        // 检查内容
        String content = getQuestionContent(question);
        Pattern threadPattern = Pattern.compile(
                "(?i)(\\bThread\\b|Runnable|Callable|ExecutorService|" +
                        "synchronized|concurrent|parallel|async|multithreading)"
        );
        return threadPattern.matcher(content).find();
    }

    /**
     * 获取问题内容（标题+正文）
     */
    private String getQuestionContent(Question question) {
        String title = question.getTitle() != null ? question.getTitle() : "";
        String body = question.getBody() != null ? question.getBody() : "";
        return title + " " + body;
    }

    /**
     * 获取异常类型分布
     */
    public Map<String, Long> getExceptionDistribution() {
        List<Question> allQuestions = questionRepository.findAll();
        List<Answer> allAnswers = answerRepository.findAll();  //  也分析答案

        Map<String, Long> exceptions = new HashMap<>();
        Pattern exceptionPattern = Pattern.compile("([A-Z][a-zA-Z]*Exception|[A-Z][a-zA-Z]*Error)");

        // 分析问题
        for (Question question : allQuestions) {
            if (!isMultithreadingRelated(question)) continue;
            String content = getQuestionContent(question);
            Matcher matcher = exceptionPattern.matcher(content);
            while (matcher.find()) {
                String exception = matcher.group(1);
                exceptions.merge(exception, 1L, Long::sum);
            }
        }

        // 分析答案
        Set<Long> threadQuestionIds = allQuestions.stream()
                .filter(this::isMultithreadingRelated)
                .map(Question::getQuestionId)
                .collect(Collectors.toSet());

        for (Answer answer : allAnswers) {
            if (answer.getQuestion() != null &&
                    threadQuestionIds.contains(answer.getQuestion().getQuestionId())) {
                String content = answer.getBody() != null ? answer.getBody() : "";
                Matcher matcher = exceptionPattern.matcher(content);
                while (matcher.find()) {
                    String exception = matcher.group(1);
                    exceptions.merge(exception, 1L, Long::sum);
                }
            }
        }

        // 返回Top 15异常
        return exceptions.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(15)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * 获取多线程问题统计
     */
    public Map<String, Object> getMultithreadingStats() {
        List<Question> allQuestions = questionRepository.findAll();
        long totalQuestions = allQuestions.size();
        long threadQuestions = allQuestions.stream()
                .filter(this::isMultithreadingRelated)
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalQuestions", totalQuestions);
        stats.put("multithreadingQuestions", threadQuestions);
        stats.put("percentage", totalQuestions > 0 ?
                Math.round(threadQuestions * 10000.0 / totalQuestions) / 100.0 : 0);
        return stats;
    }

    /**
     * 陷阱模式内部类
     */
    private static class PitfallPattern {
        String description;
        List<String> keywords;
        Pattern regex;

        PitfallPattern(String description, List<String> keywords, Pattern regex) {
            this.description = description;
            this.keywords = keywords;
            this.regex = regex;
        }

        boolean matches(String content) {
            return regex.matcher(content).find();
        }
    }
}