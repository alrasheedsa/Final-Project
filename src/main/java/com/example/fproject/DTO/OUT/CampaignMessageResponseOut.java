package com.example.fproject.DTO.OUT;

import com.example.fproject.Enum.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CampaignMessageResponseOut {

    private Integer id;
    private String messageText;
    private Double distanceKm;
    private Integer durationMinutes;
    private String distanceText;
    private MessageStatus status;
    private LocalDateTime sentAt;
    private Integer campaignId;
    private Integer customerId;
    private Integer customerAnswerId;
}
