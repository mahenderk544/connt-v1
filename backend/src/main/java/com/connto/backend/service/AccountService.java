package com.connto.backend.service;

import com.connto.backend.repository.AppUserRepository;
import com.connto.backend.web.ApiException;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final AppUserRepository users;
    private final ProfileService profileService;

    public AccountService(AppUserRepository users, ProfileService profileService) {
        this.users = users;
        this.profileService = profileService;
    }

    /**
     * Permanently deletes the user row. Database {@code ON DELETE CASCADE} removes profile,
     * messages, friendships, and connection requests.
     */
    @Transactional
    public void deleteMine(UUID userId) {
        if (!users.existsById(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Account not found");
        }
        profileService.removeVoiceFileIfExists(userId);
        users.deleteById(userId);
    }
}
