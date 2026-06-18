package com.example.fproject.DTO.OUT;

import com.example.fproject.Enum.QRCodeStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QRCodeResponseOut {

    private Integer id;
    private String code;
    private String qrImageBase64;
    private Integer maxUsageCount;
    private Integer usedCount;
    private QRCodeStatus status;
    private Integer campaignId;
}
