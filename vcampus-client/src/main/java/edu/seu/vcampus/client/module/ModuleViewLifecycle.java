package edu.seu.vcampus.client.module;

/** Optional lifecycle callbacks for a cached client-module view. */
public interface ModuleViewLifecycle {

    /** Called before the user leaves the module and returns to the campus-service home. */
    default void onModuleExit() {
    }
}
