package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.AiQuestionRequestIn;
import com.example.fproject.Model.User;
import com.example.fproject.Service.AiQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai-questions")
@RequiredArgsConstructor
public class AiQuestionController {

    private final AiQuestionService aiQuestionService;

    // ADMIN
    @GetMapping("/get")
    public ResponseEntity<?> getAllAiQuestions() {
        return ResponseEntity.status(200).body(aiQuestionService.getAllAiQuestions());
    }

    // STORE_OWNER
    @GetMapping("/get/{aiQuestionId}")
    public ResponseEntity<?> getAiQuestionById(@AuthenticationPrincipal User user, @PathVariable Integer aiQuestionId) {
        return ResponseEntity.status(200).body(aiQuestionService.getAiQuestionById(user.getId(), aiQuestionId));
    }

    // STORE_OWNER
    @PostMapping("/generate-question")
    public ResponseEntity<?> generateAiQuestion(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(200).body(aiQuestionService.generateAiQuestion(user.getId()));
    }

    // STORE_OWNER
    @PostMapping("/generate-for-campaign/{campaignId}")
    public ResponseEntity<?> generateAiQuestionForCampaign(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(aiQuestionService.generateAiQuestionForCampaign(user.getId(), campaignId));
    }

    // STORE_OWNER
    @PutMapping("/regenerate/{campaignId}")
    public ResponseEntity<?> regenerateAiQuestion(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(aiQuestionService.regenerateAiQuestion(user.getId(), campaignId));
    }

    // STORE_OWNER
    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<?> getAiQuestionByCampaignId(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(aiQuestionService.getAiQuestionByCampaignId(user.getId(), campaignId));
    }

    // STORE_OWNER
    @PostMapping("/add")
    public ResponseEntity<?> addAiQuestion(@AuthenticationPrincipal User user, @RequestBody @Valid AiQuestionRequestIn aiQuestionRequestIn) {
        aiQuestionService.addAiQuestion(user.getId(), aiQuestionRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("AI question added successfully"));
    }

    // STORE_OWNER
    @PutMapping("/update/{aiQuestionId}")
    public ResponseEntity<?> updateAiQuestion(@AuthenticationPrincipal User user,
                                              @PathVariable Integer aiQuestionId,
                                              @RequestBody @Valid AiQuestionRequestIn aiQuestionRequestIn) {
        aiQuestionService.updateAiQuestion(user.getId(), aiQuestionId, aiQuestionRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("AI question updated successfully"));
    }

    // STORE_OWNER
    @DeleteMapping("/deleted/{aiQuestionId}")
    public ResponseEntity<?> deleteAiQuestion(@AuthenticationPrincipal User user, @PathVariable Integer aiQuestionId) {
        aiQuestionService.deleteAiQuestion(user.getId(), aiQuestionId);
        return ResponseEntity.status(200).body(new ApiResponse("AI question deleted successfully"));
    }
}