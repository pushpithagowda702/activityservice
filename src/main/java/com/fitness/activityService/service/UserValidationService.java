package com.fitness.activityService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserValidationService {
    private static final Logger log = LoggerFactory.getLogger(UserValidationService.class);
    private final WebClient userServiceWebClient;

    public boolean validateUser(String userId) {
        try {
            log.info("Calling user service");
            return userServiceWebClient.get()
                    .uri("/api/users/validate-user/{userId}", userId)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();
        } catch (WebClientResponseException e) {
            if(e.getStatusCode() == HttpStatus.NOT_FOUND)
                throw new RuntimeException("User not found: " + userId);
            else
                throw new RuntimeException("Bad Request: " + userId);
        }

    }
}
