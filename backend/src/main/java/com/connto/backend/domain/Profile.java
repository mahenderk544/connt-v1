package com.connto.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(columnDefinition = "text")
    private String bio;

    @Column(name = "photo_url", length = 1024)
    private String photoUrl;

    @Column(length = 512)
    private String tags;

    @Column(name = "voice_intro_url", length = 1024)
    private String voiceIntroUrl;

    @Column(name = "communication_tone", length = 240)
    private String communicationTone;

    @Column(name = "behaviours_summary", columnDefinition = "text")
    private String behavioursSummary;

    @Column(name = "expecting_for", columnDefinition = "text")
    private String expectingFor;

    @Column(name = "expert_listed", nullable = false)
    private boolean expertListed = false;

    @Column(name = "expert_title", length = 160)
    private String expertTitle;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, label ASC")
    private List<ExpertProfileLanguage> expertLanguages = new ArrayList<>();

    @Column(name = "rate_per_min_paise")
    private Integer ratePerMinPaise;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount = 0;

    @Column(name = "age_years")
    private Short ageYears;

    @Column(name = "expert_star_featured", nullable = false)
    private boolean expertStarFeatured = false;

    @Column(name = "expert_online_hint", nullable = false)
    private boolean expertOnlineHint = false;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("category ASC")
    private List<ExpertProfileCategory> expertCategories = new ArrayList<>();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getUserId() {
        return userId;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getVoiceIntroUrl() {
        return voiceIntroUrl;
    }

    public void setVoiceIntroUrl(String voiceIntroUrl) {
        this.voiceIntroUrl = voiceIntroUrl;
    }

    public String getCommunicationTone() {
        return communicationTone;
    }

    public void setCommunicationTone(String communicationTone) {
        this.communicationTone = communicationTone;
    }

    public String getBehavioursSummary() {
        return behavioursSummary;
    }

    public void setBehavioursSummary(String behavioursSummary) {
        this.behavioursSummary = behavioursSummary;
    }

    public String getExpectingFor() {
        return expectingFor;
    }

    public void setExpectingFor(String expectingFor) {
        this.expectingFor = expectingFor;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isExpertListed() {
        return expertListed;
    }

    public void setExpertListed(boolean expertListed) {
        this.expertListed = expertListed;
    }

    public String getExpertTitle() {
        return expertTitle;
    }

    public void setExpertTitle(String expertTitle) {
        this.expertTitle = expertTitle;
    }

    public List<ExpertProfileLanguage> getExpertLanguages() {
        return expertLanguages;
    }

    public void setExpertLanguages(List<ExpertProfileLanguage> expertLanguages) {
        this.expertLanguages = expertLanguages;
    }

    public Integer getRatePerMinPaise() {
        return ratePerMinPaise;
    }

    public void setRatePerMinPaise(Integer ratePerMinPaise) {
        this.ratePerMinPaise = ratePerMinPaise;
    }

    public BigDecimal getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(BigDecimal averageRating) {
        this.averageRating = averageRating;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }

    public Short getAgeYears() {
        return ageYears;
    }

    public void setAgeYears(Short ageYears) {
        this.ageYears = ageYears;
    }

    public boolean isExpertStarFeatured() {
        return expertStarFeatured;
    }

    public void setExpertStarFeatured(boolean expertStarFeatured) {
        this.expertStarFeatured = expertStarFeatured;
    }

    public boolean isExpertOnlineHint() {
        return expertOnlineHint;
    }

    public void setExpertOnlineHint(boolean expertOnlineHint) {
        this.expertOnlineHint = expertOnlineHint;
    }

    public List<ExpertProfileCategory> getExpertCategories() {
        return expertCategories;
    }

    public void setExpertCategories(List<ExpertProfileCategory> expertCategories) {
        this.expertCategories = expertCategories;
    }
}
