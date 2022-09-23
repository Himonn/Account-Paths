package com.accountpaths;

import com.google.common.base.Strings;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.client.util.ColorUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.List;

@Singleton
public class AccountPathsSceneOverlay extends Overlay {
    private final Client client;
    @Inject
    private AccountPathsPlugin plugin;
    private final AccountPathsConfig config;
    private final ModelOutlineRenderer outliner;

    Collection<Integer> consumedPositions = new ArrayList<>();

    private static final int INTERACTING_SHIFT = 0;
    private static final Polygon ARROW_HEAD = new Polygon(
            new int[]{0, -3, 3},
            new int[]{0, -5, -5},
            3
    );

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
//        if (plugin.currentTiles != null && plugin.currentTiles.size() != 0)
//        {
//            for (WorldPoint key : plugin.currentTiles.keySet())
//            {
//                renderWorldPoint(graphics, key, config.tileColour());
//                renderWorldPointText(graphics, key, plugin.currentTiles.get(key), config.labelColour());
////                renderLine(graphics, config.tileColour(),plugin.currentTiles.keySet());
//            }
//        }

        if (plugin.tileCollection != null && plugin.tileCollection.size() != 0)
        {
            for (AccountPathsTile tile : plugin.tileCollection)
            {
                if (config.highlightTiles())
                {
                    renderWorldPoint(graphics, tile.getWorldPoint(), config.tileColour());
                }

                if (config.drawLabels())
                {
                    renderWorldPointText(graphics, tile.getWorldPoint(), tile.getLabel(), config.labelColour());
                }
            }
        }

        if (config.drawPath())
        {
            renderLine(graphics, config.tileColour(), plugin.tileMap);
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

    private void renderLine(Graphics2D graphics, Color color, HashMap<Integer, AccountPathsTile> points)
    {
        for (Integer index : points.keySet())
        {
            AccountPathsTile tile = points.get(index);
            OptionalInt endint = points.keySet().stream().filter(i -> i > index).mapToInt(i -> i).min();
            WorldPoint start = tile.getWorldPoint();

            if (!endint.isPresent())
            {
                return;
            }

            WorldPoint end = points.get(endint.getAsInt()).getWorldPoint();

            if (start != null && end != null)
            {
                LocalPoint fl = LocalPoint.fromWorld(client, start);

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

                LocalPoint tl = LocalPoint.fromWorld(client, end);

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

                graphics.setColor(config.tileColour());
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