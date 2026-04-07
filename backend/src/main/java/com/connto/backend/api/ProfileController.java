package com.connto.backend.api;

import com.connto.backend.service.ProfileService;
import com.connto.backend.service.ProfileService.ProfileResponse;
import com.connto.backend.web.CurrentUser;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me/profile")
    public ProfileResponse mine() {
        return profileService.getMine(CurrentUser.id());
    }

    @PatchMapping("/me/profile")
    public ProfileResponse update(@RequestBody UpdateProfileRequest body) {
        return profileService.updateMine(
                CurrentUser.id(), body.displayName(), body.bio(), body.tags());
    }

    @GetMapping("/users/{userId}/profile")
    public ProfileResponse getPublic(@PathVariable UUID userId) {
        return profileService.getPublic(CurrentUser.id(), userId);
    }

    @GetMapping("/users/search")
    public List<ProfileResponse> search(@RequestParam String q) {
        return profileService.search(CurrentUser.id(), q);
    }

    public record UpdateProfileRequest(
            @Size(max = 120) String displayName,
            @Size(max = 2000) String bio,
            @Size(max = 512) String tags) {}
}
