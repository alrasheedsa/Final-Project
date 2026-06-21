package com.example.fproject;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.OUT.*;
import com.example.fproject.Enum.CampaignType;
import com.example.fproject.Enum.SuggestionStatus;
import com.example.fproject.Model.*;
import com.example.fproject.Repository.*;
import com.example.fproject.Service.*;
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
public class RaghadServiceTest {

    SalesRecordService salesRecordService;
    SalesRecordItemService salesRecordItemService;
    AIAnalysisService aiAnalysisService;
    CampaignSuggestionService campaignSuggestionService;

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
    CampaignSuggestionRepository campaignSuggestionRepository;

    @Mock
    ExcelService excelService;

    @Mock
    OpenAiService openAiService;

    @Mock
    EmailService emailService;

    @Mock
    HolidayService holidayService;

    @Mock
    GoogleSheetService googleSheetService;

    Branch branch;
    SalesRecord salesRecord;
    SalesRecordItem item;
    AIAnalysis aiAnalysis;
    CampaignSuggestion suggestion;

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

        aiAnalysisService = new AIAnalysisService(
                aiAnalysisRepository,
                salesRecordRepository,
                salesRecordItemRepository,
                openAiService,
                emailService
        );

        campaignSuggestionService = new CampaignSuggestionService(
                campaignSuggestionRepository,
                aiAnalysisRepository,
                subscriptionRepository,
                openAiService,
                holidayService,
                emailService
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

        aiAnalysis = new AIAnalysis();
        aiAnalysis.setId(1);
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

        suggestion = new CampaignSuggestion();
        suggestion.setId(1);
        suggestion.setTitle("Latte Offer");
        suggestion.setDescription("Discount campaign");
        suggestion.setOfferText("20% off Latte");
        suggestion.setCampaignType(CampaignType.DIRECT_OFFER);
        suggestion.setSuggestedProductName("Latte");
        suggestion.setSuggestedStartDate(LocalDate.of(2026, 6, 20));
        suggestion.setSuggestedEndDate(LocalDate.of(2026, 6, 20));
        suggestion.setSuggestedStartTime(LocalTime.of(15, 0));
        suggestion.setSuggestedEndTime(LocalTime.of(16, 0));
        suggestion.setTargetCustomersCount(50);
        suggestion.setDiscountValue(20.0);
        suggestion.setApprovalStatus(SuggestionStatus.PENDING);
        suggestion.setSuggestionRound(1);
        suggestion.setAiAnalysis(aiAnalysis);
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

    @Test
    public void getSalesRecordItemByIdNotFoundTest() {
        when(salesRecordItemRepository.findSalesRecordItemById(100)).thenReturn(null);

        ApiException exception = Assertions.assertThrows(ApiException.class, () -> {
            salesRecordItemService.getSalesRecordItemById(100);
        });

        Assertions.assertEquals("Sales record item not found", exception.getMessage());
        verify(salesRecordItemRepository, times(1)).findSalesRecordItemById(100);
    }

    @Test
    public void getAIAnalysisByIdTest() {
        when(aiAnalysisRepository.findAIAnalysisById(aiAnalysis.getId())).thenReturn(aiAnalysis);

        AIAnalysisOut result = aiAnalysisService.getAIAnalysisById(aiAnalysis.getId());

        Assertions.assertEquals(aiAnalysis.getId(), result.getId());
        verify(aiAnalysisRepository, times(1)).findAIAnalysisById(aiAnalysis.getId());
    }

    @Test
    public void getPeakHoursTest() {
        when(aiAnalysisRepository.findAIAnalysisById(aiAnalysis.getId())).thenReturn(aiAnalysis);

        String result = aiAnalysisService.getPeakHours(aiAnalysis.getId());

        Assertions.assertEquals("09:00-11:00", result);
        verify(aiAnalysisRepository, times(1)).findAIAnalysisById(aiAnalysis.getId());
    }

    @Test
    public void getCampaignSuggestionByIdTest() {
        when(campaignSuggestionRepository.findCampaignSuggestionById(suggestion.getId())).thenReturn(suggestion);

        CampaignSuggestionOut result = campaignSuggestionService.getCampaignSuggestionById(suggestion.getId());

        Assertions.assertEquals(suggestion.getId(), result.getId());
        verify(campaignSuggestionRepository, times(1)).findCampaignSuggestionById(suggestion.getId());
    }

    @Test
    public void getPendingSuggestionsByAnalysisTest() {
        when(aiAnalysisRepository.findAIAnalysisById(aiAnalysis.getId())).thenReturn(aiAnalysis);
        when(campaignSuggestionRepository.findAllByAiAnalysis_Id(aiAnalysis.getId())).thenReturn(List.of(suggestion));

        List<CampaignSuggestionOut> result = campaignSuggestionService.getPendingSuggestionsByAnalysis(aiAnalysis.getId());

        Assertions.assertEquals(1, result.size());
        verify(aiAnalysisRepository, times(1)).findAIAnalysisById(aiAnalysis.getId());
        verify(campaignSuggestionRepository, times(1)).findAllByAiAnalysis_Id(aiAnalysis.getId());
    }
}
