package com.example.fproject.Repository;

import com.example.fproject.Model.CustomerAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerAnswerRepository extends JpaRepository<CustomerAnswer, Integer> {
}
