package com.example.fproject.Raghad_JunitTest;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.OUT.SalesRecordItemOut;
import com.example.fproject.DTO.OUT.SalesRecordOut;
import com.example.fproject.Model.Branch;
import com.example.fproject.Model.SalesRecord;
import com.example.fproject.Model.SalesRecordItem;
import com.example.fproject.Repository.AIAnalysisRepository;
import com.example.fproject.Repository.BranchRepository;
import com.example.fproject.Repository.SalesRecordItemRepository;
import com.example.fproject.Repository.SalesRecordRepository;
import com.example.fproject.Repository.SubscriptionRepository;
import com.example.fproject.Service.AIAnalysisService;
import com.example.fproject.Service.ExcelService;
import com.example.fproject.Service.GoogleSheetService;
import com.example.fproject.Service.SalesRecordItemService;
import com.example.fproject.Service.SalesRecordService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServiceTest {

    SalesRecordService salesRecordService;
    SalesRecordItemService salesRecordItemService;

    @Mock
    SalesRecordRepository salesRecordRepository;

    @Mock
    SalesRecordItemRepository salesRecordItemRepository;

    @Mock
    BranchRepository branchRepository;

    @Mock
    SubscriptionRepository subscriptionRepository;

    @Mock
    AIAnalysisRepository aiAnalysisRepository;

    @Mock
    ExcelService excelService;

    @Mock
    AIAnalysisService aiAnalysisService;

    @Mock
    GoogleSheetService googleSheetService;

    Branch branch;
    SalesRecord salesRecord;
    SalesRecordItem item;

    @BeforeEach
    void setUp() {
        salesRecordService = new SalesRecordService(
                salesRecordRepository,
                salesRecordItemRepository,
                branchRepository,
                subscriptionRepository,
                aiAnalysisRepository,
                excelService,
                aiAnalysisService,
                googleSheetService
        );

        salesRecordItemService = new SalesRecordItemService(
                salesRecordItemRepository,
                salesRecordRepository,
                aiAnalysisRepository
        );

        branch = new Branch();
        branch.setId(1);
        branch.setName("Test Branch");

        salesRecord = new SalesRecord();
        salesRecord.setId(1);
        salesRecord.setFileName("sales.xlsx");
        salesRecord.setFileUrl("uploads/sales.xlsx");
        salesRecord.setMonth(6);
        salesRecord.setYear(2026);
        salesRecord.setUploadedAt(LocalDateTime.now());
        salesRecord.setBranch(branch);

        item = new SalesRecordItem();
        item.setId(1);
        item.setProductName("Latte");
        item.setQuantity(10);
        item.setUnitPrice(12.0);
        item.setTotalPrice(120.0);
        item.setSaleDate(LocalDate.of(2026, 6, 1));
        item.setSaleTime(LocalTime.of(9, 0));
        item.setSalesRecord(salesRecord);
    }

    @Test
    public void getAllSalesRecordsTest() {
        when(salesRecordRepository.findAll()).thenReturn(List.of(salesRecord));

        List<SalesRecordOut> result = salesRecordService.getAllSalesRecords();

        Assertions.assertEquals(1, result.size());
        verify(salesRecordRepository, times(1)).findAll();
    }

    @Test
    public void getSalesRecordByIdNotFoundTest() {
        when(salesRecordRepository.findSalesRecordById(100)).thenReturn(null);

        ApiException exception = Assertions.assertThrows(ApiException.class, () -> {
            salesRecordService.getSalesRecordById(100);
        });

        Assertions.assertEquals("Sales record not found", exception.getMessage());
        verify(salesRecordRepository, times(1)).findSalesRecordById(100);
    }

    @Test
    public void getAllSalesRecordItemsTest() {
        when(salesRecordItemRepository.findAll()).thenReturn(List.of(item));

        List<SalesRecordItemOut> result = salesRecordItemService.getAllSalesRecordItems();

        Assertions.assertEquals(1, result.size());
        verify(salesRecordItemRepository, times(1)).findAll();
    }
}