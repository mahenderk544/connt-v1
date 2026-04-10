package com.connto.backend.api;

import com.connto.backend.service.ProfileService;
import java.nio.file.Files;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private final ProfileService profileService;

    public MediaController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/voice/{userId}")
    public ResponseEntity<Resource> voiceIntro(@PathVariable UUID userId) throws Exception {
        var path = profileService.voiceFilePath(userId);
        if (!Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }
        Resource body = new FileSystemResource(path);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .contentType(MediaType.parseMediaType("audio/webm"))
                .body(body);
    }
}
