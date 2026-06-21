package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.QRCodeRequestIn;
import com.example.fproject.Model.User;
import com.example.fproject.Service.QRCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/qr-codes")
@RequiredArgsConstructor
public class QRCodeController {

    private final QRCodeService qrCodeService;

    // ADMIN
    @GetMapping("/get")
    public ResponseEntity<?> getAllQRCodes() {
        return ResponseEntity.status(200).body(qrCodeService.getAllQRCodes());
    }

    // STORE_OWNER
    @GetMapping("/get/{qrCodeId}")
    public ResponseEntity<?> getQRCodeById(@AuthenticationPrincipal User user, @PathVariable Integer qrCodeId) {
        return ResponseEntity.status(200).body(qrCodeService.getQRCodeById(user.getId(), qrCodeId));
    }

    // STORE_OWNER
    @GetMapping("/code")
    public ResponseEntity<?> getQRCodeByCode(@AuthenticationPrincipal User user, @RequestParam String code) {
        return ResponseEntity.status(200).body(qrCodeService.getQRCodeByCode(user.getId(), code));
    }

    // STORE_OWNER
    @GetMapping(value = "/image/{qrCodeId}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<?> getQRCodeImage(@AuthenticationPrincipal User user, @PathVariable Integer qrCodeId) {
        return ResponseEntity.status(200).contentType(MediaType.IMAGE_PNG).body(qrCodeService.getQRCodeImage(user.getId(), qrCodeId));
    }

    // STORE_OWNER
    @PostMapping("/generate/{campaignId}")
    public ResponseEntity<?> generateQRCode(@AuthenticationPrincipal User user, @PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(qrCodeService.generateQRCode(user.getId(), campaignId));
    }

    // STORE_OWNER
    @PostMapping("/add")
    public ResponseEntity<?> addQRCode(@AuthenticationPrincipal User user, @RequestBody @Valid QRCodeRequestIn qrCodeRequestIn) {
        qrCodeService.addQRCode(user.getId(), qrCodeRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("QR code added successfully"));
    }

    // STORE_OWNER
    @PutMapping("/update/{qrCodeId}")
    public ResponseEntity<?> updateQRCode(@AuthenticationPrincipal User user,
                                          @PathVariable Integer qrCodeId,
                                          @RequestBody @Valid QRCodeRequestIn qrCodeRequestIn) {
        qrCodeService.updateQRCode(user.getId(), qrCodeId, qrCodeRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("QR code updated successfully"));
    }

    // STORE_OWNER
    @DeleteMapping("/deleted/{qrCodeId}")
    public ResponseEntity<?> deleteQRCode(@AuthenticationPrincipal User user, @PathVariable Integer qrCodeId) {
        qrCodeService.deleteQRCode(user.getId(), qrCodeId);
        return ResponseEntity.status(200).body(new ApiResponse("QR code deleted successfully"));
    }
}