package com.ibnfirnas.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.ibnfirnas.dto.response.AuthResponse;
import com.ibnfirnas.entity.User;
import com.ibnfirnas.entity.enums.UserRole;
import com.ibnfirnas.exception.BadRequestException;
import com.ibnfirnas.repository.UserRepository;
import com.ibnfirnas.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${google.client-id}")
    private String googleClientId;

    public AuthResponse authenticate(String idTokenString) {

        GoogleIdToken.Payload payload = verifyGoogleToken(idTokenString);

        String email = payload.getEmail();
        Boolean emailVerified = (Boolean) payload.getEmailVerified();

        if (emailVerified == null || !emailVerified) {
            throw new BadRequestException("Google email is not verified");
        }
        String fullName = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {

                    User newUser = User.builder()
                            .email(email)
                            .fullName(fullName)
                            .avatarUrl(picture)
                            .password(
                                    passwordEncoder.encode(UUID.randomUUID().toString())
                            )
                            .phone(null)
                            .role(UserRole.ROLE_USER)
                            .isActive(true)
                            .build();

                    return userRepository.save(newUser);
                });

        boolean updated = false;

        if (picture != null && !picture.equals(user.getAvatarUrl())) {
            user.setAvatarUrl(picture);
            updated = true;
        }

        if (fullName != null && !fullName.equals(user.getFullName())) {
            user.setFullName(fullName);
            updated = true;
        }

        if (updated) {
            userRepository.save(user);
        }

        String jwt = jwtTokenProvider.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(jwt)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idTokenString) {

        try {

            GoogleIdTokenVerifier verifier =
                    new GoogleIdTokenVerifier.Builder(
                            new NetHttpTransport(),
                            GsonFactory.getDefaultInstance())
                            .setAudience(Collections.singletonList(googleClientId))
                            .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                throw new BadRequestException("Invalid Google token");
            }

            return idToken.getPayload();

        } catch (GeneralSecurityException | IOException e) {
            throw new BadRequestException("Google authentication failed");
        }
    }
}
