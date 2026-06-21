package com.example.fproject.Raghad_JunitTest;

import com.example.fproject.Enum.RoleType;
import com.example.fproject.Enum.StoreStatus;
import com.example.fproject.Model.Branch;
import com.example.fproject.Model.SalesRecord;
import com.example.fproject.Model.Store;
import com.example.fproject.Model.StoreOwner;
import com.example.fproject.Model.User;
import com.example.fproject.Repository.BranchRepository;
import com.example.fproject.Repository.SalesRecordRepository;
import com.example.fproject.Repository.StoreOwnerRepository;
import com.example.fproject.Repository.StoreRepository;
import com.example.fproject.Repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.LocalDateTime;
import java.util.List;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RepositoryTest {

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

    User user;
    StoreOwner storeOwner;
    Store store;
    Branch branch;
    SalesRecord salesRecord;

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
}
