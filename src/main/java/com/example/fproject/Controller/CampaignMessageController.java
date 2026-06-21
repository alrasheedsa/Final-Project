package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.CampaignMessageRequestIn;
import com.example.fproject.Model.User;
import com.example.fproject.Service.CampaignMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/campaign-messages")
@RequiredArgsConstructor
public class CampaignMessageController {

    private final CampaignMessageService campaignMessageService;

    // ADMIN
    @GetMapping("/get")
    public ResponseEntity<?> getAllCampaignMessages() {
        return ResponseEntity.status(200).body(campaignMessageService.getAllCampaignMessages());
    }

    // STORE_OWNER
    @GetMapping("/get/{campaignMessageId}")
    public ResponseEntity<?> getCampaignMessageById(@AuthenticationPrincipal User user, @PathVariable Integer campaignMessageId) {
        return ResponseEntity.status(200).body(campaignMessageService.getCampaignMessageById(user.getId(), campaignMessageId));
    }

    // STORE_OWNER
    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<?> getMessagesByCampaign(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignMessageService.getMessagesByCampaign(user.getId(), campaignId));
    }

    // ADMIN
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<?> getMessagesByCustomer(@PathVariable Integer customerId) {
        return ResponseEntity.status(200).body(campaignMessageService.getMessagesByCustomer(customerId));
    }

    // STORE_OWNER — يشوف رسائل كاستومر معين برقم جواله
    @GetMapping("/open-by-phone")
    public ResponseEntity<?> getOpenMessageForCustomerPhone(@AuthenticationPrincipal User user, @RequestParam String phone) {
        return ResponseEntity.status(200).body(campaignMessageService.getOpenMessageForCustomerPhone(user.getId(), phone));
    }

    // STORE_OWNER
    @PostMapping("/add")
    public ResponseEntity<?> addCampaignMessage(@AuthenticationPrincipal User user, @RequestBody @Valid CampaignMessageRequestIn campaignMessageRequestIn) {
        campaignMessageService.addCampaignMessage(user.getId(), campaignMessageRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign message added successfully"));
    }

    // STORE_OWNER
    @PutMapping("/update/{campaignMessageId}")
    public ResponseEntity<?> updateCampaignMessage(@AuthenticationPrincipal User user,
                                                   @PathVariable Integer campaignMessageId,
                                                   @RequestBody @Valid CampaignMessageRequestIn campaignMessageRequestIn) {
        campaignMessageService.updateCampaignMessage(user.getId(), campaignMessageId, campaignMessageRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign message updated successfully"));
    }

    // STORE_OWNER
    @PutMapping("/mark-sent/{campaignMessageId}")
    public ResponseEntity<?> markMessageAsSent(@AuthenticationPrincipal User user, @PathVariable Integer campaignMessageId) {
        campaignMessageService.markMessageAsSent(user.getId(), campaignMessageId);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign message marked as sent successfully"));
    }

    // STORE_OWNER
    @PutMapping("/mark-answered/{campaignMessageId}")
    public ResponseEntity<?> markMessageAsAnswered(@AuthenticationPrincipal User user, @PathVariable Integer campaignMessageId) {
        campaignMessageService.markMessageAsAnswered(user.getId(), campaignMessageId);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign message marked as answered successfully"));
    }

    // STORE_OWNER
    @DeleteMapping("/deleted/{campaignMessageId}")
    public ResponseEntity<?> deleteCampaignMessage(@AuthenticationPrincipal User user, @PathVariable Integer campaignMessageId) {
        campaignMessageService.deleteCampaignMessage(user.getId(), campaignMessageId);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign message deleted successfully"));
    }
}