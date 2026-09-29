package org.example.motionville.config;

import org.example.motionville.account.AccountService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain applicationSecurity(HttpSecurity http, AccountService accountService) throws Exception {
        return http
                .userDetailsService(accountService)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/videos/upload", "/channels/create", "/subscriptions").authenticated()
                        .requestMatchers(
                                "/videos/*/comments",
                                "/videos/*/like",
                                "/videos/*/unlike",
                                "/videos/*/delete",
                                "/videos/*/edit",
                                "/comments/*/delete",
                                "/channels/*/edit",
                                "/channels/*/subscribe",
                                "/channels/*/unsubscribe")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET, "/", "/login", "/register", "/videos/*", "/channels/*",
                                "/media/*", "/css/**", "/js/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/register").permitAll()
                        .anyRequest().denyAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", false)
                        .permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
