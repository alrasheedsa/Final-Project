package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.CustomerIn;
import com.example.fproject.DTO.OUT.CustomerOut;
import com.example.fproject.Enum.RoleType;
import com.example.fproject.Model.Branch;
import com.example.fproject.Model.Customer;
import com.example.fproject.Model.User;
import com.example.fproject.Repository.BranchRepository;
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
    private final BranchRepository branchRepository;

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
        customer.setLocationUrl(dto.getLocationUrl());
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

        customer.setLocationUrl(dto.getLocationUrl());
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

        if (customer.getCampaignMessages() != null && !customer.getCampaignMessages().isEmpty()) {
            throw new ApiException("Cannot delete customer because it has campaign messages");
        }

        if (customer.getCustomerAnswers() != null && !customer.getCustomerAnswers().isEmpty()) {
            throw new ApiException("Cannot delete customer because it has customer answers");
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

    public CustomerOut getCustomerByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new ApiException("Phone is required");
        }

        Customer customer = customerRepository.findCustomerByUser_Phone(phone);

        if (customer == null) {
            throw new ApiException("Customer not found");
        }

        return mapToDTOOUT(customer);
    }

    public List<CustomerOut> getCustomersInsideRadius(Integer branchId) {
        Branch branch = branchRepository.findBranchById(branchId);

        if (branch == null) {
            throw new ApiException("Branch not found");
        }

        List<Customer> customers = customerRepository.findCustomersByLocationConsentTrue();
        List<CustomerOut> result = new ArrayList<>();

        for (Customer customer : customers) {
            double distance = calculateDistanceInMeters(
                    branch.getLatitude(),
                    branch.getLongitude(),
                    customer.getLatitude(),
                    customer.getLongitude()
            );

            if (distance <= branch.getCampaignRadiusMeters()) {
                result.add(mapToDTOOUT(customer));
            }
        }

        return result;
    }

    private double calculateDistanceInMeters(Double lat1, Double lon1, Double lat2, Double lon2) {
        final int earthRadiusMeters = 6371000;

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2)
                * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusMeters * c;
    }

    private CustomerOut mapToDTOOUT(Customer customer) {
        return new CustomerOut(
                customer.getId(),
                customer.getUser().getFullName(),
                customer.getUser().getPhone(),
                customer.getUser().getEmail(),
                customer.getUser().getEnabled(),
                customer.getUser().getCreatedAt(),
                customer.getLocationUrl(),
                customer.getLatitude(),
                customer.getLongitude(),
                customer.getLocationConsent()
        );
    }
}