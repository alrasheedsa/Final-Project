package com.example.fproject.Repository;

import com.example.fproject.Model.SalesRecordItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalesRecordItemRepository extends JpaRepository<SalesRecordItem, Integer> {
    SalesRecordItem findSalesRecordItemById(Integer id);
    List<SalesRecordItem> findAllBySalesRecord_Id(Integer salesRecordId);
}
