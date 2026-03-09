package com.dummyhelper;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

public class DummyHelperOverlay extends OverlayPanel
{
	private final DummyHelperPlugin plugin;
	private final DummyHelperConfig config;

	@Inject
	public DummyHelperOverlay(DummyHelperPlugin plugin, DummyHelperConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(OverlayPriority.HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverlay())
		{
			return null;
		}

		if (!plugin.isInWarriorsGuild())
		{
			return null;
		}

		String requiredStyle = plugin.getRequiredStyle();
		String currentStyle = plugin.getCurrentPlayerStyle();

		if (requiredStyle == null)
		{
			panelComponent.getChildren().add(TitleComponent.builder()
				.text("Dummy Helper")
				.color(Color.YELLOW)
				.build());

			panelComponent.getChildren().add(LineComponent.builder()
				.left("Waiting for dummy...")
				.build());

			return super.render(graphics);
		}

		boolean matches = plugin.isStyleMatches();
		boolean weaponDep = plugin.isWeaponDependent();
		Color statusColor = matches ? config.correctColor() : config.incorrectColor();

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Dummy Helper")
			.color(statusColor)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Use:")
			.right(requiredStyle)
			.rightColor(Color.WHITE)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Current:")
			.right(currentStyle != null ? currentStyle : "Unknown")
			.rightColor(statusColor)
			.build());

		if (weaponDep)
		{
			// Stab/Slash/Crush — can't auto-detect, tell user to check combat tab
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Status:")
				.right("Check combat tab")
				.rightColor(Color.YELLOW)
				.build());
		}
		else if (matches)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Status:")
				.right("Correct")
				.rightColor(config.correctColor())
				.build());
		}
		else
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Status:")
				.right("SWITCH!")
				.rightColor(config.incorrectColor())
				.build());
		}

		return super.render(graphics);
	}
}
