package com.accountpaths;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;


public class PathfinderConfig {

    @Getter
    private final CollisionMap map;
    @Getter
    private final Map<WorldPoint, List<Transport>> transports;
    private final Client client;

    public PathfinderConfig(CollisionMap map, Map<WorldPoint, List<Transport>> transports, Client client) {
        this.map = map;
        this.transports = transports;
        this.client = client;
        refresh();
    }

    public void refresh() {
        if (!GameState.LOGGED_IN.equals(client.getGameState())) {
            return;
        }
    }
}
