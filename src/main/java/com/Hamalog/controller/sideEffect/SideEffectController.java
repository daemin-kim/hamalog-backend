package com.Hamalog.controller.sideEffect;

import com.Hamalog.service.sideEffect.SideEffectService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/side-effect")
public class SideEffectController {

    private final SideEffectService sideEffectService;

    public SideEffectController(SideEffectService sideEffectService) {
        this.sideEffectService = sideEffectService;
    }


}
