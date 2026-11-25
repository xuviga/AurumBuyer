package com.xuvigan.aurumbuyer.data;

import java.util.Collection;
import java.util.UUID;

/** Player profile storage contract (SPEC.md section 4). */
public interface ProfileManager {

    PlayerProfile get(UUID id);

    PlayerProfile cached(UUID id);

    PlayerProfile getByName(String name);

    void save(PlayerProfile profile);

    /** Writes the profile immediately (used on quit and shutdown). */
    void saveBlocking(PlayerProfile profile);

    void saveAllBlocking();

    Collection<PlayerProfile> all();

    void unload(UUID id);
}