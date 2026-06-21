package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.SalesRecordItemIn;
import com.example.fproject.DTO.OUT.SalesRecordItemOut;
import com.example.fproject.Model.SalesRecord;
import com.example.fproject.Model.SalesRecordItem;
import com.example.fproject.Model.StoreOwner;
import com.example.fproject.Repository.AIAnalysisRepository;
import com.example.fproject.Repository.SalesRecordItemRepository;
import com.example.fproject.Repository.SalesRecordRepository;
import com.example.fproject.Repository.StoreOwnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesRecordItemService {

    private final SalesRecordItemRepository salesRecordItemRepository;
    private final SalesRecordRepository salesRecordRepository;
    private final AIAnalysisRepository aiAnalysisRepository;
    private final StoreOwnerRepository storeOwnerRepository;

    public List<SalesRecordItemOut> getAllSalesRecordItems() {
        List<SalesRecordItem> items = salesRecordItemRepository.findAll();
        List<SalesRecordItemOut> result = new ArrayList<>();
        for (SalesRecordItem item : items) result.add(convertToOut(item));
        return result;
    }

    public SalesRecordItemOut getSalesRecordItemById(Integer userId, Integer id) {
        SalesRecordItem item = findItemOrThrow(id);
        verifyOwnership(userId, item.getSalesRecord());
        return convertToOut(item);
    }

    public List<SalesRecordItemOut> getSalesRecordItemsBySalesRecordId(Integer userId, Integer salesRecordId) {
        SalesRecord salesRecord = findSalesRecordOrThrow(salesRecordId);
        verifyOwnership(userId, salesRecord);
        List<SalesRecordItemOut> result = new ArrayList<>();
        for (SalesRecordItem item : salesRecordItemRepository.findAllBySalesRecord_Id(salesRecordId))
            result.add(convertToOut(item));
        return result;
    }

    @Transactional
    public void addSalesRecordItem(Integer userId, Integer salesRecordId, SalesRecordItemIn dto) {
        validateSalesRecordItemIn(dto);
        SalesRecord salesRecord = findSalesRecordOrThrow(salesRecordId);
        verifyOwnership(userId, salesRecord);
        validateSalesRecordCanBeEdited(salesRecord.getId());
        validateSaleDateWithSalesRecordMonth(dto, salesRecord);

        SalesRecordItem item = new SalesRecordItem();
        item.setProductName(dto.getProductName());
        item.setQuantity(dto.getQuantity());
        item.setUnitPrice(dto.getUnitPrice());
        item.setTotalPrice(dto.getQuantity() * dto.getUnitPrice());
        item.setSaleDate(dto.getSaleDate());
        item.setSaleTime(dto.getSaleTime());
        item.setSalesRecord(salesRecord);
        salesRecordItemRepository.save(item);
    }

    @Transactional
    public void updateSalesRecordItem(Integer userId, Integer id, Integer salesRecordId, SalesRecordItemIn dto) {
        validateSalesRecordItemIn(dto);
        SalesRecordItem old = findItemOrThrow(id);
        verifyOwnership(userId, old.getSalesRecord());
        validateSalesRecordCanBeEdited(old.getSalesRecord().getId());

        SalesRecord salesRecord = findSalesRecordOrThrow(salesRecordId);
        verifyOwnership(userId, salesRecord);
        validateSalesRecordCanBeEdited(salesRecord.getId());
        validateSaleDateWithSalesRecordMonth(dto, salesRecord);

        old.setProductName(dto.getProductName());
        old.setQuantity(dto.getQuantity());
        old.setUnitPrice(dto.getUnitPrice());
        old.setTotalPrice(dto.getQuantity() * dto.getUnitPrice());
        old.setSaleDate(dto.getSaleDate());
        old.setSaleTime(dto.getSaleTime());
        old.setSalesRecord(salesRecord);
        salesRecordItemRepository.save(old);
    }

    @Transactional
    public void deleteSalesRecordItem(Integer userId, Integer id) {
        SalesRecordItem item = findItemOrThrow(id);
        verifyOwnership(userId, item.getSalesRecord());
        validateSalesRecordCanBeEdited(item.getSalesRecord().getId());
        salesRecordItemRepository.delete(item);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void verifyOwnership(Integer userId, SalesRecord salesRecord) {
        StoreOwner owner = storeOwnerRepository.findStoreOwnerByUserId(userId);
        if (owner == null || !salesRecord.getBranch().getStore().getStoreOwner().getId().equals(owner.getId()))
            throw new ApiException("You do not have permission to access this resource");
    }

    private SalesRecordItem findItemOrThrow(Integer id) {
        SalesRecordItem item = salesRecordItemRepository.findSalesRecordItemById(id);
        if (item == null) throw new ApiException("Sales record item not found");
        return item;
    }

    private SalesRecord findSalesRecordOrThrow(Integer salesRecordId) {
        SalesRecord sr = salesRecordRepository.findSalesRecordById(salesRecordId);
        if (sr == null) throw new ApiException("Sales record not found");
        return sr;
    }

    private void validateSalesRecordCanBeEdited(Integer salesRecordId) {
        if (Boolean.TRUE.equals(aiAnalysisRepository.existsBySalesRecord_Id(salesRecordId)))
            throw new ApiException("Cannot modify sales record items because sales record already has AI analysis");
    }

    private void validateSalesRecordItemIn(SalesRecordItemIn dto) {
        if (dto.getProductName() == null || dto.getProductName().isBlank()) throw new ApiException("Product name is required");
        if (dto.getQuantity() == null) throw new ApiException("Quantity is required");
        if (dto.getQuantity() <= 0) throw new ApiException("Quantity must be greater than zero");
        if (dto.getUnitPrice() == null) throw new ApiException("Unit price is required");
        if (dto.getUnitPrice() < 0) throw new ApiException("Unit price cannot be negative");
        if (dto.getSaleDate() == null) throw new ApiException("Sale date is required");
        if (dto.getSaleDate().isAfter(LocalDate.now())) throw new ApiException("Sale date cannot be in the future");
        if (dto.getSaleTime() == null) throw new ApiException("Sale time is required");
    }

    private void validateSaleDateWithSalesRecordMonth(SalesRecordItemIn dto, SalesRecord salesRecord) {
        Integer itemMonth = dto.getSaleDate().getMonthValue();
        Integer itemYear  = dto.getSaleDate().getYear();
        if (!itemMonth.equals(salesRecord.getMonth()) || !itemYear.equals(salesRecord.getYear()))
            throw new ApiException("Sale date must be within the same month and year of the sales record");
    }

    private SalesRecordItemOut convertToOut(SalesRecordItem item) {
        return new SalesRecordItemOut(
                item.getId(), item.getProductName(), item.getQuantity(),
                item.getUnitPrice(), item.getTotalPrice(),
                item.getSaleDate(), item.getSaleTime(),
                item.getSalesRecord().getId()
        );
    }
}