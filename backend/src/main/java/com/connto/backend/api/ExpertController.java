package com.connto.backend.api;

import com.connto.backend.service.ExpertService;
import com.connto.backend.service.ExpertService.ExpertCardResponse;
import com.connto.backend.web.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/experts")
public class ExpertController {

    private final ExpertService expertService;

    public ExpertController(ExpertService expertService) {
        this.expertService = expertService;
    }

    @GetMapping
    public List<ExpertCardResponse> list(
            @RequestParam(name = "category", required = false) String category) {
        return expertService.list(CurrentUser.id(), category);
    }

    @GetMapping("/{userId}")
    public ExpertCardResponse get(@PathVariable UUID userId) {
        return expertService.get(CurrentUser.id(), userId);
    }
}
