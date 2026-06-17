package com.example.fproject.Repository;

import com.example.fproject.Model.SalesRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalesRecordRepository  extends JpaRepository<SalesRecord, Integer> {

    SalesRecord findSalesRecordById(Integer id);

    List<SalesRecord> findAllByBranch_Id(Integer branchId);

    Boolean existsByBranch_IdAndMonthAndYear(Integer branchId, Integer month, Integer year);
}
