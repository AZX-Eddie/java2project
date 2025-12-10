package com.example.demo.repository;

import com.example.demo.model.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    // 根据问题ID查询答案
    List<Answer> findByQuestionQuestionId(Long questionId);

    // 查询被接受的答案
    @Query("SELECT a FROM Answer a WHERE a.isAccepted = true")
    List<Answer> findAcceptedAnswers();

    // 查询高分答案
    @Query("SELECT a FROM Answer a WHERE a.score >= :minScore")
    List<Answer> findHighScoreAnswers(@Param("minScore") Integer minScore);

    // 根据创建日期范围查询
    @Query("SELECT a FROM Answer a WHERE a.creationDate >= :startDate AND a.creationDate <= :endDate")
    List<Answer> findByCreationDateBetween(@Param("startDate") String startDate,
                                           @Param("endDate") String endDate);

    // 统计某个问题的答案数量
    @Query("SELECT COUNT(a) FROM Answer a WHERE a.question.questionId = :questionId")
    Long countByQuestionId(@Param("questionId") Long questionId);
}