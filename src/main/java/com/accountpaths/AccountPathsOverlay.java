package com.accountpaths;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ObjectComposition;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;

import static net.runelite.client.plugins.cluescrolls.ClueScrollOverlay.TITLED_CONTENT_COLOR;

@Slf4j
@Singleton
class AccountPathsOverlay extends OverlayPanel {

    private final Client client;
    private final AccountPathsConfig config;
    private final AccountPathsPlugin plugin;

    @Inject
    private AccountPathsOverlay(final Client client, final AccountPathsConfig config, final AccountPathsPlugin plugin) {
        super(plugin);
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        this.client = client;
        this.config = config;
        this.plugin = plugin;
        this.setPosition(OverlayPosition.BOTTOM_LEFT);
        this.setPriority(OverlayPriority.HIGHEST);
    }


    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (plugin == null || plugin.title == null || plugin.title.equals("") || plugin.description == null || plugin.description.equals(""))
        {
            return null;
        }

        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(TitleComponent.builder().text("Path").build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Task:")
                .right(plugin.title)
                .rightColor(TITLED_CONTENT_COLOR)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left(plugin.description)
                .leftColor(TITLED_CONTENT_COLOR)
                .build());

        if (config.nextStep() && plugin.nextTitle != null && !plugin.nextTitle.equals(""))
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Next:")
                    .right(plugin.nextTitle)
                    .rightColor(TITLED_CONTENT_COLOR)
                    .build());
        }

        if (config.nextStep() && plugin.nextDescription != null && !plugin.nextDescription.equals(""))
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left(plugin.nextDescription)
                    .leftColor(TITLED_CONTENT_COLOR)
                    .build());
        }

        return panelComponent.render(graphics);
    }
}
