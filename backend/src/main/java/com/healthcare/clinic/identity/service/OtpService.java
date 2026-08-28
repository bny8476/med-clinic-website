package com.healthcare.clinic.identity.service;

import com.healthcare.clinic.identity.entity.OtpCode;
import com.healthcare.clinic.identity.entity.User;
import com.healthcare.clinic.identity.repository.OtpCodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.ZonedDateTime;

@Slf4j
@Service
public class OtpService {

    private static final int OTP_EXPIRY_MINUTES = 10;
    /** Max OTPs a user may request within the expiry window before we reject. */
    private static final int OTP_RATE_LIMIT = 5;

    // SecureRandom is thread-safe and cryptographically strong (unlike java.util.Random).
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OtpCodeRepository otpCodeRepository;
    private final JavaMailSender mailSender;

    public OtpService(OtpCodeRepository otpCodeRepository,
                      @org.springframework.beans.factory.annotation.Autowired(required = false) JavaMailSender mailSender) {
        this.otpCodeRepository = otpCodeRepository;
        this.mailSender = mailSender;
    }

    @Transactional
    public void generateAndSendOtp(User user) {
        // Rate-limit: count un-expired OTPs already issued for this user.
        long recentCount = otpCodeRepository.countByUserAndExpiryDateAfterAndUsedFalse(
                user, ZonedDateTime.now().minusMinutes(OTP_EXPIRY_MINUTES));
        if (recentCount >= OTP_RATE_LIMIT) {
            // Do not throw — callers always return a neutral "check your inbox" message.
            log.warn("OTP rate limit hit for user id={}", user.getId());
            return;
        }

        // SecureRandom-backed 6-digit OTP (range 0–999999, zero-padded).
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));

        OtpCode otpCode = new OtpCode();
        otpCode.setUser(user);
        otpCode.setCode(code);
        otpCode.setExpiryDate(ZonedDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        otpCodeRepository.save(otpCode);

        if (mailSender == null) {
            log.warn("JavaMailSender is not configured. Skipping OTP mail delivery for user id={}", user.getId());
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Your Verification Code");
        message.setText("Your OTP is: " + code + "\nIt expires in " + OTP_EXPIRY_MINUTES + " minutes.\n\nDo not share this code with anyone.");
        try {
            mailSender.send(message);
        } catch (Exception e) {
            // Mail delivery failure: log at WARN so the app keeps running in dev without SMTP.
            // The OTP code itself is intentionally NOT logged — it is a secret.
            log.warn("OTP mail delivery failed for user id={}: {}", user.getId(), e.getMessage());
        }
    }

    @Transactional
    public boolean verifyOtp(String code, User user) {
        OtpCode otpCode = otpCodeRepository.findByCode(code).orElse(null);
        if (otpCode == null
                || otpCode.isUsed()
                || otpCode.getExpiryDate().isBefore(ZonedDateTime.now())) {
            return false;
        }
        // Ensure the OTP belongs to this user (prevents code-guessing cross-user attacks).
        if (!otpCode.getUser().getId().equals(user.getId())) {
            return false;
        }

        otpCode.setUsed(true);
        otpCodeRepository.save(otpCode);
        return true;
    }
}
