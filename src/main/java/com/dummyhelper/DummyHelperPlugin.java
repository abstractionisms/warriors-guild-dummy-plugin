package com.dummyhelper;

import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Animation;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.DynamicObject;
import net.runelite.api.EnumComposition;
import net.runelite.api.GameObject;
import net.runelite.api.StructComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.EnumID;
import net.runelite.api.ParamID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
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
	description = "Highlights the correct attack style for the active dummy in the Warriors Guild",
	tags = {"warriors", "guild", "dummy", "combat", "attack", "style", "defender"}
)
public class DummyHelperPlugin extends Plugin
{
	/**
	 * Dummy object IDs mapped to required style.
	 * Source: https://oldschool.runescape.wiki/w/Dummy_(Warriors%27_Guild)
	 */
	static final Map<Integer, String> DUMMY_STYLES = new HashMap<>();

	static
	{
		DUMMY_STYLES.put(23958, "Accurate");
		DUMMY_STYLES.put(23959, "Slash");
		DUMMY_STYLES.put(23960, "Aggressive");
		DUMMY_STYLES.put(23961, "Controlled");
		DUMMY_STYLES.put(23962, "Crush");
		DUMMY_STYLES.put(23963, "Stab");
		DUMMY_STYLES.put(23964, "Defensive");
	}

	private static final String[] STYLE_NAMES = {"Accurate", "Aggressive", "Controlled", "Defensive"};

	private static final int WARRIORS_GUILD_REGION_1 = 11575;
	private static final int WARRIORS_GUILD_REGION_2 = 11319;

	static final int COMBAT_GROUP_ID = 593;
	static final int[] STYLE_CHILDREN = {4, 8, 12, 16};

	@Getter(AccessLevel.PACKAGE)
	@Inject
	private Client client;

	@Inject
	private DummyHelperConfig config;

	@Inject
	private DummyHelperOverlay dummyOverlay;

	@Inject
	private CombatStyleOverlay combatOverlay;

	@Inject
	private OverlayManager overlayManager;

	private final Set<GameObject> trackedDummies = new HashSet<>();

	@Getter(AccessLevel.PACKAGE)
	private String requiredStyle = null;

	@Getter(AccessLevel.PACKAGE)
	private GameObject activeDummy = null;

	@Getter(AccessLevel.PACKAGE)
	private boolean inWarriorsGuild = false;

	@Getter(AccessLevel.PACKAGE)
	private int highlightButtonIndex = -1;

	private String[] buttonAttackTypes = new String[4];

	@Override
	protected void startUp()
	{
		overlayManager.add(dummyOverlay);
		overlayManager.add(combatOverlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(dummyOverlay);
		overlayManager.remove(combatOverlay);
		trackedDummies.clear();
		reset();
	}

	private void reset()
	{
		requiredStyle = null;
		activeDummy = null;
		highlightButtonIndex = -1;
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
		if (DUMMY_STYLES.containsKey(obj.getId()))
		{
			trackedDummies.add(obj);
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		GameObject obj = event.getGameObject();
		trackedDummies.remove(obj);
		if (obj.equals(activeDummy))
		{
			reset();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!inWarriorsGuild)
		{
			return;
		}

		ChatMessageType type = event.getType();
		if (type != ChatMessageType.SPAM && type != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String msg = event.getMessage().toLowerCase();

		// Successful hit clears the latch
		if (msg.contains("token") || msg.contains("you hit the dummy correctly"))
		{
			reset();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		inWarriorsGuild = checkInWarriorsGuild();

		if (!inWarriorsGuild)
		{
			reset();
			return;
		}

		// Only scan for a new dummy if nothing is latched
		if (requiredStyle == null)
		{
			scanForActiveDummy();
		}

		if (requiredStyle != null)
		{
			updateButtonAttackTypes();
			highlightButtonIndex = findMatchingButton(requiredStyle);
		}
	}

	private void scanForActiveDummy()
	{
		for (GameObject dummy : trackedDummies)
		{
			if (!(dummy.getRenderable() instanceof DynamicObject))
			{
				continue;
			}

			DynamicObject dynObj = (DynamicObject) dummy.getRenderable();
			Animation anim = dynObj.getAnimation();

			if (anim != null && anim.getId() != -1)
			{
				String style = DUMMY_STYLES.get(dummy.getId());
				if (style != null)
				{
					requiredStyle = style;
					activeDummy = dummy;
					log.debug("Dummy active: {} (ID {})", style, dummy.getId());
					return;
				}
			}
		}
	}

	private void updateButtonAttackTypes()
	{
		try
		{
			int weaponType = client.getVarbitValue(VarbitID.COMBAT_WEAPON_CATEGORY);
			EnumComposition weaponStylesEnum = client.getEnum(EnumID.WEAPON_STYLES);
			int styleEnumId = weaponStylesEnum.getIntValue(weaponType);
			EnumComposition styleEnum = client.getEnum(styleEnumId);
			int[] structIds = styleEnum.getIntVals();

			for (int i = 0; i < Math.min(structIds.length, 4); i++)
			{
				StructComposition struct = client.getStructComposition(structIds[i]);
				buttonAttackTypes[i] = struct.getStringValue(ParamID.ATTACK_STYLE_NAME);
			}
		}
		catch (Exception e)
		{
			log.debug("Could not read weapon style enums", e);
			Arrays.fill(buttonAttackTypes, null);
		}
	}

	private int findMatchingButton(String required)
	{
		// Direct combat style match (Accurate=0, Aggressive=1, Controlled=2, Defensive=3)
		for (int i = 0; i < STYLE_NAMES.length; i++)
		{
			if (STYLE_NAMES[i].equalsIgnoreCase(required))
			{
				return i;
			}
		}

		// Attack type match (Stab/Slash/Crush) — check what each button does with this weapon
		for (int i = 0; i < buttonAttackTypes.length; i++)
		{
			if (buttonAttackTypes[i] != null && buttonAttackTypes[i].equalsIgnoreCase(required))
			{
				return i;
			}
		}

		return -1;
	}

	Widget getStyleWidget(int index)
	{
		if (index < 0 || index >= STYLE_CHILDREN.length)
		{
			return null;
		}
		return client.getWidget(COMBAT_GROUP_ID, STYLE_CHILDREN[index]);
	}

	private boolean checkInWarriorsGuild()
	{
		if (client.getLocalPlayer() == null)
		{
			return false;
		}

		int[] regions = client.getMapRegions();
		if (regions == null)
		{
			return false;
		}

		return Arrays.stream(regions)
			.anyMatch(r -> r == WARRIORS_GUILD_REGION_1 || r == WARRIORS_GUILD_REGION_2);
	}
}
