package com.connto.backend.api;

import com.connto.backend.service.AccountService;
import com.connto.backend.service.ProfileService;
import com.connto.backend.service.ProfileService.ProfileResponse;
import com.connto.backend.web.CurrentUser;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class ProfileController {

    private final ProfileService profileService;
    private final AccountService accountService;

    public ProfileController(ProfileService profileService, AccountService accountService) {
        this.profileService = profileService;
        this.accountService = accountService;
    }

    @GetMapping("/me/profile")
    public ProfileResponse mine() {
        return profileService.getMine(CurrentUser.id());
    }

    @PatchMapping("/me/profile")
    public ProfileResponse update(@RequestBody UpdateProfileRequest body) {
        return profileService.updateMine(
                CurrentUser.id(),
                body.displayName(),
                body.bio(),
                body.tags(),
                body.communicationTone(),
                body.behavioursSummary(),
                body.expectingFor());
    }

    @PostMapping(value = "/me/profile/voice-intro", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileResponse uploadVoiceIntro(@RequestParam("file") MultipartFile file) throws IOException {
        return profileService.saveVoiceIntro(CurrentUser.id(), file);
    }

    @DeleteMapping("/me/profile/voice-intro")
    public ProfileResponse deleteVoiceIntro() {
        return profileService.deleteVoiceIntro(CurrentUser.id());
    }

    @DeleteMapping("/me/account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount() {
        accountService.deleteMine(CurrentUser.id());
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
            @Size(max = 512) String tags,
            @Size(max = 240) String communicationTone,
            @Size(max = 2000) String behavioursSummary,
            @Size(max = 2000) String expectingFor) {}
}
