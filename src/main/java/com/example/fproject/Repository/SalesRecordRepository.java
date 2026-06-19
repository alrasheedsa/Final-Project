package com.example.fproject.Repository;

import com.example.fproject.Model.SalesRecord;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalesRecordRepository  extends JpaRepository<SalesRecord, Integer> {

    SalesRecord findSalesRecordById(Integer id);

    List<SalesRecord> findAllByBranch_Id(Integer branchId);

    boolean existsByBranch_IdAndMonthAndYear(Integer branchId, Integer month, Integer year);

    Boolean existsByBranchId(Integer branchId);

    SalesRecord findByBranch_IdAndMonthAndYear(Integer branchId, @NotNull(message = "Month is required") @Min(value = 1, message = "Month must be between 1 and 12") @Max(value = 12, message = "Month must be between 1 and 12") Integer month, @NotNull(message = "Year is required") @Min(value = 2026, message = "Year must be 2026 or later") Integer year);
}
