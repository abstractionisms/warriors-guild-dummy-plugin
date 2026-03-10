package com.dummyhelper;

import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Animation;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.DynamicObject;
import net.runelite.api.GameObject;
import net.runelite.api.Varbits;

import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
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
	 * Dummy object ID -> required style/attack type.
	 * IDs from the Warriors Guild basement dummy room.
	 * Values are either a combat style name (Accurate, Aggressive, Controlled, Defensive)
	 * or an attack type (Stab, Slash, Crush).
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

	/**
	 * The 4 combat style names in VarPlayer 43 (COMBAT_STYLE) order.
	 * Used for direct index lookup when a dummy requires a combat style.
	 */
	private static final String[] STYLE_NAMES = {"Accurate", "Aggressive", "Controlled", "Defensive"};

	// Warriors Guild map region IDs (basement + ground floor)
	private static final int WARRIORS_GUILD_REGION_1 = 11575;
	private static final int WARRIORS_GUILD_REGION_2 = 11319;

	/**
	 * Combat options widget group (593) and the 4 style button children.
	 * Children 8/12/16/20 are the clickable style containers;
	 * child 4 is the combat level display (not a button).
	 */
	static final int COMBAT_GROUP_ID = 593;
	static final int[] STYLE_CHILDREN = {8, 12, 16, 20};

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

		// Successful hit — clear and wait for next dummy
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

		// If we have a latched dummy, verify it's still animating.
		// If it stopped (timed out or wrong hit), release the latch.
		if (activeDummy != null)
		{
			if (!isDummyAnimating(activeDummy))
			{
				reset();
			}
		}

		if (requiredStyle == null)
		{
			scanForActiveDummy();
		}

		if (requiredStyle != null)
		{
			highlightButtonIndex = findMatchingButton(requiredStyle);
		}
	}

	private boolean isDummyAnimating(GameObject dummy)
	{
		if (!(dummy.getRenderable() instanceof DynamicObject))
		{
			return false;
		}

		DynamicObject dynObj = (DynamicObject) dummy.getRenderable();
		Animation anim = dynObj.getAnimation();
		return anim != null && anim.getId() != -1;
	}

	private void scanForActiveDummy()
	{
		for (GameObject dummy : trackedDummies)
		{
			if (isDummyAnimating(dummy))
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

	/**
	 * Find which button (0-3) to highlight for the required style.
	 *
	 * Combat styles (Accurate/Aggressive/Controlled/Defensive) map directly
	 * to button indices 0-3 since they follow VarPlayer 43 order.
	 *
	 * Attack types (Stab/Slash/Crush) require reading the equipped weapon
	 * category from Varbits.EQUIPPED_WEAPON_TYPE, then consulting the
	 * WeaponAttackTypes table to find which button produces the needed type.
	 */
	private int findMatchingButton(String required)
	{
		// Combat styles map directly to button indices
		for (int i = 0; i < STYLE_NAMES.length; i++)
		{
			if (STYLE_NAMES[i].equalsIgnoreCase(required))
			{
				// Verify the button widget exists
				Widget w = client.getWidget(COMBAT_GROUP_ID, STYLE_CHILDREN[i]);
				if (w != null && !w.isHidden())
				{
					return i;
				}
				return -1;
			}
		}

		// Attack types (Stab/Slash/Crush) — use weapon category table
		int weaponCategory = client.getVarbitValue(Varbits.EQUIPPED_WEAPON_TYPE);

		for (int i = 0; i < STYLE_CHILDREN.length; i++)
		{
			Widget w = client.getWidget(COMBAT_GROUP_ID, STYLE_CHILDREN[i]);
			if (w == null || w.isHidden())
			{
				continue;
			}

			String attackType = WeaponAttackTypes.getAttackType(weaponCategory, i);
			if (attackType != null && attackType.equalsIgnoreCase(required))
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
