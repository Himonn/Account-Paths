package com.accountpaths;

import com.google.common.base.Strings;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.util.ColorUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.List;

@Singleton
public class AccountPathsSceneOverlay extends Overlay
{
    private final Client client;
    @Inject
    private AccountPathsPlugin plugin;
    private final AccountPathsConfig config;

    private static final Polygon ARROW_HEAD = new Polygon(
            new int[]{0, -3, 3},
            new int[]{0, -5, -5},
            3
    );

    @Inject
    private AccountPathsSceneOverlay(final Client client, final AccountPathsPlugin plugin, final AccountPathsConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (plugin.tileCollection != null && plugin.tileCollection.size() != 0)
        {
            for (AccountPathsTile tile : plugin.tileCollection)
            {
                if (config.highlightTiles())
                {
                    for (WorldPoint wp : tile.getPathfinder().getPath())
                    {
                        renderWorldPoint(graphics, wp, config.tileColour());
                    }
                }

                if (config.drawLabels())
                {
                    renderWorldPointText(graphics, tile.getStart(), tile.getStartLabel(), config.labelColour());
                    renderWorldPointText(graphics, tile.getEnd(), tile.getEndLabel(), config.labelColour());
                }

                if (config.drawPath())
                {
                    renderLine(graphics, config.tileColour(), tile);
                }
            }
        }

        if (plugin.getPathfinder() != null)
        {
            for (WorldPoint wp : plugin.getPathfinder().getPath())
            {
                renderWorldPoint(graphics, wp, config.tileColour());
            }
        }

        if (plugin.start != null && plugin.startLabel != null && !plugin.startLabel.equals(""))
        {
            renderWorldPointText(graphics, plugin.start, plugin.startLabel, Color.WHITE);
        }

        if (plugin.end != null && plugin.endLabel != null && !plugin.endLabel.equals(""))
        {
            renderWorldPointText(graphics, plugin.end, plugin.endLabel, Color.WHITE);
        }


        return null;
    }

    private void renderWorldPoint(Graphics2D graphics, WorldPoint worldPoint, Color color)
    {
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

    public void renderTextLocation(Graphics2D graphics, Point txtLoc, String text, Color color)
    {
        if (Strings.isNullOrEmpty(text))
        {
            return;
        }

        int x = (int) txtLoc.getX();
        int y = (int) txtLoc.getY();

        if (config.drawPath())
        {
            y = y + 10;
        }

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

    private void renderLine(Graphics2D graphics, Color color, AccountPathsTile apt)
    {
        List<WorldPoint> path = apt.getPathfinder().getPath();

        for (int i = 0; i < path.size() - 1; i++)
        {
            WorldPoint base = path.get(i);
            WorldPoint tip = path.get(i+1);

            if (base != null && tip != null)
            {
                LocalPoint fl = LocalPoint.fromWorld(client, base);

                if (fl == null)
                {
                    continue;
                }

                net.runelite.api.Point fs = Perspective.localToCanvas(client, fl, client.getPlane());

                if (fs == null)
                {
                    return;
                }

                int fsx = fs.getX();
                int fsy = fs.getY();

                LocalPoint tl = LocalPoint.fromWorld(client, tip);

                if (tl == null)
                {
                    continue;
                }

                net.runelite.api.Point ts = Perspective.localToCanvas(client, tl, client.getPlane());

                if (ts == null)
                {
                    return;
                }
                int tsx = ts.getX();
                int tsy = ts.getY();

                graphics.setColor(color);
                graphics.drawLine(tsx, tsy, fsx, fsy);

                AffineTransform t = new AffineTransform();
                t.translate(tsx, tsy);
                t.rotate(tsx - fsx, tsy - fsy);
                t.rotate(Math.PI / -2);
                AffineTransform ot = graphics.getTransform();
                graphics.setTransform(t);
                graphics.fill(ARROW_HEAD);
                graphics.setTransform(ot);
            }
        }
    }
}