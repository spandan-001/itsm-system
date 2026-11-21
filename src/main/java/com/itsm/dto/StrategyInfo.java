package com.itsm.dto;

import java.util.List;

import com.itsm.strategy.NamedStrategy;
import com.itsm.strategy.StrategyRegistry;

public record StrategyInfo(String active, List<Option> available) {

    public record Option(String name, String description) {
    }

    public static StrategyInfo of(StrategyRegistry<? extends NamedStrategy> registry) {
        return new StrategyInfo(registry.active().name(), registry.available().stream()
                .map(s -> new Option(s.name(), s.description()))
                .toList());
    }
}
