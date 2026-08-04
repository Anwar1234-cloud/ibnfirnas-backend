package com.ibnfirnas.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class FirebaseConfig {

    @Value("${FIREBASE_SERVICE_ACCOUNT:}")
    private String firebaseServiceAccount;

    @PostConstruct
    public void initialize() {
        try {

            if (firebaseServiceAccount == null || firebaseServiceAccount.isBlank()) {
                log.warn("Firebase service account not configured");
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(
                            GoogleCredentials.fromStream(
                                    new ByteArrayInputStream(
                                            firebaseServiceAccount.getBytes(StandardCharsets.UTF_8)
                                    )
                            )
                    )
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully");
            }

        } catch (Exception e) {
            log.error("Firebase initialization failed", e);
        }
    }
}