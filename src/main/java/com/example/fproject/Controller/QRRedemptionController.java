package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.QRRedemptionCodeIn;
import com.example.fproject.DTO.IN.QRRedemptionRequestIn;
import com.example.fproject.Model.User;
import com.example.fproject.Service.QRRedemptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/qr-redemptions")
@RequiredArgsConstructor
public class QRRedemptionController {

    private final QRRedemptionService qrRedemptionService;

    // ADMIN
    @GetMapping("/get")
    public ResponseEntity<?> getAllQRRedemptions() {
        return ResponseEntity.status(200).body(qrRedemptionService.getAllQRRedemptions());
    }

    // STORE_OWNER
    @GetMapping("/get/{qrRedemptionId}")
    public ResponseEntity<?> getQRRedemptionById(@AuthenticationPrincipal User user, @PathVariable Integer qrRedemptionId) {
        return ResponseEntity.status(200).body(qrRedemptionService.getQRRedemptionById(user.getId(), qrRedemptionId));
    }

    // STORE_OWNER
    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<?> getRedemptionsByCampaign(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(qrRedemptionService.getRedemptionsByCampaign(user.getId(), campaignId));
    }

    // CUSTOMER — يسترد QR بالكود
    @PostMapping("/redeem-by-code")
    public ResponseEntity<?> redeemByCode(@RequestBody @Valid QRRedemptionCodeIn qrRedemptionCodeIn) {
        return ResponseEntity.status(200).body(qrRedemptionService.redeemByCode(
                qrRedemptionCodeIn.getCode(), qrRedemptionCodeIn.getCustomerPhone()));
    }

    // CUSTOMER — يسترد QR بالـ ID
    @PostMapping("/redeem-by-qr/{qrCodeId}")
    public ResponseEntity<?> redeemByQRCodeId(@PathVariable Integer qrCodeId, @RequestParam String customerPhone) {
        return ResponseEntity.status(200).body(qrRedemptionService.redeemByQRCodeId(qrCodeId, customerPhone));
    }

    // STORE_OWNER — الكاشير يسترد كود العميل
    @PostMapping("/cashier/redeem-code")
    public ResponseEntity<?> cashierRedeemByCode(@AuthenticationPrincipal User user, @RequestBody @Valid QRRedemptionCodeIn qrRedemptionCodeIn) {
        return ResponseEntity.status(200).body(qrRedemptionService.cashierRedeemByCode(user.getId(), qrRedemptionCodeIn));
    }

    // STORE_OWNER — الكاشير يتحقق من كود
    @PostMapping("/cashier/check-code/{code}")
    public ResponseEntity<?> checkQRCodeForCashier(@AuthenticationPrincipal User user, @PathVariable String code) {
        return ResponseEntity.status(200).body(new ApiResponse(qrRedemptionService.checkQRCodeForCashier(user.getId(), code)));
    }

    // STORE_OWNER
    @PostMapping("/add")
    public ResponseEntity<?> addQRRedemption(@AuthenticationPrincipal User user, @RequestBody @Valid QRRedemptionRequestIn qrRedemptionRequestIn) {
        qrRedemptionService.addQRRedemption(user.getId(), qrRedemptionRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("QR redemption added successfully"));
    }

    // STORE_OWNER
    @PutMapping("/update/{qrRedemptionId}")
    public ResponseEntity<?> updateQRRedemption(@AuthenticationPrincipal User user,
                                                @PathVariable Integer qrRedemptionId,
                                                @RequestBody @Valid QRRedemptionRequestIn qrRedemptionRequestIn) {
        qrRedemptionService.updateQRRedemption(user.getId(), qrRedemptionId, qrRedemptionRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("QR redemption updated successfully"));
    }

    // STORE_OWNER
    @DeleteMapping("/deleted/{qrRedemptionId}")
    public ResponseEntity<?> deleteQRRedemption(@AuthenticationPrincipal User user, @PathVariable Integer qrRedemptionId) {
        qrRedemptionService.deleteQRRedemption(user.getId(), qrRedemptionId);
        return ResponseEntity.status(200).body(new ApiResponse("QR redemption deleted successfully"));
    }
}