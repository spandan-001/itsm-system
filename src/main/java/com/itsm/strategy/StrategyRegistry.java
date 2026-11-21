package com.itsm.strategy;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds every implementation of one strategy type and which one is active.
 * Generic, so priority and assignment strategies share the same mechanism.
 */
public class StrategyRegistry<T extends NamedStrategy> {

    private final String kind;
    private final Map<String, T> strategies = new LinkedHashMap<>();
    private volatile T active;

    public StrategyRegistry(String kind, Collection<? extends T> implementations, String defaultName) {
        this.kind = kind;
        for (T strategy : implementations) {
            if (strategies.putIfAbsent(strategy.name(), strategy) != null) {
                throw new IllegalStateException("Duplicate " + kind + " strategy name: " + strategy.name());
            }
        }
        if (strategies.isEmpty()) {
            throw new IllegalStateException("No " + kind + " strategies registered");
        }
        this.active = get(defaultName);
    }

    public T active() {
        return active;
    }

    /** Switches the active strategy. */
    public T use(String name) {
        this.active = get(name);
        return active;
    }

    public T get(String name) {
        T strategy = strategies.get(name);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown " + kind + " strategy '" + name
                    + "'. Available: " + strategies.keySet());
        }
        return strategy;
    }

    public List<T> available() {
        return List.copyOf(strategies.values());
    }
}
