package com.bsplay.game.application.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AutomaticDrawScheduler {
    private final GameApplicationService games;

    public AutomaticDrawScheduler(GameApplicationService games) {
        this.games = games;
    }

    @Scheduled(fixedDelay = 1000)
    public void drawDueNumbers() {
        for (var gameId : games.automaticGamesDue()) {
            games.drawAutomatic(gameId);
        }
    }
}
