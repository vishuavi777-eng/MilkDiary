package com.rudrainfotech.milkdiary.ui;

/** Screens can opt-in to global shortcuts by implementing these. */
public interface StandardActions {
    default void actionSave() {}
    default void actionRefresh() {}
    default void actionRecompute() {}
}
