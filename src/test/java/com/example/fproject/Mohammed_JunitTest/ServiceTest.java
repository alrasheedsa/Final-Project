package com.example.fproject.Mohammed_JunitTest;

import com.example.fproject.Api.ApiException;
import com.example.fproject.Enum.*;
import com.example.fproject.Model.*;
import com.example.fproject.Repository.*;
import com.example.fproject.Service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServiceTest {

    @Mock CampaignMessageRepository campaignMessageRepository;
    @Mock CampaignRepository campaignRepository;
    @Mock CustomerRepository customerRepository;
    @Mock CustomerAnswerRepository customerAnswerRepository;
    @Mock StoreOwnerRepository storeOwnerRepository;   // ← الجديد
    @Mock ModelMapper modelMapper;

    @InjectMocks CampaignMessageService campaignMessageService;

    Integer userId = 10;
    StoreOwner owner;
    CampaignMessage message;

    @BeforeEach
    void setUp() {
        User ownerUser = new User();
        ownerUser.setId(userId);

        owner = new StoreOwner();
        owner.setId(5);
        owner.setUser(ownerUser);

        Store store = new Store();
        store.setStoreOwner(owner);

        Branch branch = new Branch();
        branch.setStore(store);

        Campaign campaign = new Campaign();
        campaign.setBranch(branch);

        message = new CampaignMessage();
        message.setId(1);
        message.setMessageText("You have a new offer");
        message.setCampaign(campaign);

        // الملكية تنجح: الـ stub يرجع نفس المالك (id = 5)
        when(campaignMessageRepository.findById(1)).thenReturn(Optional.of(message));
        when(storeOwnerRepository.findStoreOwnerByUserId(userId)).thenReturn(owner);
    }

    @Test
    public void markMessageAsSent_AlreadySent_ThrowsException() {
        message.setStatus(MessageStatus.SENT);

        assertThatThrownBy(() -> campaignMessageService.markMessageAsSent(userId, 1))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already marked as sent");
    }

    @Test
    public void markMessageAsSent_Success_SetsStatusSentAndSaves() {
        message.setStatus(MessageStatus.FAILED);

        campaignMessageService.markMessageAsSent(userId, 1);

        assertThat(message.getStatus()).isEqualTo(MessageStatus.SENT);
        verify(campaignMessageRepository).save(message);
    }

    @Test
    public void markMessageAsAnswered_NoCustomerAnswer_ThrowsException() {
        message.setStatus(MessageStatus.SENT);
        message.setCustomerAnswer(null);

        assertThatThrownBy(() -> campaignMessageService.markMessageAsAnswered(userId, 1))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("has no customer answer");
    }
}