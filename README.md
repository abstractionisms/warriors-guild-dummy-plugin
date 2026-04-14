# Warriors Guild Dummy Helper

A [RuneLite](https://runelite.net/) plugin for the **dummy room** in the [Warriors' Guild](https://oldschool.runescape.wiki/w/Warriors'_Guild). It takes the guesswork out of matching attack styles to dummies so you can earn warrior guild tokens faster on your way to a dragon defender.

## What is the dummy room?

The Warriors' Guild dummy room contains dummies that each require a specific attack style or type to hit: **Accurate**, **Aggressive**, **Controlled**, **Defensive**, **Stab**, **Slash**, and **Crush**. A dummy activates at random and you need to quickly switch to the correct combat style and attack it.

- **Correct hit**: 2 warrior guild tokens + 15 Attack XP
- **Wrong style**: You get stunned for 3 ticks and earn nothing

These tokens are what you spend to fight cyclopes on the top floor (bronze through rune defenders at 1/50 drop rate) and in the basement (dragon defender at 1/100). Most players need upwards of 2,000 tokens to finish the full defender grind, so fast token farming matters.

## What does this plugin do?

When a dummy activates, the plugin:

1. **Highlights the active dummy** with a colored tile box and a "Use: [style]" text label so you can spot it instantly
2. **Highlights the correct combat style button** in your combat tab based on your current weapon, so you know exactly which button to click without memorizing style tables
3. **Latches the detection** until you land a successful hit or the dummy stops animating, so the overlay won't flicker away too early

The plugin reads your equipped weapon's category varbit to figure out which combat option buttons correspond to which attack styles, so it works correctly regardless of what weapon you're using.

## Configuration

| Setting | Description | Default |
|---------|-------------|---------|
| Show Overlay | Display the dummy helper overlay panel | On |
| Correct Style Color | Color when your attack style matches the dummy | Green |
| Incorrect Style Color | Color when your attack style does not match | Red |
