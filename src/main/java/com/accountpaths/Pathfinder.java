package com.accountpaths;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

public class Pathfinder implements Runnable {
    @Getter
    private final WorldPoint start;
    @Getter
    private final WorldPoint target;
    
    private final PathfinderConfig config;


    private final Deque<Node> boundary = new LinkedList<>();
    private final Set<WorldPoint> visited = new HashSet<>();
    private final List<Node> pending = new ArrayList<Node>() {
        @Override
        public boolean add(Node n) {
            boolean result = super.add(n);
            sort(null);
            return result;
        }
    };

    @Getter
    private List<WorldPoint> path = new ArrayList<>();
    @Getter
    private boolean done = false;

    public Pathfinder(WorldPoint start, WorldPoint target, PathfinderConfig config) {
        this.start = start;
        this.target = target;
        this.config = config;

        new Thread(this).start();
    }

    private void addNeighbor(Node node, WorldPoint neighbor, int wait) {
        if (visited.add(neighbor)) {
            Node n = new Node(neighbor, node, target, wait);
            if (n.isTransport()) {
                pending.add(n);
            } else {
                boundary.addLast(n);
            }
        }
    }

    private void addNeighbors(Node node) {

        for (WorldPoint neighbor : config.getMap().getNeighbors(node.position)) {
            addNeighbor(node, neighbor, 0);
        }
    }

    private boolean isHeuristicBetter(long candidate, Deque<Node> data) {
        for (Node n : data) {
            if (n.heuristic <= candidate) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void run() {
        boundary.addFirst(new Node(start, null, target));

        Node nearest = boundary.getFirst();
        long bestDistance = Integer.MAX_VALUE;
        Instant cutoffTime = Instant.now().plus(Duration.ofSeconds(2));

        while (!boundary.isEmpty()) {
            Node node = boundary.removeFirst();

            if (pending.size() > 0) {
                Node p = pending.get(0);
                if (isHeuristicBetter(p.heuristic, boundary)) {
                    boundary.addFirst(p);
                    pending.remove(0);
                }
            }

            if (node.position.equals(target)) {
                path = node.getPath();
                break;
            }

            long distance = node.heuristic;
            if (distance < bestDistance) {
                path = node.getPath();
                nearest = node;
                bestDistance = distance;
                cutoffTime = Instant.now().plus(Duration.ofSeconds(2));
            }

            if (Instant.now().isAfter(cutoffTime)) {
                path = nearest.getPath();
                break;
            }

            addNeighbors(node);
        }

        done = true;
        boundary.clear();
        visited.clear();
        pending.clear();
    }
}
