package com.ibnfirnas.repository;

import com.ibnfirnas.entity.OtpVerification;
import com.ibnfirnas.entity.enums.OtpPurpose;
import com.ibnfirnas.entity.enums.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

public interface OtpVerificationRepository
        extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findByIdentifierAndOtpTypeAndPurposeAndIsUsedFalse(
            String identifier, OtpType otpType, OtpPurpose purpose);

    @Transactional
    void deleteByIdentifierAndPurpose(String identifier, OtpPurpose purpose);
}