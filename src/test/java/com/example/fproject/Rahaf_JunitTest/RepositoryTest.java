package com.example.fproject.Rahaf_JunitTest;


import com.example.fproject.Enum.*;
import com.example.fproject.Model.*;
import com.example.fproject.Repository.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RepositoryTest {


    @Autowired UserRepository userRepository;
    @Autowired StoreOwnerRepository storeOwnerRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired StoreRepository storeRepository;
    @Autowired BranchRepository branchRepository;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired MonthlyReportRepository monthlyReportRepository;

    User ownerUser, customerUser;
    StoreOwner storeOwner;
    Customer customer;
    Store store;
    Branch branch;
    Subscription subscription;
    Payment payment;
    MonthlyReport report;

    String uniqueSuffix = String.valueOf(System.currentTimeMillis());

    @BeforeEach
    void setUp() {
        ownerUser = new User();
        ownerUser.setFullName("Sara AlQahtani");
        ownerUser.setPhone("051" + uniqueSuffix.substring(uniqueSuffix.length() - 7));
        ownerUser.setEmail("sara_" + uniqueSuffix + "@business.sa");
        ownerUser.setPassword("Sara@2026!");
        ownerUser.setRole(RoleType.STORE_OWNER);
        ownerUser.setEnabled(true);
        userRepository.save(ownerUser);

        customerUser = new User();
        customerUser.setFullName("Nouf AlShamri");
        customerUser.setPhone("050" + uniqueSuffix.substring(uniqueSuffix.length() - 7));
        customerUser.setEmail("nouf_" + uniqueSuffix + "@gmail.com");
        customerUser.setPassword("Nouf@1234");
        customerUser.setRole(RoleType.CUSTOMER);
        customerUser.setEnabled(true);
        userRepository.save(customerUser);

        storeOwner = new StoreOwner();
        storeOwner.setUser(ownerUser);
        storeOwnerRepository.save(storeOwner);

        customer = new Customer();
        customer.setUser(customerUser);
        customer.setLocationUrl("https://maps.google.com/?q=24.6900,46.7200");
        customer.setLatitude(24.6900);
        customer.setLongitude(46.7200);
        customerRepository.save(customer);

        store = new Store();
        store.setName("CoffeeTahaf_" + uniqueSuffix);
        store.setBusinessType("Cafe");
        store.setCommercialRegisterNo(uniqueSuffix.substring(uniqueSuffix.length() - 10));
        store.setCommercialRegisterVerified(true);
        store.setStatus(StoreStatus.ACTIVE);
        store.setStoreOwner(storeOwner);
        storeRepository.save(store);

        branch = new Branch();
        branch.setName("Riyadh-Alalia");
        branch.setLocationUrl("https://maps.google.com/?q=24.6877,46.7219");
        branch.setLatitude(24.6877);
        branch.setLongitude(46.7219);
        branch.setCampaignRadiusMeters(3000);
        branch.setOpeningTime("10:00");
        branch.setClosingTime("23:00");
        branch.setStatus(StoreStatus.ACTIVE);
        branch.setStore(store);
        branchRepository.save(branch);

        subscription = new Subscription();
        subscription.setPlanType(SubscriptionPlanType.BASIC_MONTHLY);
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusMonths(1));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStoreOwner(storeOwner);
        subscriptionRepository.save(subscription);

        payment = new Payment();
        payment.setAmount(99.0);
        payment.setTransactionId("LS-ORDER-" + uniqueSuffix);
        payment.setPaymentProvider("LEMON_SQUEEZY");
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setSubscription(subscription);
        paymentRepository.save(payment);

        report = new MonthlyReport();
        report.setMonth(6);
        report.setYear(2026);
        report.setTotalSales(3286.0);
        report.setTotalQuantity(199);
        report.setTopProducts("Iced Americano (1232.00 SAR), Latte (1224.00 SAR)");
        report.setLowProducts("Brownie (50.00 SAR), Croissant (84.00 SAR)");
        report.setPeakHours("10:00 (70 units)");
        report.setSlowHours("17:00 (2 units)");
        report.setSurplusProducts("Brownie, Croissant");
        report.setAiSummary("Strong performance in June 2026.");
        report.setPdfUrl("/api/v1/monthly-report/download/1");
        report.setGeneratedAt(LocalDateTime.now());
        report.setBranch(branch);
        monthlyReportRepository.save(report);
    }

    @AfterEach
    void tearDown() {
        monthlyReportRepository.delete(report);
        paymentRepository.delete(payment);
        subscriptionRepository.delete(subscription);
        branchRepository.delete(branch);
        storeRepository.delete(store);
        customerRepository.delete(customer);
        storeOwnerRepository.delete(storeOwner);
        userRepository.delete(ownerUser);
        userRepository.delete(customerUser);
    }

    @Test
    public void findStoreOwnerByUserIdTest() {
        StoreOwner found = storeOwnerRepository.findStoreOwnerByUserId(ownerUser.getId());

        Assertions.assertThat(found).isNotNull();
        Assertions.assertThat(found.getUser().getEmail()).isEqualTo(ownerUser.getEmail());
    }

    @Test
    public void findCustomerByPhoneTest() {
        Customer result = customerRepository.findCustomerByUser_Phone(customerUser.getPhone());

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getUser().getPhone()).isEqualTo(customerUser.getPhone());
    }

    @Test
    public void findMonthlyReportByBranchAndMonthAndYearTest() {
        MonthlyReport found = monthlyReportRepository
                .findMonthlyReportByBranchIdAndMonthAndYear(branch.getId(), 6, 2026);

        Assertions.assertThat(found).isNotNull();
        Assertions.assertThat(found.getTotalSales()).isEqualTo(3286.0);
        Assertions.assertThat(found.getMonth()).isEqualTo(6);
        Assertions.assertThat(found.getYear()).isEqualTo(2026);
    }
}
