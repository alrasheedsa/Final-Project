package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.AiQuestionRequestIn;
import com.example.fproject.Service.AiQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai-questions")
@RequiredArgsConstructor
public class AiQuestionController {

    private final AiQuestionService aiQuestionService;

    @GetMapping("/get")
    public ResponseEntity<?> getAllAiQuestions() {
        return ResponseEntity.status(200).body(aiQuestionService.getAllAiQuestions());
    }

    @GetMapping("/get/{aiQuestionId}")
    public ResponseEntity<?> getAiQuestionById(@PathVariable Integer aiQuestionId) {
        return ResponseEntity.status(200).body(aiQuestionService.getAiQuestionById(aiQuestionId));
    }

    @PostMapping("/generate-question")
    public ResponseEntity<?> generateAiQuestion() {
        return ResponseEntity.status(200).body(aiQuestionService.generateAiQuestion());
    }

    @PostMapping("/add")
    public ResponseEntity<?> addAiQuestion(@RequestBody @Valid AiQuestionRequestIn aiQuestionRequestIn) {
        aiQuestionService.addAiQuestion(aiQuestionRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("AI question added successfully"));
    }

    @PutMapping("/update/{aiQuestionId}")
    public ResponseEntity<?> updateAiQuestion(@PathVariable Integer aiQuestionId,
                                              @RequestBody @Valid AiQuestionRequestIn aiQuestionRequestIn) {
        aiQuestionService.updateAiQuestion(aiQuestionId, aiQuestionRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("AI question updated successfully"));
    }

    @DeleteMapping("/deleted/{aiQuestionId}")
    public ResponseEntity<?> deleteAiQuestion(@PathVariable Integer aiQuestionId) {
        // Business note: endpoint exists for CRUD coverage; workflow may detach or update the question instead of hard delete.
        aiQuestionService.deleteAiQuestion(aiQuestionId);
        return ResponseEntity.status(200).body(new ApiResponse("AI question deleted successfully"));
    }
}
