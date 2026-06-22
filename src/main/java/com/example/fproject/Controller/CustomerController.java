package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.CustomerIn;
import com.example.fproject.Model.User;
import com.example.fproject.Service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping("/register")
    public ResponseEntity<?> registerCustomer(@Valid @RequestBody CustomerIn dto) {
        customerService.registerCustomer(dto);
        return ResponseEntity.status(200).body(new ApiResponse("Customer registered successfully!"));
    }

    // ADMIN
    @GetMapping("/get")
    public ResponseEntity<?> getAllCustomers() {
        return ResponseEntity.status(200).body(customerService.getAllCustomers());
    }

    // ADMIN
    @GetMapping("/get/{customerId}")
    public ResponseEntity<?> getCustomerById(@PathVariable Integer customerId) {
        return ResponseEntity.status(200).body(customerService.getCustomerById(customerId));
    }

    // ADMIN
    @GetMapping("/get-by-phone")
    public ResponseEntity<?> getCustomerByPhone(@RequestParam String phone) {
        return ResponseEntity.status(200).body(customerService.getCustomerByPhone(phone));
    }

    // ADMIN
    @GetMapping("/inside-radius/{branchId}")
    public ResponseEntity<?> getCustomersInsideRadius(@PathVariable Integer branchId) {
        return ResponseEntity.status(200).body(customerService.getCustomersInsideRadius(branchId));
    }

    // CUSTOMER — بياناته هو
    @GetMapping("/my")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(200).body(customerService.getCustomerById(user.getId()));
    }

    // CUSTOMER
    @PutMapping("/update")
    public ResponseEntity<?> updateCustomer(@AuthenticationPrincipal User user, @Valid @RequestBody CustomerIn dto) {
        customerService.updateCustomer(user.getId(), dto);
        return ResponseEntity.status(200).body(new ApiResponse("Customer updated successfully"));
    }

    // CUSTOMER
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteCustomer(@AuthenticationPrincipal User user) {
        customerService.deleteCustomer(user.getId());
        return ResponseEntity.status(200).body(new ApiResponse("Customer deleted successfully"));
    }

    // CUSTOMER
    @GetMapping("/my/campaigns/in-radius")
    public ResponseEntity<?> getCampaignsInRadius(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(200).body(customerService.getCampaignsInRadius(user.getId()));
    }

    // CUSTOMER
    @GetMapping("/my/campaigns/active")
    public ResponseEntity<?> getActiveCampaigns(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(200).body(customerService.getActiveCampaignsInRadius(user.getId()));
    }

    // CUSTOMER
    @GetMapping("/my/campaigns/expired")
    public ResponseEntity<?> getExpiredCampaigns(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(200).body(customerService.getExpiredCampaignsInRadius(user.getId()));
    }

    // CUSTOMER
    @GetMapping("/my/campaigns/used")
    public ResponseEntity<?> getUsedCampaigns(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(200).body(customerService.getUsedCampaigns(user.getId()));
    }

    // CUSTOMER
    @GetMapping("/my/offers")
    public ResponseEntity<?> getCustomerOffers(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(200).body(customerService.getCustomerOffers(user.getId()));
    }

    // CUSTOMER
    @GetMapping("/my/campaign-messages/active")
    public ResponseEntity<?> getActiveMessages(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(200).body(customerService.getActiveMessages(user.getId()));
    }

    // CUSTOMER
    @GetMapping("/my/campaign-messages/answered")
    public ResponseEntity<?> getAnsweredMessages(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(200).body(customerService.getAnsweredMessages(user.getId()));
    }

    // CUSTOMER
    @GetMapping("/my/campaign-messages/unanswered")
    public ResponseEntity<?> getUnansweredMessages(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(200).body(customerService.getUnansweredMessages(user.getId()));
    }

    // CUSTOMER
    @GetMapping("/my/qr-codes")
    public ResponseEntity<?> getCustomerQRCodes(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(200).body(customerService.getCustomerQRCodes(user.getId()));
    }

    // CUSTOMER
    @GetMapping("/my/available-qr")
    public ResponseEntity<?> getAvailableQRCodes(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(200).body(customerService.getAvailableQRCodes(user.getId()));
    }

    // CUSTOMER
    @GetMapping("/my/used-qr")
    public ResponseEntity<?> getUsedQRCodes(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(200).body(customerService.getUsedQRCodes(user.getId()));
    }

    // CUSTOMER — يحدث موقعه
    @PutMapping("/my/update-location")
    public ResponseEntity<?> updateLocation(@AuthenticationPrincipal User user, @RequestParam String url) {
        customerService.updateLocation(user.getId(), url);
        return ResponseEntity.status(200).body(new ApiResponse("Location updated successfully"));
    }
}