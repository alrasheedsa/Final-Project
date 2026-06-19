package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.WhatsAppWebhookIn;
import com.example.fproject.DTO.OUT.CustomerAnswerResponseOut;
import com.example.fproject.Enum.MessageStatus;
import com.example.fproject.Model.Branch;
import com.example.fproject.Model.Campaign;
import com.example.fproject.Model.CampaignMessage;
import com.example.fproject.Model.Customer;
import com.example.fproject.Model.QRCode;
import com.example.fproject.Repository.CampaignMessageRepository;
import com.example.fproject.Repository.CustomerRepository;
import com.example.fproject.Repository.QRCodeRepository;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WhatsAppService {

    private final CustomerRepository customerRepository;
    private final CampaignMessageRepository campaignMessageRepository;
    private final QRCodeRepository qrCodeRepository;
    private final CustomerAnswerService customerAnswerService;

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.whatsapp.number}")
    private String fromNumber;

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

    public String sendMessage(String phone, String messageBody) {
        validateConfiguration();
        validateText(phone, "Phone is required");
        validateText(messageBody, "Message is required");
        Twilio.init(accountSid, authToken);
        Message.creator(
                new PhoneNumber(toWhatsAppNumber(phone)),
                new PhoneNumber(toWhatsAppNumber(fromNumber)),
                messageBody
        ).create();
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

    @Transactional
    public String receiveWebhook(WhatsAppWebhookIn webhookIn) {
        if (webhookIn == null) {
            throw new ApiException("Webhook payload is required");
        }

        String phone = normalizeWhatsAppPhone(webhookIn.getFrom());
        String selectedOption = normalizeSelectedOption(webhookIn.getBody());

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

        CustomerAnswerResponseOut answer = customerAnswerService.answerCampaignMessage(message.getId(), selectedOption);

        if (Boolean.TRUE.equals(answer.getCorrect())) {
            Campaign campaign = message.getCampaign();
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
        return phone.replace("whatsapp:", "").trim();
    }

    private String normalizeStoredPhone(String phone) {
        String value = phone.replace("whatsapp:", "").replaceAll("[^0-9]", "");
        if (value.startsWith("966")) {
            return "0" + value.substring(3);
        }
        if (value.startsWith("5")) {
            return "0" + value;
        }
        return value;
    }

    private String normalizeSelectedOption(String body) {
        validateText(body, "Message body is required");
        return body.trim().toUpperCase();
    }

    private Boolean isAnswerOption(String option) {
        return option.equals("A") || option.equals("B") || option.equals("C");
    }

    private String toWhatsAppNumber(String phone) {
        validateText(phone, "Phone is required");
        String cleanPhone = phone.trim();
        if (cleanPhone.startsWith("whatsapp:")) {
            return cleanPhone;
        }
        return "whatsapp:" + cleanPhone;
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
        if (accountSid == null || accountSid.isBlank()) {
            throw new ApiException("Twilio account SID is not configured");
        }
        if (authToken == null || authToken.isBlank()) {
            throw new ApiException("Twilio auth token is not configured");
        }
        if (fromNumber == null || fromNumber.isBlank()) {
            throw new ApiException("Twilio WhatsApp number is not configured");
        }
    }

    private void validateText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(message);
        }
    }
}
