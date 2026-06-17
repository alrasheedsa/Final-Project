package com.example.fproject.Repository;

import com.example.fproject.Model.CampaignSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignSuggestionRepository  extends JpaRepository<CampaignSuggestion, Integer> {
    CampaignSuggestion findCampaignSuggestionById(Integer id);
    List<CampaignSuggestion> findAllByAiAnalysis_Id(Integer aiAnalysisId);

    boolean existsByAiAnalysis_IdAndSuggestionRound(Integer aiAnalysisId, Integer suggestionRound);
}
