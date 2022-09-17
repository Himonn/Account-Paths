package com.accountpaths;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ObjectComposition;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
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
class AccountPathsOverlay extends Overlay {

    private final Client client;
    private final AccountPathsConfig config;
    private final AccountPathsPlugin plugin;
    private final TextComponent textComponent = new TextComponent();
    private final PanelComponent panelComponent = new PanelComponent();


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
        if (plugin == null || plugin.title == null || plugin.title == "" || plugin.description == null || plugin.description == "")
        {
            return null;
        }

        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(TitleComponent.builder().text(plugin.title).build());
//        panelComponent.getChildren().add(LineComponent.builder().left("Description:").build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left(plugin.description)
                .leftColor(TITLED_CONTENT_COLOR)
                .build());

        return panelComponent.render(graphics);
    }
}
