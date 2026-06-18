package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.CampaignMessageRequestIn;
import com.example.fproject.Service.CampaignMessageService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/campaign-messages")
@RequiredArgsConstructor
public class CampaignMessageController {

    private final CampaignMessageService campaignMessageService;

    @GetMapping("/get")
    public ResponseEntity<?> getAllCampaignMessages() {
        return ResponseEntity.status(200).body(campaignMessageService.getAllCampaignMessages());
    }

    @GetMapping("/get/{campaignMessageId}")
    public ResponseEntity<?> getCampaignMessageById(@PathVariable Integer campaignMessageId) {
        return ResponseEntity.status(200).body(campaignMessageService.getCampaignMessageById(campaignMessageId));
    }

    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<?> getMessagesByCampaign(@PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(campaignMessageService.getMessagesByCampaign(campaignId));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<?> getMessagesByCustomer(@PathVariable Integer customerId) {
        return ResponseEntity.status(200).body(campaignMessageService.getMessagesByCustomer(customerId));
    }

    @GetMapping("/open-by-phone")
    public ResponseEntity<?> getOpenMessageForCustomerPhone(@RequestParam String phone) {
        return ResponseEntity.status(200).body(campaignMessageService.getOpenMessageForCustomerPhone(phone));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addCampaignMessage(@RequestBody @Valid CampaignMessageRequestIn campaignMessageRequestIn) {
        campaignMessageService.addCampaignMessage(campaignMessageRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign message added successfully"));
    }

    @PutMapping("/update/{campaignMessageId}")
    public ResponseEntity<?> updateCampaignMessage(@PathVariable Integer campaignMessageId,
                                                   @RequestBody @Valid CampaignMessageRequestIn campaignMessageRequestIn) {
        // Business note: endpoint exists for CRUD coverage; workflow may restrict updates after the message is sent.
        campaignMessageService.updateCampaignMessage(campaignMessageId, campaignMessageRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign message updated successfully"));
    }

    @PutMapping("/mark-sent/{campaignMessageId}")
    public ResponseEntity<?> markMessageAsSent(@PathVariable Integer campaignMessageId) {
        campaignMessageService.markMessageAsSent(campaignMessageId);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign message marked as sent successfully"));
    }

    @PutMapping("/mark-answered/{campaignMessageId}")
    public ResponseEntity<?> markMessageAsAnswered(@PathVariable Integer campaignMessageId) {
        campaignMessageService.markMessageAsAnswered(campaignMessageId);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign message marked as answered successfully"));
    }

    @DeleteMapping("/deleted/{campaignMessageId}")
    public ResponseEntity<?> deleteCampaignMessage(@PathVariable Integer campaignMessageId) {
        // Business note: endpoint exists for CRUD coverage; workflow may keep sent messages for tracking and reports.
        campaignMessageService.deleteCampaignMessage(campaignMessageId);
        return ResponseEntity.status(200).body(new ApiResponse("Campaign message deleted successfully"));
    }
}
