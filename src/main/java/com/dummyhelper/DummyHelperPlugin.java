package com.dummyhelper;

import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Animation;
import net.runelite.api.Client;
import net.runelite.api.DynamicObject;
import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@PluginDescriptor(
	name = "Warriors Guild Dummy Helper",
	description = "Shows which attack style to use for the active dummy in the Warriors Guild",
	tags = {"warriors", "guild", "dummy", "combat", "attack", "style", "defender"}
)
public class DummyHelperPlugin extends Plugin
{
	/**
	 * Dummy object IDs and their required attack styles.
	 * The Warriors Guild ground floor has 7 dummies, each requiring a specific combat style.
	 * Source: https://oldschool.runescape.wiki/w/Dummy_(Warriors%27_Guild)
	 *
	 * These map directly to VarPlayer 43 style indices:
	 *   0 = Accurate, 1 = Aggressive, 2 = Controlled, 3 = Defensive
	 */
	private static final Map<Integer, String> DUMMY_ATTACK_STYLES = new HashMap<>();

	static
	{
		DUMMY_ATTACK_STYLES.put(23958, "Accurate");    // index 0
		DUMMY_ATTACK_STYLES.put(23959, "Slash");        // attack type
		DUMMY_ATTACK_STYLES.put(23960, "Aggressive");   // index 1
		DUMMY_ATTACK_STYLES.put(23961, "Controlled");   // index 2
		DUMMY_ATTACK_STYLES.put(23962, "Crush");        // attack type
		DUMMY_ATTACK_STYLES.put(23963, "Stab");         // attack type
		DUMMY_ATTACK_STYLES.put(23964, "Defensive");    // index 3
	}

	/**
	 * Styles that map directly to VarPlayer 43 indices (easy to match).
	 */
	private static final Map<String, Integer> DIRECT_STYLE_MAP = new HashMap<>();

	static
	{
		DIRECT_STYLE_MAP.put("Accurate", 0);
		DIRECT_STYLE_MAP.put("Aggressive", 1);
		DIRECT_STYLE_MAP.put("Controlled", 2);
		DIRECT_STYLE_MAP.put("Defensive", 3);
	}

	/**
	 * Warriors Guild region IDs (ground floor, where the dummy room is).
	 */
	private static final int WARRIORS_GUILD_REGION_1 = 11575;
	private static final int WARRIORS_GUILD_REGION_2 = 11319;

	/**
	 * VarPlayer index for the player's current attack style.
	 * Values: 0=Accurate, 1=Aggressive, 2=Controlled, 3=Defensive
	 */
	private static final int ATTACK_STYLE_VARPLAYER = 43;

	private static final String[] ATTACK_STYLE_NAMES = {"Accurate", "Aggressive", "Controlled", "Defensive"};

	@Inject
	private Client client;

	@Inject
	private DummyHelperConfig config;

	@Inject
	private DummyHelperOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	private final Set<GameObject> trackedDummies = new HashSet<>();

	@Getter(AccessLevel.PACKAGE)
	private String requiredStyle = null;

	@Getter(AccessLevel.PACKAGE)
	private String currentPlayerStyle = null;

	private boolean inWarriorsGuild = false;

	@Getter(AccessLevel.PACKAGE)
	private boolean styleMatches = false;

	/**
	 * Whether the required style is an attack type (Stab/Slash/Crush) that depends
	 * on the weapon, vs a direct style (Accurate/Aggressive/Controlled/Defensive).
	 */
	@Getter(AccessLevel.PACKAGE)
	private boolean weaponDependent = false;

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		trackedDummies.clear();
		requiredStyle = null;
		currentPlayerStyle = null;
		inWarriorsGuild = false;
		styleMatches = false;
		weaponDependent = false;
	}

	@Provides
	DummyHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DummyHelperConfig.class);
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject obj = event.getGameObject();
		if (DUMMY_ATTACK_STYLES.containsKey(obj.getId()))
		{
			trackedDummies.add(obj);
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		trackedDummies.remove(event.getGameObject());
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		inWarriorsGuild = isInWarriorsGuild();

		if (!inWarriorsGuild)
		{
			requiredStyle = null;
			currentPlayerStyle = null;
			styleMatches = false;
			weaponDependent = false;
			return;
		}

		int styleIndex = client.getVarpValue(ATTACK_STYLE_VARPLAYER);
		if (styleIndex >= 0 && styleIndex < ATTACK_STYLE_NAMES.length)
		{
			currentPlayerStyle = ATTACK_STYLE_NAMES[styleIndex];
		}
		else
		{
			currentPlayerStyle = "Unknown";
		}

		// Find the currently animated dummy
		requiredStyle = null;
		for (GameObject dummy : trackedDummies)
		{
			if (dummy.getRenderable() instanceof DynamicObject)
			{
				DynamicObject dynObj = (DynamicObject) dummy.getRenderable();
				Animation anim = dynObj.getAnimation();
				if (anim != null && anim.getId() != -1)
				{
					String style = DUMMY_ATTACK_STYLES.get(dummy.getId());
					if (style != null)
					{
						requiredStyle = style;
						break;
					}
				}
			}
		}

		if (requiredStyle != null && currentPlayerStyle != null)
		{
			weaponDependent = !DIRECT_STYLE_MAP.containsKey(requiredStyle);
			styleMatches = doesStyleMatch(requiredStyle, styleIndex);
		}
		else
		{
			styleMatches = false;
			weaponDependent = false;
		}
	}

	/**
	 * Checks if the player's current attack style matches the required dummy style.
	 *
	 * For Accurate/Aggressive/Controlled/Defensive dummies, this is a direct
	 * comparison against VarPlayer 43.
	 *
	 * For Stab/Slash/Crush dummies, the actual attack type depends on the equipped
	 * weapon. We cannot determine this from VarPlayer 43 alone, so we return false
	 * and let the overlay inform the player to check their combat tab.
	 */
	private boolean doesStyleMatch(String required, int styleIndex)
	{
		Integer directIndex = DIRECT_STYLE_MAP.get(required);
		if (directIndex != null)
		{
			return styleIndex == directIndex;
		}

		// Stab/Slash/Crush depend on weapon — can't determine from VarPlayer alone
		return false;
	}

	/**
	 * Checks whether the player is currently in the Warriors Guild area.
	 */
	boolean isInWarriorsGuild()
	{
		if (client.getLocalPlayer() == null)
		{
			return false;
		}

		WorldPoint wp = client.getLocalPlayer().getWorldLocation();
		if (wp == null || client.getMapRegions() == null)
		{
			return false;
		}

		return Arrays.stream(client.getMapRegions())
			.anyMatch(r -> r == WARRIORS_GUILD_REGION_1 || r == WARRIORS_GUILD_REGION_2);
	}
}
