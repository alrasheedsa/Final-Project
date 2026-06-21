package com.example.fproject.Config;

import com.example.fproject.Service.MyUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class ConfigSecurity {

    private final MyUserDetailsService myUserDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .userDetailsService(myUserDetailsService)
                .authorizeHttpRequests(auth -> auth

                        // ===== PUBLIC =====
                        .requestMatchers(
                                "/api/v1/store-owner/register",
                                "/api/v1/customer/register",
                                "/api/v1/payment/webhook",
                                "/api/v1/whatsapp/webhook",
                                "/api/v1/subscription/plans"
                        ).permitAll()

                        // ===== ADMIN only =====
                        .requestMatchers(
                                // Store Owner
                                "/api/v1/store-owner/get",
                                "/api/v1/store-owner/get/**",
                                // Customer
                                "/api/v1/customer/get",
                                "/api/v1/customer/get/**",
                                "/api/v1/customer/location-consent",
                                "/api/v1/customer/get-by-phone",
                                "/api/v1/customer/inside-radius/**",
                                // Store
                                "/api/v1/store/get",
                                "/api/v1/store/activate/**",
                                "/api/v1/store/deactivate/**",
                                // Subscription
                                "/api/v1/subscription/get",
                                "/api/v1/subscription/get/**",
                                "/api/v1/subscription/check-expired",
                                // Campaign
                                "/api/v1/campaigns/get",
                                "/api/v1/campaigns/expire-finished",
                                "/api/v1/campaigns/check-finished",
                                "/api/v1/campaigns/start-ready",
                                // Campaign Result
                                "/api/v1/campaign-results/get",
                                "/api/v1/campaign-results/generate-finished",
                                // Campaign Message
                                "/api/v1/campaign-messages/get",
                                "/api/v1/campaign-messages/customer/**",
                                // Others
                                "/api/v1/ai-analysis/get",
                                "/api/v1/ai-questions/get",
                                "/api/v1/campaign-suggestion/get",
                                "/api/v1/sales-record/get",
                                "/api/v1/sales-record-item/get",
                                "/api/v1/monthly-report/get",
                                "/api/v1/qr-codes/get",
                                "/api/v1/qr-redemptions/get",
                                "/api/v1/customer-answers/get"
                        ).hasAuthority("ADMIN")

                        // ===== STORE_OWNER only =====
                        .requestMatchers(
                                // Profile
                                "/api/v1/store-owner/my",
                                "/api/v1/store-owner/update",
                                "/api/v1/store-owner/delete",
                                // Store
                                "/api/v1/store/add",
                                "/api/v1/store/get/**",
                                "/api/v1/store/my-stores",
                                "/api/v1/store/update/**",
                                "/api/v1/store/delete/**",
                                // Branch
                                "/api/v1/branch/**",
                                // Campaign
                                "/api/v1/campaigns/**",
                                // Campaign Messages
                                "/api/v1/campaign-messages/**",
                                // Campaign Results
                                "/api/v1/campaign-results/**",
                                // Campaign Suggestions
                                "/api/v1/campaign-suggestion/**",
                                // AI
                                "/api/v1/ai-analysis/**",
                                "/api/v1/ai-questions/**",
                                // Sales
                                "/api/v1/sales-record/**",
                                "/api/v1/sales-record-item/**",
                                // Reports
                                "/api/v1/monthly-report/**",
                                // Holidays
                                "/api/v1/holidays/**",
                                // QR
                                "/api/v1/qr-codes/**",
                                // Subscription
                                "/api/v1/subscription/my",
                                "/api/v1/subscription/my/**",
                                "/api/v1/subscription/cancel/**",
                                // Payment
                                "/api/v1/payment/subscribe/**",
                                "/api/v1/payment/subscription",
                                // Customer Answers (STORE_OWNER يشوف إجابات عملائه)
                                "/api/v1/customer-answers/get/**",
                                "/api/v1/customer-answers/campaign-message/**",
                                "/api/v1/customer-answers/campaign/**",
                                "/api/v1/customer-answers/add",
                                "/api/v1/customer-answers/update/**",
                                "/api/v1/customer-answers/deleted/**",
                                // QR Redemptions
                                "/api/v1/qr-redemptions/**"
                        ).hasAuthority("STORE_OWNER")

                        // ===== CUSTOMER only =====
                        .requestMatchers(
                                "/api/v1/customer/my",
                                "/api/v1/customer/update",
                                "/api/v1/customer/delete",
                                "/api/v1/customer/my/**",
                                "/api/v1/customer-answers/answer/**",
                                "/api/v1/qr-redemptions/redeem-by-code",
                                "/api/v1/qr-redemptions/redeem-by-qr/**"
                        ).hasAuthority("CUSTOMER")

                        .anyRequest().authenticated()
                )

                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true))

                .httpBasic(httpBasic -> {});

        return http.build();
    }
}