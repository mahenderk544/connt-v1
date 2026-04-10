package com.connto.backend.service;

import com.connto.backend.config.MediaProperties;
import com.connto.backend.domain.Profile;
import com.connto.backend.repository.ProfileRepository;
import com.connto.backend.web.ApiException;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileService {

    private static final int MAX_VOICE_BYTES = 3 * 1024 * 1024;
    private static final String VOICE_SUFFIX = ".webm";

    private final ProfileRepository profiles;
    private final MediaProperties mediaProperties;

    public ProfileService(ProfileRepository profiles, MediaProperties mediaProperties) {
        this.profiles = profiles;
        this.mediaProperties = mediaProperties;
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
    public ProfileResponse updateMine(
            UUID userId,
            String displayName,
            String bio,
            String tags,
            String communicationTone,
            String behavioursSummary,
            String expectingFor) {
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
        if (communicationTone != null) {
            p.setCommunicationTone(communicationTone);
        }
        if (behavioursSummary != null) {
            p.setBehavioursSummary(behavioursSummary);
        }
        if (expectingFor != null) {
            p.setExpectingFor(expectingFor);
        }
        p.setUpdatedAt(java.time.Instant.now());
        profiles.save(p);
        return ProfileResponse.from(p);
    }

    @Transactional
    public ProfileResponse saveVoiceIntro(UUID userId, MultipartFile file) throws IOException {
        byte[] data = file.getBytes();
        if (data.length == 0 || data.length > MAX_VOICE_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Audio must be non-empty and under 3 MB");
        }
        String ct = file.getContentType();
        if (ct == null || !ct.toLowerCase().startsWith("audio/")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Upload must be an audio file");
        }
        Path dir = mediaProperties.voiceUploadDirPath();
        Files.createDirectories(dir);
        Path out = dir.resolve(userId + VOICE_SUFFIX);
        Files.write(out, data);

        Profile p =
                profiles.findByUserId(userId)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                HttpStatus.NOT_FOUND, "Profile not found"));
        String url = "/api/v1/media/voice/" + userId;
        p.setVoiceIntroUrl(url);
        p.setUpdatedAt(java.time.Instant.now());
        profiles.save(p);
        return ProfileResponse.from(p);
    }

    @Transactional
    public ProfileResponse deleteVoiceIntro(UUID userId) {
        Profile p =
                profiles.findByUserId(userId)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                HttpStatus.NOT_FOUND, "Profile not found"));
        removeVoiceFileIfExists(userId);
        p.setVoiceIntroUrl(null);
        p.setUpdatedAt(java.time.Instant.now());
        profiles.save(p);
        return ProfileResponse.from(p);
    }

    /** Removes stored voice file from disk if present (e.g. before account deletion). */
    public void removeVoiceFileIfExists(UUID userId) {
        try {
            Files.deleteIfExists(voiceFilePath(userId));
        } catch (IOException ignored) {
        }
    }

    public Path voiceFilePath(UUID userId) {
        return mediaProperties.voiceUploadDirPath().resolve(userId + VOICE_SUFFIX);
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
            String tags,
            String voiceIntroUrl,
            String communicationTone,
            String behavioursSummary,
            String expectingFor) {
        static ProfileResponse from(Profile p) {
            return new ProfileResponse(
                    p.getUserId(),
                    p.getDisplayName(),
                    p.getBio(),
                    p.getPhotoUrl(),
                    p.getTags(),
                    p.getVoiceIntroUrl(),
                    p.getCommunicationTone(),
                    p.getBehavioursSummary(),
                    p.getExpectingFor());
        }
    }
}
