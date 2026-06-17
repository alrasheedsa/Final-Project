package com.example.fproject.Repository;

import com.example.fproject.Model.QRRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QRRedemptionRepository extends JpaRepository<QRRedemption, Integer> {
}
