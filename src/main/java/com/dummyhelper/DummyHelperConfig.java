package com.dummyhelper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.awt.Color;

@ConfigGroup("dummyhelper")
public interface DummyHelperConfig extends Config
{
	@ConfigItem(
		keyName = "showOverlay",
		name = "Show Overlay",
		description = "Display the dummy helper overlay panel",
		position = 1
	)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "correctColor",
		name = "Correct Style Color",
		description = "Color shown when your attack style matches the dummy",
		position = 2
	)
	default Color correctColor()
	{
		return Color.GREEN;
	}

	@ConfigItem(
		keyName = "incorrectColor",
		name = "Incorrect Style Color",
		description = "Color shown when your attack style does not match the dummy",
		position = 3
	)
	default Color incorrectColor()
	{
		return Color.RED;
	}
}
