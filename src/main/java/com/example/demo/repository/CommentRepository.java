package com.example.demo.repository;

import com.example.demo.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 根据问题ID查询评论
    List<Comment> findByQuestionQuestionId(Long questionId);

    // 根据答案ID查询评论
    List<Comment> findByAnswerAnswerId(Long answerId);

    // 根据创建日期范围查询
    @Query("SELECT c FROM Comment c WHERE c.creationDate >= :startDate AND c.creationDate <= :endDate")
    List<Comment> findByCreationDateBetween(@Param("startDate") String startDate,
                                            @Param("endDate") String endDate);

    // 统计某个问题的评论数量
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.question.questionId = :questionId")
    Long countByQuestionId(@Param("questionId") Long questionId);

    // 统计某个答案的评论数量
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.answer.answerId = :answerId")
    Long countByAnswerId(@Param("answerId") Long answerId);
}