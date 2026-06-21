package com.example.fproject.Rahaf_JunitTest;
import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.CustomerIn;
import com.example.fproject.DTO.OUT.SubscriptionStatusOut;
import com.example.fproject.Enum.*;
import com.example.fproject.Model.*;
import com.example.fproject.Repository.*;
import com.example.fproject.Service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServiceTest {


    @Mock UserRepository userRepository;


    @InjectMocks CustomerService customerService;

    @Mock StoreOwnerRepository   storeOwnerRepository;
    @Mock SubscriptionRepository subscriptionRepository;

    @InjectMocks SubscriptionService subscriptionService;

    User ownerUser, customerUser;
    StoreOwner storeOwner;
    Customer customer;
    Subscription subscription;

    @BeforeEach
    void setUp() {
        ownerUser = new User();
        ownerUser.setId(1);
        ownerUser.setFullName("Sara AlQahtani");
        ownerUser.setPhone("0512345678");
        ownerUser.setEmail("sara@business.sa");
        ownerUser.setPassword("Sara@2026!");
        ownerUser.setRole(RoleType.STORE_OWNER);
        ownerUser.setEnabled(true);

        customerUser = new User();
        customerUser.setId(2);
        customerUser.setFullName("Nouf AlShamri");
        customerUser.setPhone("0501112233");
        customerUser.setEmail("nouf@gmail.com");
        customerUser.setPassword("Nouf@1234");
        customerUser.setRole(RoleType.CUSTOMER);
        customerUser.setEnabled(true);

        storeOwner = new StoreOwner();
        storeOwner.setUser(ownerUser);

        customer = new Customer();
        customer.setId(2);
        customer.setUser(customerUser);
        customer.setLatitude(24.6900);
        customer.setLongitude(46.7200);
        customer.setLocationConsent(true);
        customer.setLocationUrl("https://maps.google.com/?q=24.6900,46.7200");

        subscription = new Subscription();
        subscription.setId(1);
        subscription.setPlanType(SubscriptionPlanType.BASIC_MONTHLY);
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusMonths(1));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStoreOwner(storeOwner);
    }

    @Test
    public void registerCustomer_DuplicateEmail_ThrowsException() {
        when(userRepository.existsUserByEmail("nouf@gmail.com")).thenReturn(true);

        CustomerIn dto = new CustomerIn();
        dto.setEmail("nouf@gmail.com");
        dto.setPhone("0501112233");

        assertThatThrownBy(() -> customerService.registerCustomer(dto))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    public void getSubscriptionStatus_Active_ReturnsCorrectStatus() {
        when(storeOwnerRepository.findStoreOwnerById(1)).thenReturn(storeOwner);
        when(subscriptionRepository.findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(
                1, SubscriptionStatus.ACTIVE)).thenReturn(subscription);

        SubscriptionStatusOut result = subscriptionService.getSubscriptionStatus(1);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(result.getPlanType()).isEqualTo(SubscriptionPlanType.BASIC_MONTHLY);
        assertThat(result.getDaysRemaining()).isGreaterThan(0);
        assertThat(result.getIsExpired()).isFalse();
    }

    @Test
    public void getSubscriptionStatus_NoSubscription_ThrowsException() {
        when(storeOwnerRepository.findStoreOwnerById(1)).thenReturn(storeOwner);
        when(subscriptionRepository.findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(
                1, SubscriptionStatus.ACTIVE)).thenReturn(null);
        when(subscriptionRepository.findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(
                1, SubscriptionStatus.PENDING)).thenReturn(null);

        assertThatThrownBy(() -> subscriptionService.getSubscriptionStatus(1))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No subscription found");
    }

}
