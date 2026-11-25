package com.xuvigan.aurumbuyer.gui;

/**
 * Every AurumBuyer GUI type. Clicks are routed by {@link GuiHolder} identity +
 * this enum; inventory titles are never compared (SPEC.md constraint 4).
 */
public enum MenuType {

    MAIN,
    SHOP,
    HISTORY,
    SETTINGS,
    BOOSTERS,
    LIMITS,
    ADMIN
}
