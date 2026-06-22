package com.example.fproject.Mohammed_JunitTest;

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
    @Autowired CampaignRepository campaignRepository;
    @Autowired QRCodeRepository qrCodeRepository;
    @Autowired CampaignMessageRepository campaignMessageRepository;

    User ownerUser, customerUser;
    StoreOwner storeOwner;
    Customer customer;
    Store store;
    Branch branch;
    Campaign campaign;
    QRCode qrCode;
    CampaignMessage campaignMessage;

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
        customer.setLocationConsent(true);
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

        campaign = new Campaign();
        campaign.setTitle("Summer Coffee Offer_" + uniqueSuffix);
        campaign.setDescription("Test campaign description");
        campaign.setOfferText("25% off on specialty coffee");
        campaign.setCampaignType(CampaignType.DIRECT_OFFER);
        campaign.setStartDateTime(LocalDateTime.now().minusHours(1));
        campaign.setEndDateTime(LocalDateTime.now().plusDays(1));
        campaign.setTargetCustomersCount(100);
        campaign.setSentCount(0);
        campaign.setRedeemedCount(0);
        campaign.setStatus(CampaignStatus.ACTIVE);
        campaign.setBranch(branch);
        campaignRepository.save(campaign);

        qrCode = new QRCode();
        qrCode.setCode("QR-" + uniqueSuffix);
        qrCode.setQrImageBase64("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA");
        qrCode.setMaxUsageCount(100);
        qrCode.setUsedCount(0);
        qrCode.setStatus(QRCodeStatus.ACTIVE);
        qrCode.setCampaign(campaign);
        qrCodeRepository.save(qrCode);

        campaignMessage = new CampaignMessage();
        campaignMessage.setMessageText("You have a new offer from CoffeeTahaf");
        campaignMessage.setDistanceKm(1.5);
        campaignMessage.setDurationMinutes(8);
        campaignMessage.setDistanceText("1.50 km");
        campaignMessage.setStatus(MessageStatus.SENT);
        campaignMessage.setSentAt(LocalDateTime.now());
        campaignMessage.setCampaign(campaign);
        campaignMessage.setCustomer(customer);
        campaignMessageRepository.save(campaignMessage);
    }

    @AfterEach
    void tearDown() {
        campaignMessageRepository.delete(campaignMessage);
        qrCodeRepository.delete(qrCode);
        campaignRepository.delete(campaign);
        branchRepository.delete(branch);
        storeRepository.delete(store);
        customerRepository.delete(customer);
        storeOwnerRepository.delete(storeOwner);
        userRepository.delete(ownerUser);
        userRepository.delete(customerUser);
    }

    @Test
    public void findAllByBranchIdAndStatusTest() {
        List<Campaign> result = campaignRepository
                .findAllByBranchIdAndStatus(branch.getId(), CampaignStatus.ACTIVE);

        Assertions.assertThat(result).isNotEmpty();
        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result.get(0).getStatus()).isEqualTo(CampaignStatus.ACTIVE);
        Assertions.assertThat(result.get(0).getTitle()).isEqualTo(campaign.getTitle());
    }

    @Test
    public void findQRCodeByCampaignIdTest() {
        QRCode found = qrCodeRepository.findQRCodeByCampaignId(campaign.getId());

        Assertions.assertThat(found).isNotNull();
        Assertions.assertThat(found.getCode()).isEqualTo(qrCode.getCode());
        Assertions.assertThat(found.getStatus()).isEqualTo(QRCodeStatus.ACTIVE);
    }

    @Test
    public void findAllByCampaignIdTest() {
        List<CampaignMessage> result = campaignMessageRepository
                .findAllByCampaignId(campaign.getId());

        Assertions.assertThat(result).isNotEmpty();
        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result.get(0).getStatus()).isEqualTo(MessageStatus.SENT);
        Assertions.assertThat(result.get(0).getCustomer().getId()).isEqualTo(customer.getId());
    }
}
