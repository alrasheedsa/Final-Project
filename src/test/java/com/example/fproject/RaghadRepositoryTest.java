package com.example.fproject;

import com.example.fproject.Enum.CampaignType;
import com.example.fproject.Enum.RoleType;
import com.example.fproject.Enum.StoreStatus;
import com.example.fproject.Enum.SuggestionStatus;
import com.example.fproject.Model.*;
import com.example.fproject.Repository.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RaghadRepositoryTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    StoreOwnerRepository storeOwnerRepository;

    @Autowired
    StoreRepository storeRepository;

    @Autowired
    BranchRepository branchRepository;

    @Autowired
    SalesRecordRepository salesRecordRepository;

    @Autowired
    SalesRecordItemRepository salesRecordItemRepository;

    @Autowired
    AIAnalysisRepository aiAnalysisRepository;

    @Autowired
    CampaignSuggestionRepository campaignSuggestionRepository;

    User user;
    StoreOwner storeOwner;
    Store store;
    Branch branch;
    SalesRecord salesRecord;
    SalesRecordItem item1;
    SalesRecordItem item2;
    AIAnalysis aiAnalysis;
    CampaignSuggestion suggestion1;
    CampaignSuggestion suggestion2;

    @BeforeEach
    void setUp() {
        String unique = String.valueOf(System.nanoTime());

        user = new User();
        user.setFullName("Raghad Test");
        user.setPhone("05" + unique);
        user.setEmail("raghad" + unique + "@test.com");
        user.setPassword("123456");
        user.setRole(RoleType.STORE_OWNER);
        user.setEnabled(true);
        userRepository.save(user);

        storeOwner = new StoreOwner();
        storeOwner.setUser(user);
        storeOwnerRepository.save(storeOwner);

        store = new Store();
        store.setName("Test Coffee " + unique);
        store.setBusinessType("Cafe");
        store.setCommercialRegisterNo("CR" + unique);
        store.setCommercialRegisterVerified(true);
        store.setStatus(StoreStatus.ACTIVE);
        store.setStoreOwner(storeOwner);
        storeRepository.save(store);

        branch = new Branch();
        branch.setName("Test Branch " + unique);
        branch.setLocationUrl("https://maps.google.com/?q=24.6900,46.7200");
        branch.setLatitude(24.6900);
        branch.setLongitude(46.7200);
        branch.setStatus(StoreStatus.ACTIVE);
        branch.setCampaignRadiusMeters(1000);
        branch.setRecommendedRadiusMeters(800);
        branch.setOpeningTime("08:00");
        branch.setClosingTime("23:00");
        branch.setStore(store);
        branchRepository.save(branch);

        salesRecord = new SalesRecord();
        salesRecord.setUploadedAt(LocalDateTime.now());
        salesRecord.setFileName("sales.xlsx");
        salesRecord.setFileUrl("uploads/sales.xlsx");
        salesRecord.setMonth(6);
        salesRecord.setYear(2026);
        salesRecord.setBranch(branch);
        salesRecordRepository.save(salesRecord);

        item1 = new SalesRecordItem();
        item1.setProductName("Latte");
        item1.setQuantity(10);
        item1.setUnitPrice(12.0);
        item1.setSaleDate(LocalDate.of(2026, 6, 1));
        item1.setSaleTime(LocalTime.of(9, 0));
        item1.setSalesRecord(salesRecord);
        salesRecordItemRepository.save(item1);

        item2 = new SalesRecordItem();
        item2.setProductName("Cookie");
        item2.setQuantity(5);
        item2.setUnitPrice(8.0);
        item2.setSaleDate(LocalDate.of(2026, 6, 1));
        item2.setSaleTime(LocalTime.of(15, 0));
        item2.setSalesRecord(salesRecord);
        salesRecordItemRepository.save(item2);

        aiAnalysis = new AIAnalysis();
        aiAnalysis.setTopProducts("Latte");
        aiAnalysis.setLowProducts("Cookie");
        aiAnalysis.setPeakHours("09:00-11:00");
        aiAnalysis.setSlowHours("15:00-17:00");
        aiAnalysis.setSurplusProducts("Cookie");
        aiAnalysis.setSeasonalPatterns("No seasonal pattern");
        aiAnalysis.setRecommendation("Create an offer during slow hours");
        aiAnalysis.setAiSummary("Sales analysis summary");
        aiAnalysis.setAnalyzedAt(LocalDateTime.now());
        aiAnalysis.setSalesRecord(salesRecord);
        aiAnalysisRepository.save(aiAnalysis);

        suggestion1 = new CampaignSuggestion();
        suggestion1.setTitle("Latte Offer");
        suggestion1.setDescription("Discount campaign");
        suggestion1.setOfferText("20% off Latte");
        suggestion1.setCampaignType(CampaignType.DIRECT_OFFER);
        suggestion1.setSuggestedStartDate(LocalDate.of(2026, 6, 20));
        suggestion1.setSuggestedEndDate(LocalDate.of(2026, 6, 20));
        suggestion1.setSuggestedStartTime(LocalTime.of(15, 0));
        suggestion1.setSuggestedEndTime(LocalTime.of(16, 0));
        suggestion1.setTargetCustomersCount(50);
        suggestion1.setDiscountValue(20.0);
        suggestion1.setSuggestedProductName("Latte");
        suggestion1.setApprovalStatus(SuggestionStatus.PENDING);
        suggestion1.setSuggestionRound(1);
        suggestion1.setAiAnalysis(aiAnalysis);
        campaignSuggestionRepository.save(suggestion1);

        suggestion2 = new CampaignSuggestion();
        suggestion2.setTitle("Cookie Offer");
        suggestion2.setDescription("Clear surplus campaign");
        suggestion2.setOfferText("Buy coffee and get cookie discount");
        suggestion2.setCampaignType(CampaignType.DIRECT_OFFER);
        suggestion2.setSuggestedStartDate(LocalDate.of(2026, 6, 20));
        suggestion2.setSuggestedEndDate(LocalDate.of(2026, 6, 20));
        suggestion2.setSuggestedStartTime(LocalTime.of(16, 0));
        suggestion2.setSuggestedEndTime(LocalTime.of(17, 0));
        suggestion2.setTargetCustomersCount(40);
        suggestion2.setDiscountValue(15.0);
        suggestion2.setSuggestedProductName("Cookie");
        suggestion2.setApprovalStatus(SuggestionStatus.PENDING);
        suggestion2.setSuggestionRound(2);
        suggestion2.setAiAnalysis(aiAnalysis);
        campaignSuggestionRepository.save(suggestion2);
    }

    @Test
    public void findSalesRecordByIdTesting() {
        SalesRecord foundSalesRecord = salesRecordRepository.findSalesRecordById(salesRecord.getId());

        Assertions.assertThat(foundSalesRecord).isEqualTo(salesRecord);
    }

    @Test
    public void findAllByBranchIdTesting() {
        List<SalesRecord> salesRecords = salesRecordRepository.findAllByBranch_Id(branch.getId());

        Assertions.assertThat(salesRecords).isNotEmpty();
        Assertions.assertThat(salesRecords.get(0).getBranch().getId()).isEqualTo(branch.getId());
    }

    @Test
    public void existsByBranchIdAndMonthAndYearTesting() {
        boolean exists = salesRecordRepository.existsByBranch_IdAndMonthAndYear(
                branch.getId(),
                6,
                2026
        );

        Assertions.assertThat(exists).isTrue();
    }

    @Test
    public void findByBranchIdAndMonthAndYearTesting() {
        SalesRecord foundSalesRecord = salesRecordRepository.findByBranch_IdAndMonthAndYear(
                branch.getId(),
                6,
                2026
        );

        Assertions.assertThat(foundSalesRecord).isEqualTo(salesRecord);
    }

    @Test
    public void findSalesRecordItemByIdTesting() {
        SalesRecordItem foundItem = salesRecordItemRepository.findSalesRecordItemById(item1.getId());

        Assertions.assertThat(foundItem).isEqualTo(item1);
    }

    @Test
    public void findAllBySalesRecordIdTesting() {
        List<SalesRecordItem> items = salesRecordItemRepository.findAllBySalesRecord_Id(salesRecord.getId());

        Assertions.assertThat(items).hasSize(2);
        Assertions.assertThat(items.get(0).getSalesRecord().getId()).isEqualTo(salesRecord.getId());
    }

    @Test
    public void findAIAnalysisBySalesRecordIdTesting() {
        AIAnalysis foundAnalysis = aiAnalysisRepository.findAIAnalysisBySalesRecord_Id(salesRecord.getId());

        Assertions.assertThat(foundAnalysis).isEqualTo(aiAnalysis);
    }
}
