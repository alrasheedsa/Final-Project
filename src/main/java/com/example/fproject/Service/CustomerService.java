package com.example.fproject.Service;

import com.example.fproject.Api.ApiException;
import com.example.fproject.DTO.IN.CustomerIn;
import com.example.fproject.DTO.OUT.CampaignMessageResponseOut;
import com.example.fproject.DTO.OUT.CampaignResponseOut;
import com.example.fproject.DTO.OUT.CustomerOut;
import com.example.fproject.DTO.OUT.QRCodeResponseOut;
import com.example.fproject.Enum.CampaignStatus;
import com.example.fproject.Enum.MessageStatus;
import com.example.fproject.Enum.QRCodeStatus;
import com.example.fproject.Enum.RoleType;
import com.example.fproject.Model.*;
import com.example.fproject.Repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final GoogleMapService googleMapService;
    private final BranchRepository branchRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignMessageRepository campaignMessageRepository;
    private final QRCodeRepository qrCodeRepository;
    private final QRRedemptionRepository qrRedemptionRepository;

    @Transactional
    public CustomerOut registerCustomer(CustomerIn dto) {

        if (userRepository.existsUserByEmail(dto.getEmail())) {
            throw new ApiException("Email already exists");
        }

        if (userRepository.existsUserByPhone(dto.getPhone())) {
            throw new ApiException("Phone already exists");
        }

        double[] coordinates = googleMapService.extractLocationFromLink(dto.getLocationUrl());

        User user = new User();
        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setPassword(new BCryptPasswordEncoder().encode(dto.getPassword()));
        user.setRole(RoleType.CUSTOMER);
        user.setEnabled(true);

        userRepository.save(user);

        Customer customer = new Customer();
        customer.setUser(user);
        customer.setLocationUrl(dto.getLocationUrl());
        customer.setLatitude(coordinates[0]);
        customer.setLongitude(coordinates[1]);
        customer.setLocationConsent(dto.getLocationConsent());

        customerRepository.save(customer);

        return mapToDTOOUT(customer);
    }

    public List<CustomerOut> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll();
        List<CustomerOut> result = new ArrayList<>();
        for (Customer customer : customers) {
            result.add(mapToDTOOUT(customer));
        }
        return result;
    }

    // يُستخدم للادمن (بالـ ID) وللـ customer نفسه (user.getId() == customer.getId() بسبب @MapsId)
    public CustomerOut getCustomerById(Integer customerId) {
        Customer customer = customerRepository.findCustomerById(customerId);
        if (customer == null) throw new ApiException("Customer not found");
        return mapToDTOOUT(customer);
    }

    @Transactional
    public CustomerOut updateCustomer(Integer userId, CustomerIn dto) {
        Customer customer = customerRepository.findCustomerById(userId);
        if (customer == null) throw new ApiException("Customer not found");

        User user = customer.getUser();

        if (!user.getEmail().equals(dto.getEmail()) && userRepository.existsUserByEmail(dto.getEmail())) {
            throw new ApiException("Email already exists");
        }

        if (!user.getPhone().equals(dto.getPhone()) && userRepository.existsUserByPhone(dto.getPhone())) {
            throw new ApiException("Phone already exists");
        }

        double[] coordinates = googleMapService.extractLocationFromLink(dto.getLocationUrl());

        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setPassword(new BCryptPasswordEncoder().encode(dto.getPassword()));

        userRepository.save(user);

        customer.setLocationUrl(dto.getLocationUrl());
        customer.setLatitude(coordinates[0]);
        customer.setLongitude(coordinates[1]);
        customer.setLocationConsent(dto.getLocationConsent());

        customerRepository.save(customer);

        return mapToDTOOUT(customer);
    }

    @Transactional
    public void deleteCustomer(Integer userId) {
        Customer customer = customerRepository.findCustomerById(userId);
        if (customer == null) throw new ApiException("Customer not found");

        if (customer.getCampaignMessages() != null && !customer.getCampaignMessages().isEmpty()) {
            throw new ApiException("Cannot delete customer because it has campaign messages");
        }

        if (customer.getCustomerAnswers() != null && !customer.getCustomerAnswers().isEmpty()) {
            throw new ApiException("Cannot delete customer because it has customer answers");
        }

        User user = customer.getUser();
        customerRepository.delete(customer);
        userRepository.delete(user);
    }

    public List<CustomerOut> getCustomersWithLocationConsent() {
        List<Customer> customers = customerRepository.findCustomersByLocationConsentTrue();
        List<CustomerOut> result = new ArrayList<>();
        for (Customer customer : customers) {
            result.add(mapToDTOOUT(customer));
        }
        return result;
    }

    public CustomerOut getCustomerByPhone(String phone) {
        if (phone == null || phone.isBlank()) throw new ApiException("Phone is required");
        Customer customer = customerRepository.findCustomerByUser_Phone(phone);
        if (customer == null) throw new ApiException("Customer not found");
        return mapToDTOOUT(customer);
    }

    public List<CustomerOut> getCustomersInsideRadius(Integer branchId) {
        Branch branch = branchRepository.findBranchById(branchId);
        if (branch == null) throw new ApiException("Branch not found");
        if (branch.getLatitude() == null || branch.getLongitude() == null)
            throw new ApiException("Branch location coordinates are required");
        if (branch.getCampaignRadiusMeters() == null || branch.getCampaignRadiusMeters() <= 0)
            throw new ApiException("Branch campaign radius is required");

        List<Customer> customers = customerRepository.findCustomersByLocationConsentTrue();
        List<CustomerOut> result = new ArrayList<>();

        for (Customer customer : customers) {
            if (customer.getLatitude() == null || customer.getLongitude() == null) continue;
            double distance = calculateDistanceInMeters(
                    branch.getLatitude(), branch.getLongitude(),
                    customer.getLatitude(), customer.getLongitude()
            );
            if (distance <= branch.getCampaignRadiusMeters()) {
                result.add(mapToDTOOUT(customer));
            }
        }
        return result;
    }

    public List<CampaignResponseOut> getCampaignsInRadius(Integer customerId) {
        Customer customer = findCustomerWithLocationOrThrow(customerId);
        return campaignRepository.findAll()
                .stream()
                .filter(c -> isCustomerInCampaignRadius(customer, c))
                .map(this::mapCampaignToOut)
                .toList();
    }

    public List<CampaignResponseOut> getActiveCampaignsInRadius(Integer customerId) {
        Customer customer = findCustomerWithLocationOrThrow(customerId);
        return campaignRepository.findAllByStatus(CampaignStatus.ACTIVE)
                .stream()
                .filter(c -> isCustomerInCampaignRadius(customer, c))
                .map(this::mapCampaignToOut)
                .toList();
    }

    public List<CampaignResponseOut> getExpiredCampaignsInRadius(Integer customerId) {
        Customer customer = findCustomerWithLocationOrThrow(customerId);
        return campaignRepository.findAll()
                .stream()
                .filter(c -> c.getStatus() == CampaignStatus.COMPLETED
                          || c.getStatus() == CampaignStatus.EXPIRED)
                .filter(c -> isCustomerInCampaignRadius(customer, c))
                .map(this::mapCampaignToOut)
                .toList();
    }

    public List<CampaignResponseOut> getUsedCampaigns(Integer customerId) {
        findCustomerOrThrow(customerId);
        return qrRedemptionRepository.findAll()
                .stream()
                .filter(r -> r.getCustomer() != null && r.getCustomer().getId().equals(customerId))
                .map(QRRedemption::getCampaign)
                .filter(c -> c != null)
                .distinct()
                .map(this::mapCampaignToOut)
                .toList();
    }

    public List<CampaignMessageResponseOut> getCustomerOffers(Integer customerId) {
        findCustomerOrThrow(customerId);
        return campaignMessageRepository.findAllByCustomerId(customerId)
                .stream()
                .map(this::mapMessageToOut)
                .toList();
    }

    public List<CampaignMessageResponseOut> getActiveMessages(Integer customerId) {
        findCustomerOrThrow(customerId);
        return campaignMessageRepository
                .findAllByCustomerIdAndStatusOrderBySentAtDesc(customerId, MessageStatus.SENT)
                .stream()
                .filter(m -> m.getCampaign() != null && m.getCampaign().getStatus() == CampaignStatus.ACTIVE)
                .map(this::mapMessageToOut)
                .toList();
    }

    public List<CampaignMessageResponseOut> getAnsweredMessages(Integer customerId) {
        findCustomerOrThrow(customerId);
        return campaignMessageRepository.findAllByCustomerId(customerId)
                .stream()
                .filter(m -> m.getCustomerAnswer() != null)
                .map(this::mapMessageToOut)
                .toList();
    }

    public List<CampaignMessageResponseOut> getUnansweredMessages(Integer customerId) {
        findCustomerOrThrow(customerId);
        return campaignMessageRepository.findAllByCustomerId(customerId)
                .stream()
                .filter(m -> m.getCustomerAnswer() == null)
                .map(this::mapMessageToOut)
                .toList();
    }

    public List<QRCodeResponseOut> getCustomerQRCodes(Integer customerId) {
        findCustomerOrThrow(customerId);
        return campaignMessageRepository.findAllByCustomerId(customerId)
                .stream()
                .map(m -> m.getCampaign())
                .filter(c -> c != null)
                .distinct()
                .map(c -> qrCodeRepository.findQRCodeByCampaignId(c.getId()))
                .filter(qr -> qr != null)
                .map(this::mapQRCodeToOut)
                .toList();
    }

    public List<QRCodeResponseOut> getAvailableQRCodes(Integer customerId) {
        findCustomerOrThrow(customerId);
        return campaignMessageRepository.findAllByCustomerId(customerId)
                .stream()
                .map(m -> m.getCampaign())
                .filter(c -> c != null && c.getStatus() == CampaignStatus.ACTIVE)
                .distinct()
                .map(c -> qrCodeRepository.findQRCodeByCampaignId(c.getId()))
                .filter(qr -> qr != null && qr.getStatus() == QRCodeStatus.ACTIVE)
                .map(this::mapQRCodeToOut)
                .toList();
    }

    public List<QRCodeResponseOut> getUsedQRCodes(Integer customerId) {
        findCustomerOrThrow(customerId);
        return qrRedemptionRepository.findAll()
                .stream()
                .filter(r -> r.getCustomer() != null && r.getCustomer().getId().equals(customerId))
                .map(QRRedemption::getQrCode)
                .filter(qr -> qr != null)
                .distinct()
                .map(this::mapQRCodeToOut)
                .toList();
    }

    private Customer findCustomerOrThrow(Integer customerId) {
        Customer customer = customerRepository.findCustomerById(customerId);
        if (customer == null) throw new ApiException("Customer not found");
        return customer;
    }

    private Customer findCustomerWithLocationOrThrow(Integer customerId) {
        Customer customer = findCustomerOrThrow(customerId);
        if (!Boolean.TRUE.equals(customer.getLocationConsent()))
            throw new ApiException("Customer has not given location consent");
        if (customer.getLatitude() == null || customer.getLongitude() == null)
            throw new ApiException("Customer location is not set");
        return customer;
    }

    private boolean isCustomerInCampaignRadius(Customer customer, Campaign campaign) {
        if (campaign.getBranch() == null) return false;
        Branch branch = campaign.getBranch();
        if (branch.getLatitude() == null || branch.getLongitude() == null) return false;
        if (branch.getCampaignRadiusMeters() == null) return false;
        double distance = calculateDistanceInMeters(
                customer.getLatitude(), customer.getLongitude(),
                branch.getLatitude(), branch.getLongitude()
        );
        return distance <= branch.getCampaignRadiusMeters();
    }

    private double calculateDistanceInMeters(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return Double.MAX_VALUE;
        final int earthRadiusMeters = 6371000;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        return earthRadiusMeters * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private CustomerOut mapToDTOOUT(Customer customer) {
        return new CustomerOut(
                customer.getId(),
                customer.getUser().getFullName(),
                customer.getUser().getPhone(),
                customer.getUser().getEmail(),
                customer.getUser().getCreatedAt(),
                customer.getLocationUrl(),
                customer.getLatitude(),
                customer.getLongitude(),
                customer.getLocationConsent()
        );
    }

    private CampaignResponseOut mapCampaignToOut(Campaign c) {
        return new CampaignResponseOut(
                c.getId(), c.getTitle(), c.getDescription(), c.getOfferText(),
                c.getCampaignType(), c.getStatus(), c.getStartDateTime(), c.getEndDateTime(),
                c.getTargetCustomersCount(), c.getSentCount(), c.getRedeemedCount(),
                c.getBranch() != null ? c.getBranch().getId() : null,
                c.getCampaignSuggestion() != null ? c.getCampaignSuggestion().getId() : null,
                c.getAiQuestion() != null ? c.getAiQuestion().getId() : null,
                c.getQrCode() != null ? c.getQrCode().getId() : null,
                c.getCampaignResult() != null ? c.getCampaignResult().getId() : null
        );
    }

    private CampaignMessageResponseOut mapMessageToOut(CampaignMessage m) {
        return new CampaignMessageResponseOut(
                m.getId(), m.getMessageText(), m.getDistanceKm(), m.getDurationMinutes(),
                m.getDistanceText(), m.getStatus(), m.getSentAt(),
                m.getCampaign() != null ? m.getCampaign().getId() : null,
                m.getCustomer() != null ? m.getCustomer().getId() : null,
                m.getCustomerAnswer() != null ? m.getCustomerAnswer().getId() : null
        );
    }

    private QRCodeResponseOut mapQRCodeToOut(QRCode qr) {
        return new QRCodeResponseOut(
                qr.getId(), qr.getCode(), qr.getQrImageBase64(),
                qr.getMaxUsageCount(), qr.getUsedCount(), qr.getStatus(),
                qr.getCampaign() != null ? qr.getCampaign().getId() : null
        );
    }
}