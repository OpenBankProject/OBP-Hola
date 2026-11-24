package com.openbankproject.health;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The liveness probe OBP-API answers at GET /alive, repeated here so every app can be watched the same way.
 *
 * It answers true as soon as the process serves HTTP. It does not look at OBP-API, OBP-OIDC or Redis: those belong in /status, so a sick dependency never gets a healthy Hola restarted.
 */
@RestController
public class AliveController {

    @GetMapping(value = "/alive", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> alive() {
        // A probe must always see the live answer, never a cached one.
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body("true");
    }
}
