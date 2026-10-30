# Changelog

## Unreleased

## 0.2.0

- **Minecraft 26.1.2 (NeoForge)** build, on the `26.1` branch — same config file and features.
  On 26.x the NeoForge startup screen is vanilla's overlay, so the loading screen layout covers it directly.
- Effects get `layer` (`background` by default — rain and fireflies now pass behind the logo and other
  images; `behind_buttons`; `front`) instead of `front`, and `hide_under_buttons` (default on): effects
  fade out inside buttons as they appear, so see-through button textures no longer show rain on top.

## 0.1.0

First release.

- Title screen layouts from `config/menu_ld/menu.json5`: background image or animated GIF, vanilla and
  other mods' buttons moved, hidden, retextured (optionally nine-sliced) and recolored, logo/splash/Realms
  switches, image/text/button elements with anchors and staggered fade-ins.
- Button actions: open link (`{player}` placeholder), join server, every vanilla screen, mod list, and
  actions registered by other mods.
- Rain (with thunder flashes and sound) and fireflies effects.
- Menu music and ambience streamed from `.ogg` files; vanilla menu music silenced while a music track plays.
- Loading screen: background color and image, own logo images, styled progress bar; covers NeoForge's
  startup overlay too.
- Optional: the layout background behind every menu outside a world.
- Addon API: `menu_ld` entrypoint / `MenuLdAddon` provider, `registerAction`, `registerWidgetId`.
- Debug overlay with widget ids and config problems; live reload when the title screen reopens.
