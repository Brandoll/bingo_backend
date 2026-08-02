package com.bsplay.physicalcard.presentation;

import com.bsplay.game.application.dto.PhysicalCardResponse;
import com.bsplay.game.application.service.GameApplicationService;
import jakarta.validation.constraints.Size;
import com.bsplay.physicalcard.application.PhysicalCardCatalogQueryService;
import com.bsplay.physicalcard.application.PhysicalCardImportReport;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.bsplay.shared.security.GuestPrincipal;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/physical-cards")
@Validated
public class PhysicalCardController {
    private final GameApplicationService games;
    private final PhysicalCardCatalogQueryService catalog;

    public PhysicalCardController(GameApplicationService games, PhysicalCardCatalogQueryService catalog) {
        this.games = games;
        this.catalog = catalog;
    }

    @GetMapping("/{externalId}")
    public PhysicalCardResponse find(@PathVariable @Size(max = 32) String externalId) {
        return games.findPhysicalCard(externalId);
    }

    @GetMapping("/imports/latest")
    public PhysicalCardImportReport latestImportReport() {
        return catalog.latestReport();
    }

    @PostMapping
    public PhysicalCardResponse register(@Valid @RequestBody RegisterPhysicalCardRequest request,
                                         @AuthenticationPrincipal GuestPrincipal principal) {
        return catalog.register(request.externalId(), request.numbers(), principal);
    }
}
