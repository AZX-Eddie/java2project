package com.example.demo.service;

import com.example.demo.model.Question;
import com.example.demo.model.dto.MultithreadingPitfallDTO;
import com.example.demo.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MultithreadingAnalysisService {

    private final QuestionRepository questionRepository;

    // 多线程陷阱的关键词和正则模式
    private static final Map<String, PitfallPattern> PITFALL_PATTERNS = new LinkedHashMap<>();

    static {
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
    }

    public MultithreadingAnalysisService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /**
     * 获取Top N多线程陷阱
     */
    public List<MultithreadingPitfallDTO> getTopPitfalls(int topN) {
        List<Question> allQuestions = questionRepository.findAll();

        // 筛选多线程相关问题
        List<Question> threadQuestions = allQuestions.stream()
                .filter(this::isMultithreadingRelated)
                .collect(Collectors.toList());

        long totalThreadQuestions = threadQuestions.size();

        // 分析每种陷阱
        Map<String, List<Question>> pitfallQuestions = new HashMap<>();

        for (Question question : threadQuestions) {
            String content = getQuestionContent(question);

            for (Map.Entry<String, PitfallPattern> entry : PITFALL_PATTERNS.entrySet()) {
                String pitfallName = entry.getKey();
                PitfallPattern pattern = entry.getValue();

                if (pattern.matches(content)) {
                    pitfallQuestions.computeIfAbsent(pitfallName, k -> new ArrayList<>())
                            .add(question);
                }
            }
        }

        // 构建返回结果
        return pitfallQuestions.entrySet().stream()
                .map(entry -> {
                    String pitfallName = entry.getKey();
                    List<Question> questions = entry.getValue();
                    PitfallPattern pattern = PITFALL_PATTERNS.get(pitfallName);

                    MultithreadingPitfallDTO dto = new MultithreadingPitfallDTO();
                    dto.setPitfallCategory(pitfallName);
                    dto.setDescription(pattern.description);
                    dto.setOccurrenceCount((long) questions.size());
                    dto.setPercentage(Math.round(questions.size() * 10000.0 / totalThreadQuestions) / 100.0);
                    dto.setKeywords(pattern.keywords);
                    dto.setExampleTitles(questions.stream()
                            .limit(3)
                            .map(Question::getTitle)
                            .collect(Collectors.toList()));

                    return dto;
                })
                .sorted((a, b) -> Long.compare(b.getOccurrenceCount(), a.getOccurrenceCount()))
                .limit(topN)
                .collect(Collectors.toList());
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

        Map<String, Long> exceptions = new HashMap<>();
        Pattern exceptionPattern = Pattern.compile("([A-Z][a-zA-Z]*Exception|[A-Z][a-zA-Z]*Error)");

        for (Question question : allQuestions) {
            if (!isMultithreadingRelated(question)) continue;

            String content = getQuestionContent(question);
            Matcher matcher = exceptionPattern.matcher(content);

            while (matcher.find()) {
                String exception = matcher.group(1);
                exceptions.merge(exception, 1L, Long::sum);
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
        stats.put("percentage", Math.round(threadQuestions * 10000.0 / totalQuestions) / 100.0);

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