package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.GoogleSheetSalesRecordIn;
import com.example.fproject.DTO.IN.SalesRecordIn;
import com.example.fproject.DTO.OUT.SalesRecordOut;
import com.example.fproject.Enum.StoreStatus;
import com.example.fproject.Enum.SubscriptionStatus;
import com.example.fproject.Model.Branch;
import com.example.fproject.Model.SalesRecord;
import com.example.fproject.Model.SalesRecordItem;
import com.example.fproject.Model.StoreOwner;
import com.example.fproject.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesRecordService {

    private final SalesRecordRepository salesRecordRepository;
    private final SalesRecordItemRepository salesRecordItemRepository;
    private final BranchRepository branchRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AIAnalysisRepository aiAnalysisRepository;
    private final StoreOwnerRepository storeOwnerRepository;
    private final ExcelService excelService;
    private final AIAnalysisService aiAnalysisService;
    private final GoogleSheetService googleSheetService;

    public List<SalesRecordOut> getAllSalesRecords() {
        List<SalesRecord> salesRecords = salesRecordRepository.findAll();
        List<SalesRecordOut> result = new ArrayList<>();
        for (SalesRecord salesRecord : salesRecords) result.add(convertToOut(salesRecord));
        return result;
    }

    public SalesRecordOut getSalesRecordById(Integer userId, Integer id) {
        SalesRecord salesRecord = findSalesRecordOrThrow(id);
        verifyOwnership(userId, salesRecord.getBranch());
        return convertToOut(salesRecord);
    }

    public List<SalesRecordOut> getSalesRecordsByBranchId(Integer userId, Integer branchId) {
        Branch branch = findBranchOrThrow(branchId);
        verifyOwnership(userId, branch);
        List<SalesRecordOut> result = new ArrayList<>();
        for (SalesRecord sr : salesRecordRepository.findAllByBranch_Id(branchId)) result.add(convertToOut(sr));
        return result;
    }

    @Transactional
    public void addSalesRecord(Integer userId, MultipartFile file, SalesRecordIn salesRecordIn) {
        String fileUrl = null;
        try {
            validateSalesRecordIn(salesRecordIn);
            excelService.validateExcelFile(file);

            Branch branch = validateBranchReadyForSalesRecord(salesRecordIn.getBranchId());
            verifyOwnership(userId, branch);

            if (Boolean.TRUE.equals(salesRecordRepository.existsByBranch_IdAndMonthAndYear(
                    salesRecordIn.getBranchId(), salesRecordIn.getMonth(), salesRecordIn.getYear())))
                throw new ApiException("Sales record already exists for this branch in the same month and year");

            String salesData = excelService.extractSalesData(file);
            List<SalesRecordItem> salesRecordItems = excelService.extractSalesRecordItems(file);
            fileUrl = saveExcelFile(file);

            SalesRecord salesRecord = new SalesRecord();
            salesRecord.setFileName(file.getOriginalFilename());
            salesRecord.setFileUrl(fileUrl);
            salesRecord.setMonth(salesRecordIn.getMonth());
            salesRecord.setYear(salesRecordIn.getYear());
            salesRecord.setUploadedAt(LocalDateTime.now());
            salesRecord.setBranch(branch);

            SalesRecord saved = salesRecordRepository.save(salesRecord);
            for (SalesRecordItem item : salesRecordItems) {
                item.setSalesRecord(saved);
                salesRecordItemRepository.save(item);
            }
            aiAnalysisService.generateAIAnalysisFromSalesRecord(saved.getId(), salesData);

        } catch (ApiException e) {
            deleteSavedExcelFile(fileUrl);
            throw e;
        } catch (Exception e) {
            deleteSavedExcelFile(fileUrl);
            throw new ApiException("Failed to add sales record");
        }
    }

    @Transactional
    public void importSalesRecordFromGoogleSheet(Integer userId, Integer branchId, GoogleSheetSalesRecordIn dto) {
        SalesRecordIn salesRecordIn = new SalesRecordIn(dto.getMonth(), dto.getYear(), branchId);
        validateSalesRecordIn(salesRecordIn);

        Branch branch = validateBranchReadyForSalesRecord(branchId);
        verifyOwnership(userId, branch);

        if (Boolean.TRUE.equals(salesRecordRepository.existsByBranch_IdAndMonthAndYear(
                branchId, dto.getMonth(), dto.getYear())))
            throw new ApiException("Sales record already exists for this branch in the same month and year");

        String salesData = googleSheetService.extractSalesData(dto.getSpreadsheetId(), dto.getRange());
        List<SalesRecordItem> items = googleSheetService.extractSalesRecordItems(dto.getSpreadsheetId(), dto.getRange());

        SalesRecord salesRecord = new SalesRecord();
        salesRecord.setFileName("Google Sheets Import");
        salesRecord.setFileUrl("https://docs.google.com/spreadsheets/d/" + dto.getSpreadsheetId());
        salesRecord.setMonth(dto.getMonth());
        salesRecord.setYear(dto.getYear());
        salesRecord.setUploadedAt(LocalDateTime.now());
        salesRecord.setBranch(branch);

        SalesRecord saved = salesRecordRepository.save(salesRecord);
        for (SalesRecordItem item : items) {
            item.setSalesRecord(saved);
            salesRecordItemRepository.save(item);
        }
        aiAnalysisService.generateAIAnalysisFromSalesRecord(saved.getId(), salesData);
    }

    @Transactional
    public void updateSalesRecord(Integer userId, Integer id, MultipartFile file, SalesRecordIn salesRecordIn) {
        validateSalesRecordIn(salesRecordIn);

        SalesRecord old = findSalesRecordOrThrow(id);
        verifyOwnership(userId, old.getBranch());

        if (Boolean.TRUE.equals(aiAnalysisRepository.existsBySalesRecord_Id(id)))
            throw new ApiException("Cannot update sales record because it already has AI analysis");

        if (file != null && !file.isEmpty()) excelService.validateExcelFile(file);

        Branch branch = validateBranchReadyForSalesRecord(salesRecordIn.getBranchId());
        verifyOwnership(userId, branch);

        boolean changedBranch = !old.getBranch().getId().equals(salesRecordIn.getBranchId());
        boolean changedMonth  = !old.getMonth().equals(salesRecordIn.getMonth());
        boolean changedYear   = !old.getYear().equals(salesRecordIn.getYear());

        if (changedBranch || changedMonth || changedYear) {
            if (Boolean.TRUE.equals(salesRecordRepository.existsByBranch_IdAndMonthAndYear(
                    salesRecordIn.getBranchId(), salesRecordIn.getMonth(), salesRecordIn.getYear())))
                throw new ApiException("Another sales record already exists for this branch in the same month and year");
        }

        if (file != null && !file.isEmpty()) {
            old.setFileName(file.getOriginalFilename());
            old.setFileUrl(saveExcelFile(file));
        }

        old.setMonth(salesRecordIn.getMonth());
        old.setYear(salesRecordIn.getYear());
        old.setBranch(branch);
        salesRecordRepository.save(old);
    }

    public void deleteSalesRecord(Integer userId, Integer id) {
        SalesRecord salesRecord = findSalesRecordOrThrow(id);
        verifyOwnership(userId, salesRecord.getBranch());

        if (Boolean.TRUE.equals(aiAnalysisRepository.existsBySalesRecord_Id(id)))
            throw new ApiException("Cannot delete sales record because it has AI analysis");

        if (salesRecord.getItems() != null && !salesRecord.getItems().isEmpty())
            throw new ApiException("Cannot delete sales record because it has sales record items");

        salesRecordRepository.delete(salesRecord);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void verifyOwnership(Integer userId, Branch branch) {
        StoreOwner owner = storeOwnerRepository.findStoreOwnerByUserId(userId);
        if (owner == null || !branch.getStore().getStoreOwner().getId().equals(owner.getId()))
            throw new ApiException("You do not have permission to access this resource");
    }

    private SalesRecord findSalesRecordOrThrow(Integer id) {
        SalesRecord sr = salesRecordRepository.findSalesRecordById(id);
        if (sr == null) throw new ApiException("Sales record not found");
        return sr;
    }

    private Branch findBranchOrThrow(Integer branchId) {
        Branch branch = branchRepository.findBranchById(branchId);
        if (branch == null) throw new ApiException("Branch not found");
        return branch;
    }

    private void validateSalesRecordIn(SalesRecordIn salesRecordIn) {
        if (salesRecordIn.getMonth() == null) throw new ApiException("Month is required");
        if (salesRecordIn.getMonth() < 1 || salesRecordIn.getMonth() > 12) throw new ApiException("Month must be between 1 and 12");
        if (salesRecordIn.getYear() == null) throw new ApiException("Year is required");
        if (salesRecordIn.getYear() < 2020) throw new ApiException("Year must be valid");
        if (salesRecordIn.getBranchId() == null) throw new ApiException("Branch id is required");
        LocalDate today = LocalDate.now();
        if (salesRecordIn.getYear() > today.getYear()) throw new ApiException("Sales record year cannot be in the future");
        if (salesRecordIn.getYear().equals(today.getYear()) && salesRecordIn.getMonth() > today.getMonthValue())
            throw new ApiException("Sales record month cannot be in the future");
    }

    private Branch validateBranchReadyForSalesRecord(Integer branchId) {
        Branch branch = branchRepository.findBranchById(branchId);
        if (branch == null) throw new ApiException("Branch not found");
        if (branch.getStatus() != StoreStatus.ACTIVE) throw new ApiException("Branch must be active before uploading sales record");
        if (branch.getStore() == null) throw new ApiException("Branch store not found");
        if (branch.getStore().getStatus() != StoreStatus.ACTIVE) throw new ApiException("Store must be active before uploading sales record");
        if (branch.getStore().getStoreOwner() == null) throw new ApiException("Store owner not found for this branch");

        var activeSubscription = subscriptionRepository.findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(
                branch.getStore().getStoreOwner().getId(), SubscriptionStatus.ACTIVE);
        if (activeSubscription == null) throw new ApiException("Branch does not have an active subscription");
        if (activeSubscription.getEndDate().isBefore(LocalDate.now())) throw new ApiException("Branch subscription is expired");

        return branch;
    }

    private String saveExcelFile(MultipartFile file) {
        try {
            Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "sales-records");
            Files.createDirectories(uploadDir);
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || originalFileName.isBlank()) originalFileName = "sales-record.xlsx";
            String safeFileName = originalFileName.replaceAll("[^A-Za-z0-9._-]", "_");
            String storedFileName = System.currentTimeMillis() + "_" + safeFileName;
            Path targetPath = uploadDir.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return "uploads/sales-records/" + storedFileName;
        } catch (IOException e) {
            throw new ApiException("Failed to save Excel sales file");
        }
    }

    private void deleteSavedExcelFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;
        try {
            Files.deleteIfExists(Paths.get(System.getProperty("user.dir"), fileUrl));
        } catch (IOException ignored) {}
    }

    private SalesRecordOut convertToOut(SalesRecord salesRecord) {
        Integer itemsCount = salesRecord.getItems() != null ? salesRecord.getItems().size() : 0;
        Integer aiAnalysisId = salesRecord.getAiAnalysis() != null ? salesRecord.getAiAnalysis().getId() : null;
        return new SalesRecordOut(
                salesRecord.getId(), salesRecord.getFileName(), salesRecord.getFileUrl(),
                salesRecord.getMonth(), salesRecord.getYear(), salesRecord.getUploadedAt(),
                salesRecord.getBranch().getId(), itemsCount, aiAnalysisId
        );
    }
}