package com.accountpaths;

import com.google.common.base.Strings;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.client.util.ColorUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;

@Singleton
public class AccountPathsSceneOverlay extends Overlay {
    private final Client client;
    @Inject
    private AccountPathsPlugin plugin;
    private final AccountPathsConfig config;
    private final ModelOutlineRenderer outliner;

    @Inject
    private AccountPathsSceneOverlay(final Client client, final AccountPathsPlugin plugin, final AccountPathsConfig config, ModelOutlineRenderer outliner) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.outliner = outliner;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (plugin.currentTiles != null && plugin.currentTiles.size() != 0)
        {
            for (WorldPoint key : plugin.currentTiles.keySet())
            {
                renderWorldPoint(graphics, key, Color.WHITE);
                renderWorldPointText(graphics, key, plugin.currentTiles.get(key), Color.WHITE);
            }
        }

        return null;
    }

    private void renderWorldPoint(Graphics2D graphics, WorldPoint worldPoint, Color color) {
        LocalPoint lp = LocalPoint.fromWorld(client, worldPoint);
        if (lp != null) {
            renderTile(graphics, lp, color, 1);
        }
    }

    private void renderTile(final Graphics2D graphics, final LocalPoint dest, final Color color, final double borderWidth)
    {
        if (dest == null) {
            return;
        }

        final Polygon poly = Perspective.getCanvasTilePoly(client, dest);

        if (poly == null) {
            return;
        }

        renderPoly(graphics, color, poly);
    }

    private void renderWorldPointText(Graphics2D graphics, WorldPoint worldPoint, String text, Color color)
    {
        if (text == null || text.equals(""))
        {
            return;
        }

        LocalPoint lp = LocalPoint.fromWorld(client, worldPoint);
        if (lp != null)
        {
            net.runelite.api.Point point = Perspective.localToCanvas(client, lp, worldPoint.getPlane());
            if (point != null)
            {
                int textWidth = graphics.getFontMetrics().stringWidth(text);
                int textHeight = graphics.getFontMetrics().getAscent();

                Point centerPoint = new Point(point.getX() - textWidth / 2, point.getY() + textHeight / 2);

                renderTextLocation(graphics, centerPoint, text, color);
            }
        }
    }

    public static void renderTextLocation(Graphics2D graphics, Point txtLoc, String text, Color color)
    {
        if (Strings.isNullOrEmpty(text))
        {
            return;
        }

        int x = (int) txtLoc.getX();
        int y = (int) txtLoc.getY();

        graphics.setColor(Color.BLACK);
        graphics.drawString(text, x + 1, y + 1);

        graphics.setColor(color);
        graphics.drawString(text, x, y);
    }

    private void renderPoly(Graphics2D graphics, Color color, Shape polygon)
    {
        if (polygon != null) {
            graphics.setColor(color);
            graphics.setStroke(new BasicStroke((float) 1.2));
            graphics.draw(polygon);
            graphics.setColor(ColorUtil.colorWithAlpha(color, 20));
            graphics.fill(polygon);
        }
    }
}