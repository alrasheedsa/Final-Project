package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.SalesRecordItemIn;
import com.example.fproject.DTO.OUT.SalesRecordItemOut;
import com.example.fproject.Model.SalesRecord;
import com.example.fproject.Model.SalesRecordItem;
import com.example.fproject.Repository.SalesRecordItemRepository;
import com.example.fproject.Repository.SalesRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesRecordItemService {

    private final SalesRecordItemRepository salesRecordItemRepository;
    private final SalesRecordRepository salesRecordRepository;

    public List<SalesRecordItemOut> getAllSalesRecordItems() {
        List<SalesRecordItem> salesRecordItems = salesRecordItemRepository.findAll();
        List<SalesRecordItemOut> salesRecordItemOuts = new ArrayList<>();

        for (SalesRecordItem salesRecordItem : salesRecordItems) {
            salesRecordItemOuts.add(convertToOut(salesRecordItem));
        }

        return salesRecordItemOuts;
    }

    public SalesRecordItemOut getSalesRecordItemById(Integer id) {
        SalesRecordItem salesRecordItem = salesRecordItemRepository.findSalesRecordItemById(id);

        if (salesRecordItem == null) {
            throw new ApiException("Sales record item not found");
        }

        return convertToOut(salesRecordItem);
    }

    public List<SalesRecordItemOut> getSalesRecordItemsBySalesRecordId(Integer salesRecordId) {
        SalesRecord salesRecord = salesRecordRepository.findSalesRecordById(salesRecordId);

        if (salesRecord == null) {
            throw new ApiException("Sales record not found");
        }

        List<SalesRecordItem> salesRecordItems = salesRecordItemRepository.findAllBySalesRecord_Id(salesRecordId);
        List<SalesRecordItemOut> salesRecordItemOuts = new ArrayList<>();

        for (SalesRecordItem salesRecordItem : salesRecordItems) {
            salesRecordItemOuts.add(convertToOut(salesRecordItem));
        }

        return salesRecordItemOuts;
    }

    public void addSalesRecordItem(SalesRecordItemIn salesRecordItemIn) {
        validateSalesRecordItemIn(salesRecordItemIn);

        SalesRecord salesRecord = salesRecordRepository.findSalesRecordById(salesRecordItemIn.getSalesRecordId());

        if (salesRecord == null) {
            throw new ApiException("Sales record not found");
        }

        validateSaleDateWithSalesRecordMonth(salesRecordItemIn, salesRecord);

        SalesRecordItem salesRecordItem = new SalesRecordItem();

        salesRecordItem.setProductName(salesRecordItemIn.getProductName());
        salesRecordItem.setQuantity(salesRecordItemIn.getQuantity());
        salesRecordItem.setUnitPrice(salesRecordItemIn.getUnitPrice());

        // اليوزر ما يدخلها، السيرفس يحسبها
        salesRecordItem.setTotalPrice(salesRecordItemIn.getQuantity() * salesRecordItemIn.getUnitPrice());

        salesRecordItem.setSaleDate(salesRecordItemIn.getSaleDate());
        salesRecordItem.setSaleTime(salesRecordItemIn.getSaleTime());
        salesRecordItem.setSalesRecord(salesRecord);

        salesRecordItemRepository.save(salesRecordItem);
    }

    public void updateSalesRecordItem(Integer id, SalesRecordItemIn salesRecordItemIn) {
        validateSalesRecordItemIn(salesRecordItemIn);

        SalesRecordItem oldSalesRecordItem = salesRecordItemRepository.findSalesRecordItemById(id);

        if (oldSalesRecordItem == null) {
            throw new ApiException("Sales record item not found");
        }

        SalesRecord salesRecord = salesRecordRepository.findSalesRecordById(salesRecordItemIn.getSalesRecordId());

        if (salesRecord == null) {
            throw new ApiException("Sales record not found");
        }

        validateSaleDateWithSalesRecordMonth(salesRecordItemIn, salesRecord);

        oldSalesRecordItem.setProductName(salesRecordItemIn.getProductName());
        oldSalesRecordItem.setQuantity(salesRecordItemIn.getQuantity());
        oldSalesRecordItem.setUnitPrice(salesRecordItemIn.getUnitPrice());

        // نعيد حسابها وقت التعديل
        oldSalesRecordItem.setTotalPrice(salesRecordItemIn.getQuantity() * salesRecordItemIn.getUnitPrice());

        oldSalesRecordItem.setSaleDate(salesRecordItemIn.getSaleDate());
        oldSalesRecordItem.setSaleTime(salesRecordItemIn.getSaleTime());
        oldSalesRecordItem.setSalesRecord(salesRecord);

        salesRecordItemRepository.save(oldSalesRecordItem);
    }

    public void deleteSalesRecordItem(Integer id) {
        SalesRecordItem salesRecordItem = salesRecordItemRepository.findSalesRecordItemById(id);

        if (salesRecordItem == null) {
            throw new ApiException("Sales record item not found");
        }

        salesRecordItemRepository.delete(salesRecordItem);
    }

    private void validateSalesRecordItemIn(SalesRecordItemIn salesRecordItemIn) {
        if (salesRecordItemIn.getProductName() == null || salesRecordItemIn.getProductName().isBlank()) {
            throw new ApiException("Product name is required");
        }

        if (salesRecordItemIn.getQuantity() == null) {
            throw new ApiException("Quantity is required");
        }

        if (salesRecordItemIn.getQuantity() <= 0) {
            throw new ApiException("Quantity must be greater than zero");
        }

        if (salesRecordItemIn.getUnitPrice() == null) {
            throw new ApiException("Unit price is required");
        }

        if (salesRecordItemIn.getUnitPrice() < 0) {
            throw new ApiException("Unit price cannot be negative");
        }

        if (salesRecordItemIn.getSaleDate() == null) {
            throw new ApiException("Sale date is required");
        }

        if (salesRecordItemIn.getSaleDate().isAfter(LocalDate.now())) {
            throw new ApiException("Sale date cannot be in the future");
        }

        if (salesRecordItemIn.getSaleTime() == null) {
            throw new ApiException("Sale time is required");
        }

        if (salesRecordItemIn.getSalesRecordId() == null) {
            throw new ApiException("Sales record id is required");
        }
    }

    private void validateSaleDateWithSalesRecordMonth(SalesRecordItemIn salesRecordItemIn, SalesRecord salesRecord) {
        Integer itemMonth = salesRecordItemIn.getSaleDate().getMonthValue();
        Integer itemYear = salesRecordItemIn.getSaleDate().getYear();

        if (!itemMonth.equals(salesRecord.getMonth()) || !itemYear.equals(salesRecord.getYear())) {
            throw new ApiException("Sale date must be within the same month and year of the sales record");
        }
    }

    private SalesRecordItemOut convertToOut(SalesRecordItem salesRecordItem) {
        return new SalesRecordItemOut(
                salesRecordItem.getId(),
                salesRecordItem.getProductName(),
                salesRecordItem.getQuantity(),
                salesRecordItem.getUnitPrice(),
                salesRecordItem.getTotalPrice(),
                salesRecordItem.getSaleDate(),
                salesRecordItem.getSaleTime(),
                salesRecordItem.getSalesRecord().getId()
        );
    }
}
