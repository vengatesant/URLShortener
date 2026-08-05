package com.urlshortener.api.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Only relevant when the frontend build is embedded as static resources (see the root
 * Dockerfile) — the SPA's own routes must resolve to index.html on a hard refresh/direct visit,
 * since there's no server-side route for e.g. {@code /links}. Deliberately lists the SPA's known
 * routes rather than a catch-all {@code "/**"}, so it can never shadow {@code /api/**},
 * {@code /r/**}, or a genuinely missing static asset.
 */
@Controller
public class SpaForwardController {

    @GetMapping({"/", "/links", "/links/{shortCode}/stats"})
    public String forwardToApp() {
        return "forward:/index.html";
    }
}
