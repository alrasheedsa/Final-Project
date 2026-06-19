package com.example.fproject.Repository;

import com.example.fproject.Model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    Customer findCustomerById(Integer id);

    List<Customer> findCustomersByLocationConsentTrue();

    Customer findCustomerByUser_Phone(String phone);
}