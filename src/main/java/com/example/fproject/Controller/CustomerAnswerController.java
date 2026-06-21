package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.CustomerAnswerRequestIn;
import com.example.fproject.Model.User;
import com.example.fproject.Service.CustomerAnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer-answers")
@RequiredArgsConstructor
public class CustomerAnswerController {

    private final CustomerAnswerService customerAnswerService;

    // ADMIN
    @GetMapping("/get")
    public ResponseEntity<?> getAllCustomerAnswers() {
        return ResponseEntity.status(200).body(customerAnswerService.getAllCustomerAnswers());
    }

    // STORE_OWNER
    @GetMapping("/get/{customerAnswerId}")
    public ResponseEntity<?> getCustomerAnswerById(@AuthenticationPrincipal User user, @PathVariable Integer customerAnswerId) {
        return ResponseEntity.status(200).body(customerAnswerService.getCustomerAnswerById(user.getId(), customerAnswerId));
    }

    // STORE_OWNER
    @GetMapping("/campaign-message/{campaignMessageId}")
    public ResponseEntity<?> getCustomerAnswerByCampaignMessage(@AuthenticationPrincipal User user, @PathVariable Integer campaignMessageId) {
        return ResponseEntity.status(200).body(customerAnswerService.getCustomerAnswerByCampaignMessage(user.getId(), campaignMessageId));
    }

    // STORE_OWNER
    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<?> getAnswersByCampaign(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(customerAnswerService.getAnswersByCampaign(user.getId(), campaignId));
    }

    // CUSTOMER — يجاوب على رسالة حملة
    @PostMapping("/answer/{campaignMessageId}")
    public ResponseEntity<?> answerCampaignMessage(@AuthenticationPrincipal User user,
                                                   @PathVariable Integer campaignMessageId,
                                                   @RequestParam String answer) {
        return ResponseEntity.status(200).body(customerAnswerService.answerCampaignMessage(user.getId(), campaignMessageId, answer));
    }

    // STORE_OWNER
    @PostMapping("/add")
    public ResponseEntity<?> addCustomerAnswer(@AuthenticationPrincipal User user, @RequestBody @Valid CustomerAnswerRequestIn customerAnswerRequestIn) {
        customerAnswerService.addCustomerAnswer(user.getId(), customerAnswerRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("Customer answer added successfully"));
    }

    // STORE_OWNER
    @PutMapping("/update/{customerAnswerId}")
    public ResponseEntity<?> updateCustomerAnswer(@AuthenticationPrincipal User user,
                                                  @PathVariable Integer customerAnswerId,
                                                  @RequestBody @Valid CustomerAnswerRequestIn customerAnswerRequestIn) {
        customerAnswerService.updateCustomerAnswer(user.getId(), customerAnswerId, customerAnswerRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("Customer answer updated successfully"));
    }

    // STORE_OWNER
    @DeleteMapping("/deleted/{customerAnswerId}")
    public ResponseEntity<?> deleteCustomerAnswer(@AuthenticationPrincipal User user, @PathVariable Integer customerAnswerId) {
        customerAnswerService.deleteCustomerAnswer(user.getId(), customerAnswerId);
        return ResponseEntity.status(200).body(new ApiResponse("Customer answer deleted successfully"));
    }
}