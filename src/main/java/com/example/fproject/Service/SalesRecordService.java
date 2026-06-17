package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.SalesRecordIn;
import com.example.fproject.DTO.OUT.SalesRecordOut;
import com.example.fproject.Model.Branch;
import com.example.fproject.Model.SalesRecord;
import com.example.fproject.Model.Store;
import com.example.fproject.Repository.BranchRepository;
import com.example.fproject.Repository.SalesRecordRepository;
import com.example.fproject.Repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesRecordService {

    private final SalesRecordRepository salesRecordRepository;
    private final BranchRepository branchRepository;

    public List<SalesRecordOut> getAllSalesRecords() {
        List<SalesRecord> salesRecords = salesRecordRepository.findAll();
        List<SalesRecordOut> salesRecordOuts = new ArrayList<>();

        for (SalesRecord salesRecord : salesRecords) {
            salesRecordOuts.add(convertToOut(salesRecord));
        }

        return salesRecordOuts;
    }

    public SalesRecordOut getSalesRecordById(Integer id) {
        SalesRecord salesRecord = salesRecordRepository.findSalesRecordById(id);

        if (salesRecord == null) {
            throw new ApiException("Sales record not found");
        }

        return convertToOut(salesRecord);
    }

    public List<SalesRecordOut> getSalesRecordsByBranchId(Integer branchId) {
        Branch branch = branchRepository.findBranchById(branchId);

        if (branch == null) {
            throw new ApiException("Branch not found");
        }

        List<SalesRecord> salesRecords = salesRecordRepository.findAllByBranch_Id(branchId);
        List<SalesRecordOut> salesRecordOuts = new ArrayList<>();

        for (SalesRecord salesRecord : salesRecords) {
            salesRecordOuts.add(convertToOut(salesRecord));
        }

        return salesRecordOuts;
    }

    public void addSalesRecord(SalesRecordIn salesRecordIn) {
        validateSalesRecordIn(salesRecordIn);

        Branch branch = branchRepository.findBranchById(salesRecordIn.getBranchId());

        if (branch == null) {
            throw new ApiException("Branch not found");
        }

        Boolean exists = salesRecordRepository.existsByBranch_IdAndMonthAndYear(
                salesRecordIn.getBranchId(),
                salesRecordIn.getMonth(),
                salesRecordIn.getYear()
        );

        if (Boolean.TRUE.equals(exists)) {
            throw new ApiException("Sales record already exists for this branch in the same month and year");
        }

        SalesRecord salesRecord = new SalesRecord();

        salesRecord.setFileName(salesRecordIn.getFileName());
        salesRecord.setFileUrl(salesRecordIn.getFileUrl());
        salesRecord.setMonth(salesRecordIn.getMonth());
        salesRecord.setYear(salesRecordIn.getYear());
        salesRecord.setUploadedAt(LocalDateTime.now());
        salesRecord.setBranch(branch);

        salesRecordRepository.save(salesRecord);
    }

    public void updateSalesRecord(Integer id, SalesRecordIn salesRecordIn) {
        validateSalesRecordIn(salesRecordIn);

        SalesRecord oldSalesRecord = salesRecordRepository.findSalesRecordById(id);

        if (oldSalesRecord == null) {
            throw new ApiException("Sales record not found");
        }

        Branch branch = branchRepository.findBranchById(salesRecordIn.getBranchId());

        if (branch == null) {
            throw new ApiException("Branch not found");
        }

        Boolean changedBranch = !oldSalesRecord.getBranch().getId().equals(salesRecordIn.getBranchId());
        Boolean changedMonth = !oldSalesRecord.getMonth().equals(salesRecordIn.getMonth());
        Boolean changedYear = !oldSalesRecord.getYear().equals(salesRecordIn.getYear());

        if (changedBranch || changedMonth || changedYear) {
            Boolean exists = salesRecordRepository.existsByBranch_IdAndMonthAndYear(
                    salesRecordIn.getBranchId(),
                    salesRecordIn.getMonth(),
                    salesRecordIn.getYear()
            );

            if (Boolean.TRUE.equals(exists)) {
                throw new ApiException("Another sales record already exists for this branch in the same month and year");
            }
        }

        oldSalesRecord.setFileName(salesRecordIn.getFileName());
        oldSalesRecord.setFileUrl(salesRecordIn.getFileUrl());
        oldSalesRecord.setMonth(salesRecordIn.getMonth());
        oldSalesRecord.setYear(salesRecordIn.getYear());
        oldSalesRecord.setBranch(branch);

        salesRecordRepository.save(oldSalesRecord);
    }

    public void deleteSalesRecord(Integer id) {
        SalesRecord salesRecord = salesRecordRepository.findSalesRecordById(id);

        if (salesRecord == null) {
            throw new ApiException("Sales record not found");
        }

        if (salesRecord.getItems() != null && !salesRecord.getItems().isEmpty()) {
            throw new ApiException("Cannot delete sales record because it has sales record items");
        }

        if (salesRecord.getAiAnalysis() != null) {
            throw new ApiException("Cannot delete sales record because it has AI analysis");
        }

        salesRecordRepository.delete(salesRecord);
    }

    private void validateSalesRecordIn(SalesRecordIn salesRecordIn) {
        if (salesRecordIn.getFileName() == null || salesRecordIn.getFileName().isBlank()) {
            throw new ApiException("File name is required");
        }

        if (salesRecordIn.getFileUrl() == null || salesRecordIn.getFileUrl().isBlank()) {
            throw new ApiException("File URL is required");
        }

        if (salesRecordIn.getMonth() == null) {
            throw new ApiException("Month is required");
        }

        if (salesRecordIn.getMonth() < 1 || salesRecordIn.getMonth() > 12) {
            throw new ApiException("Month must be between 1 and 12");
        }

        if (salesRecordIn.getYear() == null) {
            throw new ApiException("Year is required");
        }

        if (salesRecordIn.getYear() < 2020) {
            throw new ApiException("Year must be valid");
        }

        if (salesRecordIn.getBranchId() == null) {
            throw new ApiException("Branch id is required");
        }

        LocalDate today = LocalDate.now();

        if (salesRecordIn.getYear() > today.getYear()) {
            throw new ApiException("Sales record year cannot be in the future");
        }

        if (salesRecordIn.getYear().equals(today.getYear())
                && salesRecordIn.getMonth() > today.getMonthValue()) {
            throw new ApiException("Sales record month cannot be in the future");
        }
    }

    private SalesRecordOut convertToOut(SalesRecord salesRecord) {
        Integer itemsCount = 0;
        Integer aiAnalysisId = null;

        if (salesRecord.getItems() != null) {
            itemsCount = salesRecord.getItems().size();
        }

        if (salesRecord.getAiAnalysis() != null) {
            aiAnalysisId = salesRecord.getAiAnalysis().getId();
        }

        return new SalesRecordOut(
                salesRecord.getId(),
                salesRecord.getFileName(),
                salesRecord.getFileUrl(),
                salesRecord.getMonth(),
                salesRecord.getYear(),
                salesRecord.getUploadedAt(),
                salesRecord.getBranch().getId(),
                itemsCount,
                aiAnalysisId
        );
    }
}
