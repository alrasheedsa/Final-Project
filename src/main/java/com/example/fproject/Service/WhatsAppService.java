package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.UltraMsgWebhookIn;
import com.example.fproject.DTO.OUT.CustomerAnswerResponseOut;
import com.example.fproject.Enum.CampaignStatus;
import com.example.fproject.Enum.MessageStatus;
import com.example.fproject.Model.Branch;
import com.example.fproject.Model.Campaign;
import com.example.fproject.Model.CampaignMessage;
import com.example.fproject.Model.Customer;
import com.example.fproject.Model.QRCode;
import com.example.fproject.Repository.CampaignMessageRepository;
import com.example.fproject.Repository.CustomerRepository;
import com.example.fproject.Repository.QRCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class WhatsAppService {

    private final CustomerRepository customerRepository;
    private final CampaignMessageRepository campaignMessageRepository;
    private final QRCodeRepository qrCodeRepository;
    private final CustomerAnswerService customerAnswerService;
    private final EmailService emailService;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String ULTRAMSG_BASE_URL = "https://api.ultramsg.com";

    @Value("${ultramsg.instance.id}")
    private String instanceId;

    @Value("${ultramsg.token}")
    private String token;

    private static final String QUESTION_MESSAGE_TEMPLATE = """
            مرحبا

            لديك فرصة للحصول على عرض من متجر %s

            السؤال:
            %s

            A) %s

            B) %s

            C) %s

            للاجابة ارسل فقط:
            A او B او C

            تنبيه:
            لديك محاولة واحدة فقط.
            """;

    private static final String CORRECT_ANSWER_MESSAGE_TEMPLATE = """
            مبروك

            اجابتك صحيحة.

            اسم المتجر:
            %s

            الفرع:
            %s

            الحملة:
            %s

            العرض:
            %s

            وقت الحملة:
            %s

            موقع الفرع:
            %s

            يبعد عنك:
            %s

            مدة الوصول:
            %d دقائق

            QR Code:
            %s
            """;

    private static final String DIRECT_OFFER_MESSAGE_TEMPLATE = """
            مرحبا

            لديك عرض من متجر %s

            الفرع:
            %s

            الحملة:
            %s

            العرض:
            %s

            وقت الحملة:
            %s

            موقع الفرع:
            %s

            يبعد عنك:
            %s

            مدة الوصول:
            %d دقائق

            QR Code:
            %s
            """;

    private static final String WRONG_ANSWER_MESSAGE = """
            اجابتك غير صحيحة.

            لكن يمكنك زيارة الفرع وقد يحالفك الحظ في عروض اخرى مستقبلا.
            """;

    private static final String INVALID_ANSWER_MESSAGE = """
            الرجاء إرسال A أو B أو C فقط.
            """;

    private static final String NO_OPEN_CAMPAIGN_MESSAGE = """
            لا يوجد لديك عرض أو سؤال نشط حالياً.
            """;

    private static final String CAMPAIGN_ENDED_MESSAGE = """
            انتهت مدة الحملة، شكراً لتفاعلك.
            """;

    public String sendMessage(String phone, String messageBody) {
        validateConfiguration();
        validateText(phone, "Phone is required");
        validateText(messageBody, "Message is required");

        String url = ULTRAMSG_BASE_URL + "/" + instanceId + "/messages/chat";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", token);
        form.add("to", toUltraMsgNumber(phone));
        form.add("body", messageBody);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        try {
            restTemplate.postForEntity(url, request, String.class);
        } catch (Exception e) {
            throw new ApiException("Failed to send WhatsApp message: " + e.getMessage());
        }

        return "WhatsApp message has been sent";
    }

    public String sendQuestionMessage(String phone, String storeName, String questionText,
                                      String optionA, String optionB, String optionC) {
        validateText(phone, "Phone is required");
        validateText(storeName, "Store name is required");
        validateText(questionText, "Question text is required");
        validateText(optionA, "Option A is required");
        validateText(optionB, "Option B is required");
        validateText(optionC, "Option C is required");

        String messageBody = QUESTION_MESSAGE_TEMPLATE.formatted(storeName, questionText, optionA, optionB, optionC);

        return sendMessage(phone, messageBody);
    }

    public String sendCorrectAnswerMessage(String phone, String storeName, String branchName,
                                           String campaignTitle, String offerText, String campaignTime,
                                           String branchLocationUrl, String distanceText,
                                           Integer durationMinutes, String qrCode) {
        validateOfferMessage(phone, storeName, branchName, campaignTitle, offerText, campaignTime,
                branchLocationUrl, distanceText, durationMinutes, qrCode);

        String messageBody = CORRECT_ANSWER_MESSAGE_TEMPLATE.formatted(storeName, branchName, campaignTitle,
                offerText, campaignTime, branchLocationUrl, distanceText, durationMinutes, qrCode);

        return sendMessage(phone, messageBody);
    }

    public String sendDirectOfferMessage(String phone, String storeName, String branchName,
                                         String campaignTitle, String offerText, String campaignTime,
                                         String branchLocationUrl, String distanceText,
                                         Integer durationMinutes, String qrCode) {
        validateOfferMessage(phone, storeName, branchName, campaignTitle, offerText, campaignTime,
                branchLocationUrl, distanceText, durationMinutes, qrCode);

        String messageBody = DIRECT_OFFER_MESSAGE_TEMPLATE.formatted(storeName, branchName, campaignTitle,
                offerText, campaignTime, branchLocationUrl, distanceText, durationMinutes, qrCode);

        return sendMessage(phone, messageBody);
    }

    public String sendWrongAnswerMessage(String phone) {
        validateText(phone, "Phone is required");
        return sendMessage(phone, WRONG_ANSWER_MESSAGE);
    }

    public String sendInvalidAnswerMessage(String phone) {
        validateText(phone, "Phone is required");
        return sendMessage(phone, INVALID_ANSWER_MESSAGE);
    }

    public String sendNoOpenCampaignMessage(String phone) {
        validateText(phone, "Phone is required");
        return sendMessage(phone, NO_OPEN_CAMPAIGN_MESSAGE);
    }

    public String sendCampaignEndedMessage(String phone) {
        validateText(phone, "Phone is required");
        return sendMessage(phone, CAMPAIGN_ENDED_MESSAGE);
    }

    @Transactional
    public String receiveWebhook(UltraMsgWebhookIn webhookIn) {
        if (webhookIn == null || webhookIn.getData() == null) {
            throw new ApiException("Webhook payload is required");
        }

        if (Boolean.TRUE.equals(webhookIn.getData().getFromMe())) {
            return "Outgoing message ignored";
        }

        String phone = normalizeWhatsAppPhone(webhookIn.getData().getFrom());
        String selectedOption = normalizeSelectedOption(webhookIn.getData().getBody());

        validateText(phone, "Sender phone is required");
        validateText(selectedOption, "Message body is required");

        if (!isAnswerOption(selectedOption)) {
            sendInvalidAnswerMessage(phone);
            return "Invalid WhatsApp answer message has been sent";
        }

        Customer customer = findCustomerByPhone(phone);
        if (customer == null) {
            sendNoOpenCampaignMessage(phone);
            return "No open campaign message has been sent";
        }

        CampaignMessage message = findOpenMessage(customer.getId());
        if (message == null) {
            sendNoOpenCampaignMessage(phone);
            return "No open campaign message has been sent";
        }

        Campaign campaign = message.getCampaign();
        if (!isCampaignAcceptingAnswers(campaign)) {
            sendCampaignEndedMessage(phone);
            return "Campaign ended message has been sent";
        }

        CustomerAnswerResponseOut answer = customerAnswerService.answerCampaignMessage(customer.getId(), message.getId(), selectedOption);

        if (Boolean.TRUE.equals(answer.getCorrect())) {
            Branch branch = campaign.getBranch();
            QRCode qrCode = qrCodeRepository.findQRCodeByCampaignId(campaign.getId());
            if (qrCode == null) {
                throw new ApiException("Campaign QR code not found");
            }
            sendCorrectAnswerMessage(phone,
                    branch.getStore().getName(),
                    branch.getName(),
                    campaign.getTitle(),
                    campaign.getOfferText(),
                    campaign.getStartDateTime() + " - " + campaign.getEndDateTime(),
                    branch.getLocationUrl(),
                    message.getDistanceText(),
                    message.getDurationMinutes(),
                    qrCode.getCode());
            sendQrCodeEmailSafely(customer, branch, campaign, qrCode);
        } else {
            sendWrongAnswerMessage(phone);
        }

        return "WhatsApp webhook has been handled";
    }

    private Customer findCustomerByPhone(String phone) {
        String normalizedPhone = normalizeStoredPhone(phone);
        for (Customer customer : customerRepository.findAll()) {
            if (customer.getUser() != null
                    && customer.getUser().getPhone() != null
                    && normalizeStoredPhone(customer.getUser().getPhone()).equals(normalizedPhone)) {
                return customer;
            }
        }
        return null;
    }

    private boolean isCampaignAcceptingAnswers(Campaign campaign) {
        if (campaign == null || campaign.getStatus() != CampaignStatus.ACTIVE) return false;
        if (campaign.getStartDateTime() == null || campaign.getEndDateTime() == null) return false;
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(campaign.getStartDateTime()) && !now.isAfter(campaign.getEndDateTime());
    }

    private void sendQrCodeEmailSafely(Customer customer, Branch branch, Campaign campaign, QRCode qrCode) {
        try {
            if (customer.getUser() == null || customer.getUser().getEmail() == null
                    || customer.getUser().getEmail().isBlank()) return;
            byte[] qrPng = Base64.getDecoder().decode(qrCode.getQrImageBase64());
            emailService.sendQrCodeEmail(customer.getUser().getEmail(),
                    branch.getStore().getName(), campaign.getTitle(), qrCode.getCode(), qrPng);
        } catch (Exception e) {
            // email delivery must not break the WhatsApp answer flow
        }
    }

    private CampaignMessage findOpenMessage(Integer customerId) {
        for (CampaignMessage message : campaignMessageRepository
                .findAllByCustomerIdAndStatusOrderBySentAtDesc(customerId, MessageStatus.SENT)) {
            if (message.getCustomerAnswer() == null) {
                return message;
            }
        }
        return null;
    }

    private String normalizeWhatsAppPhone(String phone) {
        validateText(phone, "Sender phone is required");
        return canonicalPhone(phone);
    }

    private String normalizeStoredPhone(String phone) {
        validateText(phone, "Phone is required");
        return canonicalPhone(phone);
    }

    private String canonicalPhone(String phone) {
        return phone.replace("whatsapp:", "")
                .replace("@c.us", "")
                .replace("+", "")
                .replaceAll("\\s", "")
                .trim();
    }

    private String normalizeSelectedOption(String body) {
        validateText(body, "Message body is required");
        return body.trim().toUpperCase();
    }

    private Boolean isAnswerOption(String option) {
        return option.equals("A") || option.equals("B") || option.equals("C");
    }

    private String toUltraMsgNumber(String phone) {
        validateText(phone, "Phone is required");
        return phone.replace("whatsapp:", "")
                .replace("@c.us", "")
                .trim();
    }

    private void validateOfferMessage(String phone, String storeName, String branchName,
                                      String campaignTitle, String offerText, String campaignTime,
                                      String branchLocationUrl, String distanceText,
                                      Integer durationMinutes, String qrCode) {
        validateText(phone, "Phone is required");
        validateText(storeName, "Store name is required");
        validateText(branchName, "Branch name is required");
        validateText(campaignTitle, "Campaign title is required");
        validateText(offerText, "Offer text is required");
        validateText(campaignTime, "Campaign time is required");
        validateText(branchLocationUrl, "Branch location is required");
        validateText(distanceText, "Distance is required");
        validateText(qrCode, "QR code is required");

        if (durationMinutes == null || durationMinutes < 0) {
            throw new ApiException("Duration must be valid");
        }
    }

    private void validateConfiguration() {
        if (instanceId == null || instanceId.isBlank()) {
            throw new ApiException("UltraMsg instance id is not configured");
        }
        if (token == null || token.isBlank()) {
            throw new ApiException("UltraMsg token is not configured");
        }
    }

    private void validateText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(message);
        }
    }
}
