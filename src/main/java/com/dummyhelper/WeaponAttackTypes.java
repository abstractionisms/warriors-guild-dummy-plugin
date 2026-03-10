package com.dummyhelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps weapon category (from VarbitID.COMBAT_WEAPON_CATEGORY) to the
 * attack type (Stab/Slash/Crush) each of the 4 style buttons produces.
 *
 * Source: https://oldschool.runescape.wiki/w/Weapons/Types
 */
public class WeaponAttackTypes
{
	/**
	 * Map of weaponCategory -> String[4] of attack types per button index.
	 * null means that button doesn't exist for this weapon.
	 */
	private static final Map<Integer, String[]> CATEGORY_ATTACK_TYPES = new HashMap<>();

	static
	{
		// Category IDs from VarbitID.COMBAT_WEAPON_CATEGORY
		// Values sourced from OSRS wiki Weapons/Types page

		// 0: Unarmed
		CATEGORY_ATTACK_TYPES.put(0, new String[]{"Crush", "Crush", "Crush", null});
		// 1: Axe
		CATEGORY_ATTACK_TYPES.put(1, new String[]{"Slash", "Slash", "Crush", "Slash"});
		// 2: Blunt (mace-like, 3 styles)
		CATEGORY_ATTACK_TYPES.put(2, new String[]{"Crush", "Crush", "Crush", null});
		// 3: Bow
		CATEGORY_ATTACK_TYPES.put(3, new String[]{null, null, null, null});
		// 4: Claws
		CATEGORY_ATTACK_TYPES.put(4, new String[]{"Slash", "Slash", "Stab", "Slash"});
		// 5: Crossbow
		CATEGORY_ATTACK_TYPES.put(5, new String[]{null, null, null, null});
		// 6: Salamander
		CATEGORY_ATTACK_TYPES.put(6, new String[]{null, null, null, null});
		// 7: Chinchompa
		CATEGORY_ATTACK_TYPES.put(7, new String[]{null, null, null, null});
		// 8: Gun (hand cannon)
		CATEGORY_ATTACK_TYPES.put(8, new String[]{null, null, null, null});
		// 9: Slash Sword (longsword, scimitar)
		CATEGORY_ATTACK_TYPES.put(9, new String[]{"Slash", "Slash", "Stab", "Slash"});
		// 10: 2h Sword
		CATEGORY_ATTACK_TYPES.put(10, new String[]{"Slash", "Slash", "Crush", "Slash"});
		// 11: Pickaxe
		CATEGORY_ATTACK_TYPES.put(11, new String[]{"Stab", "Stab", "Crush", "Stab"});
		// 12: Polearm (halberd)
		CATEGORY_ATTACK_TYPES.put(12, new String[]{"Stab", "Slash", "Stab", null});
		// 13: Polestaff
		CATEGORY_ATTACK_TYPES.put(13, new String[]{"Crush", "Crush", "Crush", null});
		// 14: Scythe
		CATEGORY_ATTACK_TYPES.put(14, new String[]{"Slash", "Slash", "Crush", "Slash"});
		// 15: Spear
		CATEGORY_ATTACK_TYPES.put(15, new String[]{"Stab", "Slash", "Crush", "Stab"});
		// 16: Spiked (mace with spike)
		CATEGORY_ATTACK_TYPES.put(16, new String[]{"Crush", "Crush", "Stab", "Crush"});
		// 17: Stab Sword (dagger, rapier)
		CATEGORY_ATTACK_TYPES.put(17, new String[]{"Stab", "Stab", "Slash", "Stab"});
		// 18: Staff (magic staff)
		CATEGORY_ATTACK_TYPES.put(18, new String[]{"Crush", "Crush", "Crush", null});
		// 19: Thrown (darts, knives)
		CATEGORY_ATTACK_TYPES.put(19, new String[]{null, null, null, null});
		// 20: Whip
		CATEGORY_ATTACK_TYPES.put(20, new String[]{"Slash", "Slash", "Slash", null});
		// 21: Banner
		CATEGORY_ATTACK_TYPES.put(21, new String[]{"Stab", "Slash", "Crush", "Stab"});
		// 22: Bludgeon
		CATEGORY_ATTACK_TYPES.put(22, new String[]{"Crush", "Crush", "Crush", null});
		// 23: Bulwark
		CATEGORY_ATTACK_TYPES.put(23, new String[]{"Crush", null, null, null});
		// 24: Partisan
		CATEGORY_ATTACK_TYPES.put(24, new String[]{"Stab", "Stab", "Crush", "Stab"});
	}

	/**
	 * Get the attack type for a given weapon category and style button index.
	 *
	 * @param weaponCategory from VarbitID.COMBAT_WEAPON_CATEGORY
	 * @param buttonIndex 0-3 for the 4 style buttons
	 * @return "Stab", "Slash", "Crush", or null if unknown
	 */
	public static String getAttackType(int weaponCategory, int buttonIndex)
	{
		String[] types = CATEGORY_ATTACK_TYPES.get(weaponCategory);
		if (types == null || buttonIndex < 0 || buttonIndex >= types.length)
		{
			return null;
		}
		return types[buttonIndex];
	}
}
