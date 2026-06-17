package com.example.fproject.Repository;

import com.example.fproject.Model.MonthlyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonthlyReportRepository extends JpaRepository<MonthlyReport, Integer> {

    MonthlyReport findMonthlyReportById(Integer id);

    List<MonthlyReport> findMonthlyReportsByBranchId(Integer branchId);

    MonthlyReport findMonthlyReportByBranchIdAndMonthAndYear(Integer branchId, Integer month, Integer year);

    List<MonthlyReport> findMonthlyReportsByBranchIdOrderByYearDescMonthDesc(Integer branchId);

    Boolean existsMonthlyReportByBranchIdAndMonthAndYear(Integer branchId, Integer month, Integer year);
}