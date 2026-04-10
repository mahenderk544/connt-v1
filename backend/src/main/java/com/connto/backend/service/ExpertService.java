package com.connto.backend.service;

import com.connto.backend.domain.ConnectionRequestStatus;
import com.connto.backend.domain.ExpertProfileCategory;
import com.connto.backend.domain.ExpertProfileLanguage;
import com.connto.backend.domain.Profile;
import com.connto.backend.repository.ConnectionRequestRepository;
import com.connto.backend.repository.FriendshipRepository;
import com.connto.backend.repository.ProfileRepository;
import com.connto.backend.web.ApiException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpertService {

    private final ProfileRepository profiles;
    private final ConnectionRequestRepository requests;
    private final FriendshipRepository friendships;

    public ExpertService(
            ProfileRepository profiles,
            ConnectionRequestRepository requests,
            FriendshipRepository friendships) {
        this.profiles = profiles;
        this.requests = requests;
        this.friendships = friendships;
    }

    /**
     * Listed experts for the discover feed. Mock {@code online} and {@code averageRating} come from
     * profile columns until real presence and reviews are implemented.
     */
    @Transactional(readOnly = true)
    public List<ExpertCardResponse> list(UUID viewerId, String categoryFilter) {
        String cat = normalizeCategory(categoryFilter);
        return profiles.findListedExperts(cat).stream()
                .filter(p -> !p.getUserId().equals(viewerId))
                .map(p -> toCard(p, viewerId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExpertCardResponse get(UUID viewerId, UUID expertUserId) {
        Profile p =
                profiles.findWithExpertCollectionsByUserId(expertUserId)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                HttpStatus.NOT_FOUND, "User not found"));
        if (!p.isExpertListed()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Expert not found");
        }
        return toCard(p, viewerId);
    }

    private static String normalizeCategory(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim().toLowerCase();
        if (t.isEmpty() || "all".equals(t)) {
            return null;
        }
        return t;
    }

    private ExpertCardResponse toCard(Profile p, UUID viewerId) {
        boolean alreadyFriends = friendships.areFriends(viewerId, p.getUserId());
        boolean friendRequestPending =
                requests.findByFromUserIdAndToUserId(viewerId, p.getUserId())
                        .filter(r -> r.getStatus() == ConnectionRequestStatus.PENDING)
                        .isPresent();

        Double rating =
                p.getAverageRating() == null ? null : p.getAverageRating().doubleValue();
        Integer age = p.getAgeYears() == null ? null : p.getAgeYears().intValue();

        List<String> languages =
                p.getExpertLanguages() == null
                        ? List.of()
                        : p.getExpertLanguages().stream()
                                .map(ExpertProfileLanguage::getLabel)
                                .toList();
        List<String> categories =
                p.getExpertCategories() == null
                        ? List.of()
                        : p.getExpertCategories().stream()
                                .map(ExpertProfileCategory::getCategory)
                                .toList();

        return new ExpertCardResponse(
                p.getUserId(),
                p.getDisplayName(),
                p.getBio(),
                p.getPhotoUrl(),
                p.getExpertTitle(),
                languages,
                p.getRatePerMinPaise(),
                rating,
                p.getRatingCount(),
                age,
                p.isExpertOnlineHint(),
                p.isExpertStarFeatured(),
                categories,
                alreadyFriends,
                friendRequestPending);
    }

    public record ExpertCardResponse(
            UUID userId,
            String displayName,
            String bio,
            String photoUrl,
            String expertTitle,
            List<String> languages,
            Integer ratePerMinPaise,
            Double averageRating,
            int ratingCount,
            Integer ageYears,
            boolean online,
            boolean starFeatured,
            List<String> categories,
            boolean alreadyFriends,
            boolean friendRequestPending) {}
}
