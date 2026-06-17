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
        return ResponseEntity.status(200).body(customerService.registerCustomer(dto));
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
        return ResponseEntity.status(200).body(customerService.updateCustomer(customerId, dto));
    }

    @DeleteMapping("/delete/{customerId}")
    public ResponseEntity<?> deleteCustomer(@PathVariable Integer customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseEntity.status(200).body(new ApiResponse("Customer deleted successfully"));
    }
}