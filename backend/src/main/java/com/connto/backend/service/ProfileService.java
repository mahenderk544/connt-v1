package com.connto.backend.service;

import com.connto.backend.domain.Profile;
import com.connto.backend.repository.ProfileRepository;
import com.connto.backend.web.ApiException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final ProfileRepository profiles;

    public ProfileService(ProfileRepository profiles) {
        this.profiles = profiles;
    }

    public ProfileResponse getMine(UUID userId) {
        Profile p =
                profiles.findByUserId(userId)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                HttpStatus.NOT_FOUND, "Profile not found"));
        return ProfileResponse.from(p);
    }

    public ProfileResponse getPublic(UUID viewerId, UUID userId) {
        if (!viewerId.equals(userId)) {
            // could add privacy rules later
        }
        Profile p =
                profiles.findByUserId(userId)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                HttpStatus.NOT_FOUND, "Profile not found"));
        return ProfileResponse.from(p);
    }

    @Transactional
    public ProfileResponse updateMine(UUID userId, String displayName, String bio, String tags) {
        Profile p =
                profiles.findByUserId(userId)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                HttpStatus.NOT_FOUND, "Profile not found"));
        if (displayName != null && !displayName.isBlank()) {
            p.setDisplayName(displayName.trim());
        }
        if (bio != null) {
            p.setBio(bio);
        }
        if (tags != null) {
            p.setTags(tags);
        }
        p.setUpdatedAt(java.time.Instant.now());
        return ProfileResponse.from(p);
    }

    public List<ProfileResponse> search(UUID excludeUserId, String q) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        return profiles.search(excludeUserId, q.trim()).stream()
                .map(ProfileResponse::from)
                .collect(Collectors.toList());
    }

    public record ProfileResponse(
            UUID userId,
            String displayName,
            String bio,
            String photoUrl,
            String tags) {
        static ProfileResponse from(Profile p) {
            return new ProfileResponse(
                    p.getUserId(),
                    p.getDisplayName(),
                    p.getBio(),
                    p.getPhotoUrl(),
                    p.getTags());
        }
    }
}
