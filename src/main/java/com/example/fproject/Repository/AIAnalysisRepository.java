package com.example.fproject.Repository;

import com.example.fproject.Model.AIAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AIAnalysisRepository extends JpaRepository<AIAnalysis, Integer> {
    AIAnalysis findAIAnalysisById(Integer id);

    AIAnalysis findAIAnalysisBySalesRecord_Id(Integer salesRecordId);

    Boolean existsBySalesRecord_Id(Integer salesRecordId);
}
