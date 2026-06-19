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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONArray;
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

        StoreOwner storeOwner = storeOwnerRepository.findStoreOwnerById(storeOwnerId);

        if (storeOwner == null) {
            throw new ApiException("Store owner not found");
        }

        if (storeId == null || storeId.isBlank()) {
            throw new ApiException("Store ID is not configured");
        }

        SubscriptionPlanType planType = parsePlanType(planTypeText);
        String variantId = getVariantIdByPlanType(planType);

        Subscription activeSubscription =
                subscriptionRepository.findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(
                        storeOwnerId,
                        SubscriptionStatus.ACTIVE
                );

        if (activeSubscription != null && !activeSubscription.getEndDate().isBefore(LocalDate.now())) {
            throw new ApiException("Store owner already has an active subscription");
        }

        Subscription pendingSubscription =
                subscriptionRepository.findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(
                        storeOwnerId,
                        SubscriptionStatus.PENDING
                );

        if (pendingSubscription != null) {
            throw new ApiException("Store owner already has a pending subscription");
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

        String payload = String.format("""
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
                          "variant_id": "%s",
                          "plan_type": "%s"
                        }
                      }
                    },
                    "relationships": {
                      "store": {
                        "data": {
                          "type": "stores",
                          "id": "%s"
                        }
                      },
                      "variant": {
                        "data": {
                          "type": "variants",
                          "id": "%s"
                        }
                      }
                    }
                  }
                }
                """,
                storeOwner.getUser().getEmail(),
                storeOwner.getUser().getFullName(),
                storeOwnerId,
                subscription.getId(),
                payment.getId(),
                variantId,
                planType.name(),
                storeId,
                variantId
        );

        String response = sendCheckoutRequest(payload);
        String checkoutUrl = extractCheckoutUrl(response);

        payment.setCheckoutUrl(checkoutUrl);
        paymentRepository.save(payment);

        return checkoutUrl;
    }

    public Subscription getSubscription(Integer storeOwnerId) {

        StoreOwner storeOwner = storeOwnerRepository.findStoreOwnerById(storeOwnerId);

        if (storeOwner == null) {
            throw new ApiException("Store owner not found");
        }

        Subscription subscription =
                subscriptionRepository.findFirstByStoreOwnerIdAndStatusOrderByEndDateDesc(
                        storeOwnerId,
                        SubscriptionStatus.ACTIVE
                );

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

    private String sendCheckoutRequest(String payload) {

        try {
            return webClient.post()
                    .uri("/checkouts")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

        } catch (Exception e) {
            throw new ApiException("Failed to create checkout session");
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
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(rawBody);

            if (eventType == null || eventType.isBlank()) {
                eventType = root.path("meta").path("event_name").asText();
            }

            if ("order_created".equals(eventType)) {
                handleOrderCreated(rawBody);
                return;
            }

            if ("subscription_created".equals(eventType)) {
                handleSubscriptionCreated(rawBody);
                return;
            }

        } catch (ApiException e) {
            throw e;

        } catch (Exception e) {
            throw new ApiException("Could not process Lemon Squeezy webhook: " + e.getMessage());
        }
    }

    private void handleOrderCreated(String rawBody) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(rawBody);

            JsonNode customData = root.path("meta").path("custom_data");

            if (customData.isMissingNode() || customData.isNull()) {
                throw new ApiException("Missing custom data in order_created webhook");
            }

            Integer storeOwnerId = customData.path("store_owner_id").asInt();
            Integer subscriptionId = customData.path("subscription_id").asInt();
            Integer localPaymentId = customData.path("local_payment_id").asInt();

            String status = root.path("data").path("attributes").path("status").asText();
            String orderNumber = root.path("data").path("attributes").path("order_number").asText();
            Long total = root.path("data").path("attributes").path("total").asLong();
            String currency = root.path("data").path("attributes").path("currency").asText();

            if (storeOwnerId == null || storeOwnerId == 0) {
                throw new ApiException("Missing store owner id in webhook payload");
            }

            if (subscriptionId == null || subscriptionId == 0) {
                throw new ApiException("Missing subscription id in webhook payload");
            }

            if (localPaymentId == null || localPaymentId == 0) {
                throw new ApiException("Missing local payment id in webhook payload");
            }

            if (orderNumber == null || orderNumber.isBlank()) {
                throw new ApiException("Missing Lemon Squeezy order number");
            }

            if (!"paid".equalsIgnoreCase(status)) {
                throw new ApiException("Order is not paid");
            }

            if (!"SAR".equalsIgnoreCase(currency)) {
                throw new ApiException("Lemon Squeezy currency does not match local payment currency");
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

            if (!payment.getSubscription().getId().equals(subscriptionId)
                    || !subscription.getStoreOwner().getId().equals(storeOwnerId)) {
                throw new ApiException("Webhook payment, subscription, and store owner do not match");
            }

            if (payment.getStatus() == PaymentStatus.PAID) {
                if (orderNumber.equals(payment.getTransactionId())) {
                    return;
                }
                throw new ApiException("Payment is already linked to another Lemon Squeezy order");
            }

            if (payment.getStatus() != PaymentStatus.PENDING) {
                throw new ApiException("Only pending payment can be marked as paid");
            }

            if (subscription.getStatus() != SubscriptionStatus.PENDING) {
                throw new ApiException("Only pending subscription can be activated");
            }

            Payment existingPayment = paymentRepository.findPaymentByTransactionId(orderNumber);

            if (existingPayment != null && !existingPayment.getId().equals(payment.getId())) {
                throw new ApiException("Lemon Squeezy transaction id is already linked to another payment");
            }

            long expectedAmountInCents = Math.round(payment.getAmount() * 100);
            if (total == null || total != expectedAmountInCents) {
                throw new ApiException("Lemon Squeezy amount does not match local payment amount");
            }

            payment.setTransactionId(orderNumber);
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            payment.setPaymentProvider("LEMON_SQUEEZY");
            payment.setSubscription(subscription);

            paymentRepository.save(payment);

            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepository.save(subscription);

            activateStoreOwnerAccountStoresAndBranches(subscription);

        } catch (JsonProcessingException e) {
            throw new ApiException("Failed to parse JSON order data");

        } catch (ApiException e) {
            throw e;

        } catch (Exception e) {
            throw new ApiException("Unexpected error while processing order: " + e.getMessage());
        }
    }

    private void handleSubscriptionCreated(String rawBody) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(rawBody);

            JsonNode customData = root.path("meta").path("custom_data");

            if (customData.isMissingNode() || customData.isNull()) {
                throw new ApiException("Missing custom data in subscription_created webhook");
            }

            Integer subscriptionId = customData.path("subscription_id").asInt();

            if (subscriptionId == null || subscriptionId == 0) {
                throw new ApiException("Missing subscription id in subscription_created webhook");
            }

            Subscription subscription = subscriptionRepository.findSubscriptionById(subscriptionId);

            if (subscription == null) {
                throw new ApiException("Subscription not found");
            }

            JsonNode data = root.path("data");
            JsonNode attributes = data.path("attributes");

            String lemonSubscriptionId = data.path("id").asText();
            String variantId = attributes.path("variant_id").asText();
            String productName = attributes.path("product_name").asText();
            String status = attributes.path("status").asText();
            String renewsAt = attributes.path("renews_at").asText();

            subscription.setLemonSubscriptionId(lemonSubscriptionId);
            subscription.setVariantId(variantId);
            subscription.setProductName(productName);
            subscription.setLemonStatus(status);
            subscription.setRenewsAt(renewsAt);

            subscriptionRepository.save(subscription);

        } catch (JsonProcessingException e) {
            throw new ApiException("Failed to parse JSON subscription data");

        } catch (ApiException e) {
            throw e;

        } catch (Exception e) {
            throw new ApiException("Unexpected error while processing subscription: " + e.getMessage());
        }
    }

    private void activateStoreOwnerAccountStoresAndBranches(Subscription subscription) {

        User user = subscription.getStoreOwner().getUser();
        user.setEnabled(true);
        userRepository.save(user);

        List<Store> stores = storeRepository.findStoresByStoreOwnerId(subscription.getStoreOwner().getId());

        for (Store store : stores) {

            if (Boolean.TRUE.equals(store.getCommercialRegisterVerified())) {

                store.setStatus(StoreStatus.ACTIVE);
                storeRepository.save(store);

                List<Branch> branches = branchRepository.findBranchesByStoreId(store.getId());

                for (Branch branch : branches) {
                    branch.setStatus(StoreStatus.ACTIVE);
                    branchRepository.save(branch);
                }
            }
        }
    }

    private SubscriptionPlanType parsePlanType(String planTypeText) {

        try {
            return SubscriptionPlanType.valueOf(planTypeText.toUpperCase());

        } catch (Exception e) {
            throw new ApiException("Invalid subscription plan type");
        }
    }

    private String getVariantIdByPlanType(SubscriptionPlanType planType) {

        return switch (planType) {
            case BASIC_MONTHLY -> basicMonthlyVariantId;
            case PROFESSIONAL_MONTHLY -> professionalMonthlyVariantId;
            case PROFESSIONAL_YEARLY -> professionalYearlyVariantId;
        };
    }

    private boolean verifySignature(String rawBody, String signature) {

        if (signature == null || signature.isBlank()) {
            return false;
        }

        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");

            SecretKeySpec secretKey =
                    new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

            sha256Hmac.init(secretKey);

            byte[] hashBytes = sha256Hmac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));

            String computedSignature = Hex.encodeHexString(hashBytes);

            return computedSignature.equals(signature);

        } catch (Exception e) {
            throw new ApiException("Failed to verify webhook signature");
        }
    }

    private String extractCheckoutUrl(String response) {

        if (response == null || response.isBlank()) {
            throw new ApiException("Empty response from payment provider");
        }

        JSONObject json = new JSONObject(response);

        return json.getJSONObject("data")
                .getJSONObject("attributes")
                .getString("url");
    }
}