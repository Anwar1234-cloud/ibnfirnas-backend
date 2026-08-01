-- Drops tables left behind by deleted entities that ddl-auto:update never
-- cleaned up. All four confirmed empty (0 rows) before this migration was
-- written:
--   banners                -> Banner entity, deleted 2026-07-23/07-31
--   newsletter_subscribers -> Newsletter module, deleted 2026-07-18
--   otp_verifications      -> OtpVerification (email-OTP), deleted 2026-07-18
--   tokens                 -> PasswordResetToken, deleted 2026-07-18
DROP TABLE IF EXISTS banners;
DROP TABLE IF EXISTS newsletter_subscribers;
DROP TABLE IF EXISTS otp_verifications;
DROP TABLE IF EXISTS tokens;
