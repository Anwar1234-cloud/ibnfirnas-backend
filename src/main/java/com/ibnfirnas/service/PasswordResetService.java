package com.ibnfirnas.service;

import com.ibnfirnas.entity.PasswordResetToken;
import com.ibnfirnas.entity.User;
import com.ibnfirnas.entity.enums.TokenType;
import com.ibnfirnas.exception.BadRequestException;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.PasswordResetTokenRepository;
import com.ibnfirnas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Delete old tokens
        tokenRepository.deleteByUserIdAndTokenType(user.getId(),
                TokenType.PASSWORD_RESET);

        String token = UUID.randomUUID().toString();

        tokenRepository.save(PasswordResetToken.builder()
                .token(token)
                .user(user)
                .tokenType(TokenType.PASSWORD_RESET)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build());

        emailService.sendPasswordResetEmail(user.getEmail(), token);
        log.info("Password reset token sent to: {}", email);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository
                .findByTokenAndTokenType(token, TokenType.PASSWORD_RESET)
                .orElseThrow(() -> new BadRequestException("Invalid or expired token"));

        if (resetToken.getIsUsed()) {
            throw new BadRequestException("Token already used");
        }
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Token has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setIsUsed(true);
        tokenRepository.save(resetToken);
    }
}