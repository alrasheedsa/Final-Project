package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.Enum.PaymentStatus;
import com.example.fproject.Enum.StoreStatus;
import com.example.fproject.Enum.SubscriptionPlanType;
import com.example.fproject.Enum.SubscriptionStatus;
import com.example.fproject.Model.Branch;
import com.example.fproject.Model.Payment;
import com.example.fproject.Model.Store;
import com.example.fproject.Model.StoreOwner;
import com.example.fproject.Model.Subscription;
import com.example.fproject.Model.User;
import com.example.fproject.Repository.BranchRepository;
import com.example.fproject.Repository.PaymentRepository;
import com.example.fproject.Repository.StoreOwnerRepository;
import com.example.fproject.Repository.StoreRepository;
import com.example.fproject.Repository.SubscriptionRepository;
import com.example.fproject.Repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LemonSqueezyService {

    private final WebClient webClient;
    private final StoreOwnerRepository storeOwnerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final BranchRepository branchRepository;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${lemon.squeezy.store.id}")
    private String storeId;

    @Value("${lemon.squeezy.webhook.secret}")
    private String webhookSecret;

    @Value("${lemon.squeezy.basic-monthly.variant-id}")
    private String basicMonthlyVariantId;

    @Value("${lemon.squeezy.professional-monthly.variant-id}")
    private String professionalMonthlyVariantId;

    @Value("${lemon.squeezy.professional-yearly.variant-id}")
    private String professionalYearlyVariantId;


    @Transactional
    public String createSubscriptionCheckout(String planTypeText, Integer storeOwnerId) {

        StoreOwner storeOwner = findStoreOwnerOrThrow(storeOwnerId);

        if (storeId == null || storeId.isBlank()) {
            throw new ApiException("Lemon Squeezy store ID is not configured");
        }

        SubscriptionPlanType planType = parsePlanType(planTypeText);
        String variantId = getVariantIdByPlanType(planType);

        if (variantId == null || variantId.isBlank()) {
            throw new ApiException("Variant ID for plan " + planType.name() + " is not configured");
        }

        Subscription activeSubscription = subscriptionRepository
                .findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(storeOwnerId, SubscriptionStatus.ACTIVE);

        if (activeSubscription != null && !activeSubscription.getEndDate().isBefore(LocalDate.now())) {
            throw new ApiException("You already have an active subscription");
        }

        Subscription pendingSubscription = subscriptionRepository
                .findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(storeOwnerId, SubscriptionStatus.PENDING);

        if (pendingSubscription != null) {
            throw new ApiException(
                    "You already have a pending subscription. Complete your payment or cancel it first"
            );
        }

        Subscription subscription = new Subscription();
        subscription.setPlanType(planType);
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusMonths(planType.getDurationMonths()));
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setVariantId(variantId);
        subscription.setStoreOwner(storeOwner);
        subscription = subscriptionRepository.save(subscription);

        Payment payment = new Payment();
        payment.setAmount(planType.getPrice());
        payment.setPaymentProvider("LEMON_SQUEEZY");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setSubscription(subscription);
        paymentRepository.save(payment);

        String payload = buildCheckoutPayload(
                storeOwner, subscription, payment, variantId, planType
        );

        String response = sendCheckoutRequest(payload);
        String checkoutUrl = extractCheckoutUrl(response);

        payment.setCheckoutUrl(checkoutUrl);
        paymentRepository.save(payment);

        return checkoutUrl;
    }

    public Subscription getSubscription(Integer storeOwnerId) {

        findStoreOwnerOrThrow(storeOwnerId);

        Subscription subscription = subscriptionRepository
                .findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(storeOwnerId, SubscriptionStatus.ACTIVE);

        if (subscription == null) {
            throw new ApiException("No active subscription found for this store owner");
        }

        return subscription;
    }

    public void cancelLemonSubscription(String lemonSubscriptionId) {

        if (lemonSubscriptionId == null || lemonSubscriptionId.isBlank()) {
            throw new ApiException("Lemon Squeezy subscription id is missing");
        }

        try {
            webClient.delete()
                    .uri("/subscriptions/" + lemonSubscriptionId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

        } catch (Exception e) {
            throw new ApiException("Failed to cancel Lemon Squeezy subscription");
        }
    }

    @Transactional
    public void processWebhook(HttpHeaders headers, String rawBody) {

        String signature = headers.getFirst("X-Signature");
        String eventType = headers.getFirst("X-Event-Name");

        if (!verifySignature(rawBody, signature)) {
            throw new ApiException("Invalid webhook signature");
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);

            if (eventType == null || eventType.isBlank()) {
                eventType = root.path("meta").path("event_name").asText();
            }

            switch (eventType) {
                case "order_created"        -> handleOrderCreated(root);
                case "subscription_created" -> handleSubscriptionCreated(root);
                default -> { }
            }

        } catch (ApiException e) {
            throw e;

        } catch (Exception e) {
            throw new ApiException("Could not process Lemon Squeezy webhook: " + e.getMessage());
        }
    }

    private void handleOrderCreated(JsonNode root) {

        try {
            JsonNode customData = root.path("meta").path("custom_data");

            if (customData.isMissingNode() || customData.isNull()) {
                throw new ApiException("Missing custom data in order_created webhook");
            }

            Integer storeOwnerId   = customData.path("store_owner_id").asInt();
            Integer subscriptionId = customData.path("subscription_id").asInt();
            Integer localPaymentId = customData.path("local_payment_id").asInt();

            if (storeOwnerId == null || storeOwnerId == 0) {
                throw new ApiException("Missing store owner id in webhook payload");
            }

            if (subscriptionId == null || subscriptionId == 0) {
                throw new ApiException("Missing subscription id in webhook payload");
            }

            if (localPaymentId == null || localPaymentId == 0) {
                throw new ApiException("Missing local payment id in webhook payload");
            }

            String status      = root.path("data").path("attributes").path("status").asText();
            String orderNumber = root.path("data").path("attributes").path("order_number").asText();
            Long   total       = root.path("data").path("attributes").path("total").asLong();

            if (orderNumber == null || orderNumber.isBlank()) {
                throw new ApiException("Missing Lemon Squeezy order number");
            }

            if (!"paid".equalsIgnoreCase(status)) {
                throw new ApiException("Order is not paid");
            }

            StoreOwner storeOwner = storeOwnerRepository.findStoreOwnerById(storeOwnerId);
            if (storeOwner == null) {
                throw new ApiException("Store owner not found");
            }

            Subscription subscription = subscriptionRepository.findSubscriptionById(subscriptionId);
            if (subscription == null) {
                throw new ApiException("Subscription not found");
            }

            Payment payment = paymentRepository.findPaymentById(localPaymentId);
            if (payment == null) {
                throw new ApiException("Payment not found");
            }

            // منع ربط نفس orderNumber بدفعة ثانية
            Payment existingPayment = paymentRepository.findPaymentByTransactionId(orderNumber);
            if (existingPayment != null && !existingPayment.getId().equals(payment.getId())) {
                throw new ApiException("Lemon Squeezy transaction id is already linked to another payment");
            }

            // لو الدفع مكتمل مسبقاً بنفس الـ orderNumber نتجاهل (idempotency)
            if (payment.getStatus() == PaymentStatus.PAID
                    && orderNumber.equals(payment.getTransactionId())) {
                return;
            }

            // المبلغ الفعلي من Lemon Squeezy — لو جاي صح نستخدمه، وإلا نحتفظ بمبلغ الباقة
            Double amount = total != null && total > 0
                    ? total / 100.0
                    : subscription.getPlanType().getPrice();

            // تحديث الدفع
            payment.setAmount(amount);
            payment.setTransactionId(orderNumber);
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            payment.setPaymentProvider("LEMON_SQUEEZY");
            payment.setSubscription(subscription);
            paymentRepository.save(payment);

            // تفعيل الاشتراك
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepository.save(subscription);

            // تفعيل الحساب والمتاجر والفروع
            activateAccountStoresAndBranches(subscription);

        } catch (ApiException e) {
            throw e;

        } catch (Exception e) {
            throw new ApiException("Unexpected error while processing order: " + e.getMessage());
        }
    }

    private void handleSubscriptionCreated(JsonNode root) {

        JsonNode customData = root.path("meta").path("custom_data");

        if (customData.isMissingNode() || customData.isNull()) {
            throw new ApiException("Missing custom_data in subscription_created webhook");
        }

        int subscriptionId = customData.path("subscription_id").asInt();
        if (subscriptionId == 0) throw new ApiException("Missing subscription_id in webhook");

        Subscription subscription = subscriptionRepository.findSubscriptionById(subscriptionId);
        if (subscription == null) throw new ApiException("Subscription not found");

        JsonNode data       = root.path("data");
        JsonNode attributes = data.path("attributes");

        subscription.setLemonSubscriptionId(data.path("id").asText());
        subscription.setVariantId(attributes.path("variant_id").asText());
        subscription.setProductName(attributes.path("product_name").asText());
        subscription.setLemonStatus(attributes.path("status").asText());
        subscription.setRenewsAt(attributes.path("renews_at").asText());

        subscriptionRepository.save(subscription);
    }

    private void activateAccountStoresAndBranches(Subscription subscription) {

        User user = subscription.getStoreOwner().getUser();
        user.setEnabled(true);
        userRepository.save(user);

        List<Store> stores = storeRepository.findStoresByStoreOwnerId(
                subscription.getStoreOwner().getId()
        );

        for (Store store : stores) {


            if (store.getStatus() == StoreStatus.PENDING
                    && Boolean.TRUE.equals(store.getCommercialRegisterVerified())) {

                store.setStatus(StoreStatus.ACTIVE);
                storeRepository.save(store);

                List<Branch> branches = branchRepository.findBranchesByStoreId(store.getId());

                for (Branch branch : branches) {
                    if (branch.getStatus() == StoreStatus.PENDING) {
                        branch.setStatus(StoreStatus.ACTIVE);
                        branchRepository.save(branch);
                    }
                }
            }
        }
    }

    private SubscriptionPlanType parsePlanType(String planTypeText) {
        try {
            return SubscriptionPlanType.valueOf(planTypeText.toUpperCase());
        } catch (Exception e) {
            throw new ApiException(
                    "Invalid subscription plan type: " + planTypeText +
                            ". Valid values: BASIC_MONTHLY, PROFESSIONAL_MONTHLY, PROFESSIONAL_YEARLY"
            );
        }
    }

    private String getVariantIdByPlanType(SubscriptionPlanType planType) {
        return switch (planType) {
            case BASIC_MONTHLY        -> basicMonthlyVariantId;
            case PROFESSIONAL_MONTHLY -> professionalMonthlyVariantId;
            case PROFESSIONAL_YEARLY  -> professionalYearlyVariantId;
        };
    }


    private SubscriptionPlanType resolvePlanTypeFromVariantId(String variantId) {

        if (variantId == null || variantId.isBlank()) {
            throw new ApiException("Variant ID is missing from webhook — cannot determine plan type");
        }

        if (variantId.equals(basicMonthlyVariantId))        return SubscriptionPlanType.BASIC_MONTHLY;
        if (variantId.equals(professionalMonthlyVariantId)) return SubscriptionPlanType.PROFESSIONAL_MONTHLY;
        if (variantId.equals(professionalYearlyVariantId))  return SubscriptionPlanType.PROFESSIONAL_YEARLY;

        throw new ApiException(
                "Unknown variant ID received from Lemon Squeezy: " + variantId +
                        ". Payment recorded but plan type could not be determined"
        );
    }


    private String buildCheckoutPayload(
            StoreOwner storeOwner,
            Subscription subscription,
            Payment payment,
            String variantId,
            SubscriptionPlanType planType
    ) {
        return String.format("""
                {
                  "data": {
                    "type": "checkouts",
                    "attributes": {
                      "checkout_data": {
                        "email": "%s",
                        "name": "%s",
                        "custom": {
                          "store_owner_id": "%s",
                          "subscription_id": "%s",
                          "local_payment_id": "%s",
                          "plan_type": "%s"
                        }
                      }
                    },
                    "relationships": {
                      "store": {
                        "data": { "type": "stores", "id": "%s" }
                      },
                      "variant": {
                        "data": { "type": "variants", "id": "%s" }
                      }
                    }
                  }
                }
                """,
                storeOwner.getUser().getEmail(),
                storeOwner.getUser().getFullName(),
                storeOwner.getId(),
                subscription.getId(),
                payment.getId(),
                planType.name(),
                storeId,
                variantId
        );
    }

    private String sendCheckoutRequest(String payload) {
        try {
            return webClient.post()
                    .uri("/checkouts")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

        } catch (Exception e) {
            throw new ApiException("Failed to create checkout session with Lemon Squeezy");
        }
    }

    private String extractCheckoutUrl(String response) {

        if (response == null || response.isBlank()) {
            throw new ApiException("Empty response from Lemon Squeezy");
        }

        try {
            JSONObject json = new JSONObject(response);
            return json.getJSONObject("data")
                    .getJSONObject("attributes")
                    .getString("url");

        } catch (Exception e) {
            throw new ApiException("Could not extract checkout URL from Lemon Squeezy response");
        }
    }


    private boolean verifySignature(String rawBody, String signature) {

        if (signature == null || signature.isBlank()) {
            return false;
        }

        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
            );
            sha256Hmac.init(secretKey);

            byte[] hashBytes = sha256Hmac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String computed  = Hex.encodeHexString(hashBytes);

            return computed.equals(signature);

        } catch (Exception e) {
            throw new ApiException("Failed to verify webhook signature");
        }
    }

    private StoreOwner findStoreOwnerOrThrow(Integer storeOwnerId) {
        StoreOwner storeOwner = storeOwnerRepository.findStoreOwnerById(storeOwnerId);
        if (storeOwner == null) {
            throw new ApiException("Store owner not found");
        }
        return storeOwner;
    }
}
