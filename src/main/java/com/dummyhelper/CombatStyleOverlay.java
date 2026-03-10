package com.dummyhelper;

import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Highlights the correct attack style button in the combat options tab.
 */
public class CombatStyleOverlay extends Overlay
{
	private final DummyHelperPlugin plugin;
	private final DummyHelperConfig config;

	@Inject
	public CombatStyleOverlay(DummyHelperPlugin plugin, DummyHelperConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverlay() || !plugin.isInWarriorsGuild())
		{
			return null;
		}

		int index = plugin.getHighlightButtonIndex();
		if (index < 0)
		{
			return null;
		}

		Widget widget = plugin.getStyleWidget(index);
		if (widget == null || widget.isHidden())
		{
			return null;
		}

		Rectangle bounds = widget.getBounds();
		if (bounds == null || bounds.width == 0)
		{
			return null;
		}

		Color color = config.correctColor();

		// Green fill
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
		graphics.fill(bounds);

		// Green border
		graphics.setColor(color);
		graphics.setStroke(new BasicStroke(3));
		graphics.draw(bounds);

		return null;
	}
}
