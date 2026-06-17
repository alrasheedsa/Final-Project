package com.example.fproject.Repository;

import com.example.fproject.Model.QRCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QRCodeRepository extends JpaRepository<QRCode, Integer> {
}
