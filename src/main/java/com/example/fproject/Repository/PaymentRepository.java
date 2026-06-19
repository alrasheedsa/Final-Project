package com.example.fproject.Repository;

import com.example.fproject.Enum.PaymentStatus;
import com.example.fproject.Model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    Payment findPaymentById(Integer id);

    Payment findPaymentByTransactionId(String transactionId);

    List<Payment> findPaymentsBySubscriptionId(Integer subscriptionId);

    List<Payment> findPaymentsByStatus(PaymentStatus status);

}