package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.CustomerIn;
import com.example.fproject.DTO.OUT.CustomerOut;
import com.example.fproject.Enum.RoleType;
import com.example.fproject.Model.Customer;
import com.example.fproject.Model.User;
import com.example.fproject.Repository.CustomerRepository;
import com.example.fproject.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final GoogleMapService googleMapService;

    public CustomerOut registerCustomer(CustomerIn dto) {

        if (userRepository.existsUserByEmail(dto.getEmail())) {
            throw new ApiException("Email already exists");
        }

        if (userRepository.existsUserByPhone(dto.getPhone())) {
            throw new ApiException("Phone already exists");
        }

        double[] coordinates = googleMapService.extractLocationFromLink(dto.getLocationUrl());

        User user = new User();
        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setRole(RoleType.CUSTOMER);
        user.setEnabled(true);

        userRepository.save(user);

        Customer customer = new Customer();
        customer.setUser(user);
        customer.setLatitude(coordinates[0]);
        customer.setLongitude(coordinates[1]);
        customer.setLocationConsent(dto.getLocationConsent());

        customerRepository.save(customer);

        return mapToDTOOUT(customer);
    }

    public List<CustomerOut> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll();
        List<CustomerOut> result = new ArrayList<>();

        for (Customer customer : customers) {
            result.add(mapToDTOOUT(customer));
        }

        return result;
    }

    public CustomerOut getCustomerById(Integer customerId) {
        Customer customer = customerRepository.findCustomerById(customerId);

        if (customer == null) {
            throw new ApiException("Customer not found");
        }

        return mapToDTOOUT(customer);
    }

    public CustomerOut updateCustomer(Integer customerId, CustomerIn dto) {
        Customer customer = customerRepository.findCustomerById(customerId);

        if (customer == null) {
            throw new ApiException("Customer not found");
        }

        User user = customer.getUser();

        if (!user.getEmail().equals(dto.getEmail()) && userRepository.existsUserByEmail(dto.getEmail())) {
            throw new ApiException("Email already exists");
        }

        if (!user.getPhone().equals(dto.getPhone()) && userRepository.existsUserByPhone(dto.getPhone())) {
            throw new ApiException("Phone already exists");
        }

        double[] coordinates = googleMapService.extractLocationFromLink(dto.getLocationUrl());

        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());

        userRepository.save(user);

        customer.setLatitude(coordinates[0]);
        customer.setLongitude(coordinates[1]);
        customer.setLocationConsent(dto.getLocationConsent());

        customerRepository.save(customer);

        return mapToDTOOUT(customer);
    }

    public void deleteCustomer(Integer customerId) {
        Customer customer = customerRepository.findCustomerById(customerId);

        if (customer == null) {
            throw new ApiException("Customer not found");
        }

        User user = customer.getUser();

        customerRepository.delete(customer);
        userRepository.delete(user);
    }

    public List<CustomerOut> getCustomersWithLocationConsent() {
        List<Customer> customers = customerRepository.findCustomersByLocationConsentTrue();
        List<CustomerOut> result = new ArrayList<>();

        for (Customer customer : customers) {
            result.add(mapToDTOOUT(customer));
        }

        return result;
    }

    private CustomerOut mapToDTOOUT(Customer customer) {
        return new CustomerOut(
                customer.getId(),
                customer.getUser().getFullName(),
                customer.getUser().getPhone(),
                customer.getUser().getEmail(),
                customer.getUser().getEnabled(),
                customer.getUser().getCreatedAt(),
                customer.getLatitude(),
                customer.getLongitude(),
                customer.getLocationConsent()
        );
    }
}