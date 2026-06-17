package com.example.fproject.Repository;

import com.example.fproject.Model.MonthlyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonthlyReportRepository extends JpaRepository<MonthlyReport, Integer> {

    MonthlyReport findMonthlyReportById(Integer id);

    List<MonthlyReport> findMonthlyReportsByStoreId(Integer storeId);

    MonthlyReport findMonthlyReportByStoreIdAndMonthAndYear(Integer storeId, Integer month, Integer year);

    List<MonthlyReport> findMonthlyReportsByStoreIdOrderByYearDescMonthDesc(Integer storeId);

    Boolean existsMonthlyReportByStoreIdAndMonthAndYear(Integer storeId, Integer month, Integer year);
}