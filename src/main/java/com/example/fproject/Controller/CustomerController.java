package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.CustomerIn;
import com.example.fproject.Service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping("/register")
    public ResponseEntity<?> registerCustomer(@Valid @RequestBody CustomerIn dto) {
        customerService.registerCustomer(dto);
        return ResponseEntity.status(200).body(new ApiResponse("Customer registered successfully"));
    }

    @GetMapping("/get")
    public ResponseEntity<?> getAllCustomers() {
        return ResponseEntity.status(200).body(customerService.getAllCustomers());
    }

    @GetMapping("/get/{customerId}")
    public ResponseEntity<?> getCustomerById(@PathVariable Integer customerId) {
        return ResponseEntity.status(200).body(customerService.getCustomerById(customerId));
    }

    @GetMapping("/location-consent")
    public ResponseEntity<?> getCustomersWithLocationConsent() {
        return ResponseEntity.status(200).body(customerService.getCustomersWithLocationConsent());
    }

    @PutMapping("/update/{customerId}")
    public ResponseEntity<?> updateCustomer(@PathVariable Integer customerId, @Valid @RequestBody CustomerIn dto) {
        customerService.updateCustomer(customerId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Customer updated successfully"));
    }

    @DeleteMapping("/delete/{customerId}")
    public ResponseEntity<?> deleteCustomer(@PathVariable Integer customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseEntity.status(200).body(new ApiResponse("Customer deleted successfully"));
    }

    @GetMapping("/get-by-phone")
    public ResponseEntity<?> getCustomerByPhone(@RequestParam String phone) {
        return ResponseEntity.status(200).body(customerService.getCustomerByPhone(phone));
    }

    @GetMapping("/inside-radius/{branchId}")
    public ResponseEntity<?> getCustomersInsideRadius(@PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(customerService.getCustomersInsideRadius(branchId));
    }

    @GetMapping("/{customerId}/campaigns/in-radius")
    public ResponseEntity<?> getCampaignsInRadius(@PathVariable Integer customerId) {
        return ResponseEntity.status(200).body(customerService.getCampaignsInRadius(customerId));
    }

    @GetMapping("/{customerId}/campaigns/active")
    public ResponseEntity<?> getActiveCampaigns(@PathVariable Integer customerId) {
        return ResponseEntity.status(200).body(customerService.getActiveCampaignsInRadius(customerId));
    }

    @GetMapping("/{customerId}/campaigns/expired")
    public ResponseEntity<?> getExpiredCampaigns(@PathVariable Integer customerId) {
        return ResponseEntity.status(200).body(customerService.getExpiredCampaignsInRadius(customerId));
    }

    @GetMapping("/{customerId}/campaigns/used")
    public ResponseEntity<?> getUsedCampaigns(@PathVariable Integer customerId) {
        return ResponseEntity.status(200).body(customerService.getUsedCampaigns(customerId));
    }

    @GetMapping("/{customerId}/offers")
    public ResponseEntity<?> getCustomerOffers(@PathVariable Integer customerId) {
        return ResponseEntity.status(200).body(customerService.getCustomerOffers(customerId));
    }

    @GetMapping("/{customerId}/campaign-messages/active")
    public ResponseEntity<?> getActiveMessages(@PathVariable Integer customerId) {
        return ResponseEntity.status(200).body(customerService.getActiveMessages(customerId));
    }

    @GetMapping("/{customerId}/campaign-messages/answered")
    public ResponseEntity<?> getAnsweredMessages(@PathVariable Integer customerId) {
        return ResponseEntity.status(200).body(customerService.getAnsweredMessages(customerId));
    }

    @GetMapping("/{customerId}/campaign-messages/unanswered")
    public ResponseEntity<?> getUnansweredMessages(@PathVariable Integer customerId) {
        return ResponseEntity.status(200).body(customerService.getUnansweredMessages(customerId));
    }

    @GetMapping("/{customerId}/qr-codes")
    public ResponseEntity<?> getCustomerQRCodes(@PathVariable Integer customerId) {
        return ResponseEntity.status(200).body(customerService.getCustomerQRCodes(customerId));
    }

    @GetMapping("/{customerId}/available-qr")
    public ResponseEntity<?> getAvailableQRCodes(@PathVariable Integer customerId) {
        return ResponseEntity.status(200).body(customerService.getAvailableQRCodes(customerId));
    }

    @GetMapping("/{customerId}/used-qr")
    public ResponseEntity<?> getUsedQRCodes(@PathVariable Integer customerId) {
        return ResponseEntity.status(200).body(customerService.getUsedQRCodes(customerId));
    }
}