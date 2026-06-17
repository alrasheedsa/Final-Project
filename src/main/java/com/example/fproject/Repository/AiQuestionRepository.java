package com.example.fproject.Repository;

import com.example.fproject.Model.AIQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiQuestionRepository extends JpaRepository<AIQuestion, Integer> {
}
