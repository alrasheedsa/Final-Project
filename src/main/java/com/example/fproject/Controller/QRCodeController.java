package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.QRCodeRequestIn;
import com.example.fproject.Service.QRCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
@RequestMapping("/api/v1/qr-codes")
@RequiredArgsConstructor
public class QRCodeController {

    private final QRCodeService qrCodeService;

    @GetMapping("/get")
    public ResponseEntity<?> getAllQRCodes() {
        return ResponseEntity.status(200).body(qrCodeService.getAllQRCodes());
    }

    @GetMapping("/get/{qrCodeId}")
    public ResponseEntity<?> getQRCodeById(@PathVariable Integer qrCodeId) {
        return ResponseEntity.status(200).body(qrCodeService.getQRCodeById(qrCodeId));
    }

    @GetMapping("/code")
    public ResponseEntity<?> getQRCodeByCode(@RequestParam String code) {
        return ResponseEntity.status(200).body(qrCodeService.getQRCodeByCode(code));
    }

    @GetMapping(value = "/image/{qrCodeId}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<?> getQRCodeImage(@PathVariable Integer qrCodeId) {
        return ResponseEntity.status(200).contentType(MediaType.IMAGE_PNG).body(qrCodeService.getQRCodeImage(qrCodeId));
    }

    @PostMapping("/generate/{campaignId}")
    public ResponseEntity<?> generateQRCode(@PathVariable Integer campaignId) {
        return ResponseEntity.status(200).body(qrCodeService.generateQRCode(campaignId));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addQRCode(@RequestBody @Valid QRCodeRequestIn qrCodeRequestIn) {
        qrCodeService.addQRCode(qrCodeRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("QR code added successfully"));
    }

    @PutMapping("/update/{qrCodeId}")
    public ResponseEntity<?> updateQRCode(@PathVariable Integer qrCodeId,
                                          @RequestBody @Valid QRCodeRequestIn qrCodeRequestIn) {
        // Business note: endpoint exists for CRUD coverage; workflow may restrict updates after QR code is sent or used.
        qrCodeService.updateQRCode(qrCodeId, qrCodeRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("QR code updated successfully"));
    }

    @DeleteMapping("/deleted/{qrCodeId}")
    public ResponseEntity<?> deleteQRCode(@PathVariable Integer qrCodeId) {
        // Business note: endpoint exists for CRUD coverage; workflow may update status instead of hard delete.
        qrCodeService.deleteQRCode(qrCodeId);
        return ResponseEntity.status(200).body(new ApiResponse("QR code deleted successfully"));
    }
}
