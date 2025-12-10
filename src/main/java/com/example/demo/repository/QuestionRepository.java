package com.example.demo.repository;

import com.example.demo.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    // 根据标签查询问题
    @Query("SELECT q FROM Question q JOIN q.tags t WHERE t = :tag")
    List<Question> findByTag(@Param("tag") String tag);

    // 根据多个标签查询（包含任意一个）
    @Query("SELECT DISTINCT q FROM Question q JOIN q.tags t WHERE t IN :tags")
    List<Question> findByTagsIn(@Param("tags") List<String> tags);

    // 查询已解决的问题（有被接受的答案）
    @Query("SELECT q FROM Question q WHERE q.acceptedAnswerId IS NOT NULL")
    List<Question> findSolvedQuestions();

    // 查询未解决的问题
    @Query("SELECT q FROM Question q WHERE q.acceptedAnswerId IS NULL")
    List<Question> findUnsolvedQuestions();

    // 查询有答案的问题
    @Query("SELECT q FROM Question q WHERE q.answerCount > 0")
    List<Question> findQuestionsWithAnswers();

    // 查询没有答案的问题
    @Query("SELECT q FROM Question q WHERE q.answerCount = 0 OR q.answerCount IS NULL")
    List<Question> findQuestionsWithoutAnswers();

    // 根据创建日期范围查询
    @Query("SELECT q FROM Question q WHERE q.creationDate >= :startDate AND q.creationDate <= :endDate")
    List<Question> findByCreationDateBetween(@Param("startDate") String startDate,
                                             @Param("endDate") String endDate);

    // 查询高分问题
    @Query("SELECT q FROM Question q WHERE q.score >= :minScore")
    List<Question> findHighScoreQuestions(@Param("minScore") Integer minScore);
}