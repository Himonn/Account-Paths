package com.accountpaths;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.config.*;

import java.awt.event.KeyEvent;

@ConfigGroup("accountpaths")
public interface AccountPathsConfig extends Config {

    @ConfigItem(
            name = "Path",
            description = "Your path",
            position = 0,
            keyName = "path"
    )
    default Path path() { return Path.HARDCORE; }

    @ConfigItem(
            keyName = "index",
            name = "Index",
            description = "Stage number",
            position = 1
    )
    default int index() { return 0; }

    @ConfigItem(
            keyName = "next",
            name = "Next Step Hotkey",
            description = "Hotkey to move to next step with",
            position = 5
    )
    default Keybind next() { return new Keybind(KeyEvent.VK_BACK_SLASH, 0); }

    @ConfigItem(
            keyName = "previous",
            name = "Previous Step Hotkey",
            description = "Hotkey to move to previous step with",
            position = 10
    )
    default Keybind previous() { return new Keybind(KeyEvent.VK_BACK_SLASH, 0); }

    @AllArgsConstructor
    public enum Path {
        HARDCORE("hardcore"),
        IRONMAN("ironman"),
        MAIN("main"),
        DS2("ds2");

        @Getter
        private String path;

    }

}