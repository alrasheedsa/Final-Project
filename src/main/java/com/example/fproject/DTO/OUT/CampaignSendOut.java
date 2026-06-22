package com.example.fproject.DTO.OUT;

import com.example.fproject.Enum.CampaignStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CampaignSendOut {

    private Integer campaignId;
    private Integer sentCount;
    private Integer skippedCount;
    private CampaignStatus status;
}
