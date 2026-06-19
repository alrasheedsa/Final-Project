package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.QRRedemptionCodeIn;
import com.example.fproject.DTO.IN.QRRedemptionRequestIn;
import com.example.fproject.Service.QRRedemptionService;
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
@RequestMapping("/api/v1/qr-redemptions")
@RequiredArgsConstructor
public class QRRedemptionController {

    private final QRRedemptionService qrRedemptionService;

    @GetMapping("/get")
    public ResponseEntity<?> getAllQRRedemptions() {
        return ResponseEntity.status(200).body(qrRedemptionService.getAllQRRedemptions());
    }

    @GetMapping("/get/{qrRedemptionId}")
    public ResponseEntity<?> getQRRedemptionById(@PathVariable Integer qrRedemptionId) {
        return ResponseEntity.status(200).body(qrRedemptionService.getQRRedemptionById(qrRedemptionId));
    }

    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<?> getRedemptionsByCampaign(@PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(qrRedemptionService.getRedemptionsByCampaign(campaignId));
    }

    @PostMapping("/redeem-by-code")
    public ResponseEntity<?> redeemByCode(@RequestBody @Valid QRRedemptionCodeIn qrRedemptionCodeIn) {
        return ResponseEntity.status(200).body(qrRedemptionService.redeemByCode(qrRedemptionCodeIn.getCode(),
                qrRedemptionCodeIn.getCustomerPhone()));
    }

    @PostMapping("/redeem-by-qr/{qrCodeId}")
    public ResponseEntity<?> redeemByQRCodeId(@PathVariable Integer qrCodeId, @RequestParam String customerPhone) {
        return ResponseEntity.status(200).body(qrRedemptionService.redeemByQRCodeId(qrCodeId, customerPhone));
    }

    @PostMapping("/cashier/redeem-code")
    public ResponseEntity<?> cashierRedeemByCode(@RequestBody @Valid QRRedemptionCodeIn qrRedemptionCodeIn) {
        return ResponseEntity.status(200).body(qrRedemptionService.cashierRedeemByCode(qrRedemptionCodeIn));
    }

    @PostMapping("/cashier/check-code/{code}")
    public ResponseEntity<?> checkQRCodeForCashier(@PathVariable String code) {
        return ResponseEntity.status(200).body(new ApiResponse(qrRedemptionService.checkQRCodeForCashier(code)));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addQRRedemption(@RequestBody @Valid QRRedemptionRequestIn qrRedemptionRequestIn) {
        qrRedemptionService.addQRRedemption(qrRedemptionRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("QR redemption added successfully"));
    }

    @PutMapping("/update/{qrRedemptionId}")
    public ResponseEntity<?> updateQRRedemption(@PathVariable Integer qrRedemptionId,
                                                @RequestBody @Valid QRRedemptionRequestIn qrRedemptionRequestIn) {
        // Business note: endpoint exists for CRUD coverage; workflow may restrict updates because redemption is a usage record.
        qrRedemptionService.updateQRRedemption(qrRedemptionId, qrRedemptionRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("QR redemption updated successfully"));
    }

    @DeleteMapping("/deleted/{qrRedemptionId}")
    public ResponseEntity<?> deleteQRRedemption(@PathVariable Integer qrRedemptionId) {
        // Business note: endpoint exists for CRUD coverage; workflow may keep QR redemption records for reports.
        qrRedemptionService.deleteQRRedemption(qrRedemptionId);
        return ResponseEntity.status(200).body(new ApiResponse("QR redemption deleted successfully"));
    }
}
