package com.example.fproject.DTO.OUT;

import com.example.fproject.Enum.CampaignStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CampaignStatusOut {

    private Integer campaignId;
    private CampaignStatus status;
    private LocalDateTime updatedAt;
}
