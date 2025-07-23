package com.Hamalog.controller.sideEffect;

import com.Hamalog.dto.sideEffect.response.RecentSideEffectResponse;
import com.Hamalog.service.sideEffect.SideEffectService;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/side-effect")
public class SideEffectController {

    private final SideEffectService sideEffectService;

    public SideEffectController(SideEffectService sideEffectService) {
        this.sideEffectService = sideEffectService;
    }

    @GetMapping("/recent")
    public ResponseEntity<RecentSideEffectResponse> getRecentSideEffects(@RequestParam Long userId) {
        return ResponseEntity.ok(sideEffectService.getRecentSideEffects(userId));
    }

}
