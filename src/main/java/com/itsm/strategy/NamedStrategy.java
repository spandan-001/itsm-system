package com.itsm.strategy;

/** A strategy that can be picked by name at runtime. */
public interface NamedStrategy {

    /** Short identifier, e.g. "weighted". */
    String name();

    String description();
}
