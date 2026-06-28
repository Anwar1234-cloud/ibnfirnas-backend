package com.ibnfirnas.service;

import com.ibnfirnas.entity.Newsletter;
import com.ibnfirnas.exception.BadRequestException;
import com.ibnfirnas.repository.NewsletterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NewsletterService {

    private final NewsletterRepository newsletterRepository;

    public Newsletter subscribe(String email) {
        if (newsletterRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already subscribed");
        }
        return newsletterRepository.save(
                Newsletter.builder().email(email).build());
    }

    public void unsubscribe(String email) {
        Newsletter subscriber = newsletterRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Email not found"));
        subscriber.setIsActive(false);
        newsletterRepository.save(subscriber);
    }
}