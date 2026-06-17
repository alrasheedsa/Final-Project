package com.example.fproject.Controller;

import com.example.fproject.Api.ApiResponse;
import com.example.fproject.DTO.IN.CustomerAnswerRequestIn;
import com.example.fproject.Service.CustomerAnswerService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer-answers")
@RequiredArgsConstructor
public class CustomerAnswerController {

    private final CustomerAnswerService customerAnswerService;

    @GetMapping("/get")
    public ResponseEntity<?> getAllCustomerAnswers() {
        return ResponseEntity.status(200).body(customerAnswerService.getAllCustomerAnswers());
    }

    @GetMapping("/get/{customerAnswerId}")
    public ResponseEntity<?> getCustomerAnswerById(@PathVariable Integer customerAnswerId) {
        return ResponseEntity.status(200).body(customerAnswerService.getCustomerAnswerById(customerAnswerId));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addCustomerAnswer(@RequestBody @Valid CustomerAnswerRequestIn customerAnswerRequestIn) {
        customerAnswerService.addCustomerAnswer(customerAnswerRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("Customer answer added successfully"));
    }

    @PutMapping("/update/{customerAnswerId}")
    public ResponseEntity<?> updateCustomerAnswer(@PathVariable Integer customerAnswerId,
                                                  @RequestBody @Valid CustomerAnswerRequestIn customerAnswerRequestIn) {
        // Business note: endpoint exists for CRUD coverage; workflow may restrict answer updates after submission.
        customerAnswerService.updateCustomerAnswer(customerAnswerId, customerAnswerRequestIn);
        return ResponseEntity.status(200).body(new ApiResponse("Customer answer updated successfully"));
    }

    @DeleteMapping("/deleted/{customerAnswerId}")
    public ResponseEntity<?> deleteCustomerAnswer(@PathVariable Integer customerAnswerId) {
        // Business note: endpoint exists for CRUD coverage; workflow may keep customer answers for campaign evaluation.
        customerAnswerService.deleteCustomerAnswer(customerAnswerId);
        return ResponseEntity.status(200).body(new ApiResponse("Customer answer deleted successfully"));
    }
}
