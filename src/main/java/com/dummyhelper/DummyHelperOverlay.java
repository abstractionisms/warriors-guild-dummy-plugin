package com.dummyhelper;

import net.runelite.api.GameObject;
import net.runelite.api.Point;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;

/**
 * Draws a highlight box around the active dummy and shows the required style as text above it.
 */
public class DummyHelperOverlay extends Overlay
{
	private final DummyHelperPlugin plugin;
	private final DummyHelperConfig config;

	@Inject
	public DummyHelperOverlay(DummyHelperPlugin plugin, DummyHelperConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverlay() || !plugin.isInWarriorsGuild())
		{
			return null;
		}

		GameObject dummy = plugin.getActiveDummy();
		String style = plugin.getRequiredStyle();

		if (dummy == null || style == null)
		{
			return null;
		}

		// Draw box around the dummy
		Shape hull = dummy.getConvexHull();
		if (hull != null)
		{
			Color highlightColor = config.correctColor();
			graphics.setColor(highlightColor);
			graphics.setStroke(new BasicStroke(3));
			graphics.draw(hull);

			// Semi-transparent fill
			graphics.setColor(new Color(
				highlightColor.getRed(),
				highlightColor.getGreen(),
				highlightColor.getBlue(),
				50
			));
			graphics.fill(hull);
		}

		// Draw style text above the dummy
		LocalPoint localPoint = dummy.getLocalLocation();
		if (localPoint != null)
		{
			String text = "Use: " + style;
			Point textPoint = Perspective.getCanvasTextLocation(
				plugin.getClient(), graphics, localPoint, text, 150
			);

			if (textPoint != null)
			{
				Font original = graphics.getFont();
				graphics.setFont(original.deriveFont(Font.BOLD, 16f));
				OverlayUtil.renderTextLocation(graphics, textPoint, text, Color.WHITE);
				graphics.setFont(original);
			}
		}

		return null;
	}
}
