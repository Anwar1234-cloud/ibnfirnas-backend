package com.ibnfirnas.repository;

import com.ibnfirnas.entity.PasswordResetToken;
import com.ibnfirnas.entity.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenAndTokenType(String token, TokenType tokenType);
    void deleteByUserIdAndTokenType(Long userId, TokenType tokenType);
}