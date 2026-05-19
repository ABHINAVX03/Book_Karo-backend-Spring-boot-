package com.codingshuttle.project.uber.uberApp.configs;

import com.codingshuttle.project.uber.uberApp.security.AuthRateLimitFilter;
import com.codingshuttle.project.uber.uberApp.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(securedEnabled = true)
public class WebSecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthRateLimitFilter authRateLimitFilter;
    private final AppSecurityProperties appSecurityProperties;
    /** Public auth endpoints only; driver onboarding requires a signed-in user + JWT. */
    private static final String[] PUBLIC_AUTH_ROUTES = {
            "/auth/signup",
            "/auth/login",
            "/auth/refresh",
            "/auth/send-otp",
            "/auth/verify-otp",
            "/auth/logout"
    };
    private static final String[] PUBLIC_PLATFORM_ROUTES = {
            "/actuator/health/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {

        httpSecurity
                .cors(Customizer.withDefaults())
                .sessionManagement(sessionConfig -> sessionConfig
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrfConfig -> csrfConfig.disable())
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(31536000)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_AUTH_ROUTES).permitAll()
                        .requestMatchers(PUBLIC_PLATFORM_ROUTES).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(authRateLimitFilter, JwtAuthFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(appSecurityProperties.getAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin", "X-Captcha-Token"));
        config.setExposedHeaders(List.of("Set-Cookie"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // 1 hour cache for preflight
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
