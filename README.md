# Menu_LD

A lightweight title screen and loading screen customizer for Minecraft **1.21.1** (Fabric and NeoForge).
One config file restyles the main menu in place — background image or animated GIF, buttons moved,
hidden, retextured and recolored, extra link and join-server buttons, images, text, rain and fireflies,
menu music — and gives the resource-loading screen your own colors, logo and progress bar. It is the
small replacement for FancyMenu (plus Konkrete and Melody) when all you need is the main menu.

> Minecraft **1.21.1** · Java 21 · Fabric (needs [Fabric API](https://modrinth.com/mod/fabric-api)) or
> NeoForge. **Client-side only** — servers never need it. No in-game editor: layouts are a hand-written
> JSON5 file that reloads when you reopen the title screen. For Minecraft 26.1.2 (NeoForge) see the
> [`26.1` branch](https://github.com/ledokua/Menu_LD/tree/26.1); the config file is the same on both.

## Contents

[Getting started](#getting-started) · [How it works](#how-it-works) · [The config file](#the-config-file) ·
[Placement and fades](#placement-and-fades) · [Background](#background) · [Title screen](#title-screen) ·
[Widgets](#widgets--the-buttons-already-there) · [Elements](#elements--images-text-buttons) ·
[Button actions](#button-actions) · [Effects](#effects) · [Audio](#audio) ·
[Loading screen](#loading-screen) · [Text and languages](#text-and-languages) · [Assets](#assets) ·
[Coming from FancyMenu](#coming-from-fancymenu) · [File locations](#file-locations) ·
[Troubleshooting](#troubleshooting) · [Addon API](#addon-api) · [Compatibility](#compatibility) ·
[Notes for developers](#notes-for-developers) · [License](#license)

## Getting started

1. Drop the jar for your loader into `mods/` (plus Fabric API on Fabric).
2. Start the game once: `config/menu_ld/menu.json5` appears, changing nothing yet.
3. Put your images and `.ogg` files in `config/menu_ld/` (any subfolders) and describe the layout in
   `menu.json5` — start from [`docs/examples/fallen-eden.json5`](docs/examples/fallen-eden.json5).
4. Save, then click **Options…** and back: the title screen reloads the file whenever it changed.
   Turn on `"debug": true` under `title_screen` to see every widget's id and bounds.

## How it works

Menu_LD does not replace the title screen with a screen of its own. The vanilla screen opens as usual,
other mods add their buttons as usual (Mod Menu's *Mods*, NeoForge's *Mods*, …), and then the layout is
applied on top: listed widgets are moved, hidden or restyled, the layout's own elements are added, and
the panorama, logo and splash are swapped or switched off. Buttons keep their original behavior.

## The config file

`config/menu_ld/menu.json5` — JSON with `//` comments and trailing commas allowed. Every key is
optional; a missing key keeps vanilla behavior. A wrong value is reported and replaced by its default
(the rest of the layout still loads); problems show as a red line on the title screen and in the log.

| Key | What |
|---|---|
| `background` | The title screen background (and optionally every other menu's) — [Background](#background) |
| `audio` | Menu music and ambience — [Audio](#audio) |
| `title_screen` | Widgets, elements, effects and vanilla switches — [Title screen](#title-screen) |
| `loading_screen` | The resource-loading overlay — [Loading screen](#loading-screen) |

Colors are `"#RRGGBB"` or `"#RRGGBBAA"` (CSS order, as FancyMenu writes them).

## Placement and fades

Everything placed on screen (widgets, elements, the progress bar) shares these keys. Units are GUI
pixels, so a layout scales with the GUI scale like vanilla screens do.

| Key | Default | What |
|---|---|---|
| `anchor` | `top_left` | Screen point the offset starts from: `top_left`, `top`, `top_right`, `left`, `center`, `right`, `bottom_left`, `bottom`, `bottom_right` (FancyMenu names like `mid-centered` work too) |
| `x`, `y` | `0` | Offset of the element's top-left corner from the anchor point |
| `width`, `height` | own size | Image size: the file's pixel size; widgets keep their size; layout buttons default to 200×20 |
| `stretch_x`, `stretch_y` | `false` | Fill the whole screen width / height |
| `stay_on_screen` | `true` | Push the element back inside the screen when the offset would cut it off |
| `opacity` | `1` | 0–1 |
| `fade_in` | `0` | Seconds to fade from invisible to `opacity`, every time the title screen opens |
| `delay` | `0` | Seconds before the fade starts |

Fades start once the loading overlay is gone. Giving lower buttons longer fades (`1.4`, `1.6`, `1.8`…)
makes the menu cascade in.

## Background

```json5
"background": { "image": "my_pack/background.gif", "fit": "stretch", "smooth": true, "all_menus": false }
```

| Key | Default | What |
|---|---|---|
| `image` | — | PNG, GIF or frame archive ([Assets](#assets)) |
| `color` | none | Solid fill under the image (or instead of it) |
| `fit` | `stretch` | `stretch` (fill, ignore aspect ratio), `cover` (fill, crop), `contain` (whole image, letterboxed) |
| `smooth` | `true` | Linear filtering when scaled; `false` keeps pixel art crisp |
| `all_menus` | `false` | Also replace the panorama behind other menus outside a world (Options, server list, connecting…). Only the image: effects and elements stay on the title screen |

## Title screen

| Key | Default | What |
|---|---|---|
| `logo` | `true` | The vanilla Minecraft logo |
| `splash` | `true` | The yellow splash text |
| `realms_notifications` | `true` | Realms news icons (when off, nothing Realms-related runs on the title screen) |
| `hide_unlisted_widgets` | `false` | Hide every widget not named in `widgets` |
| `debug` | `false` | Outline every widget with its id (green: listed, yellow: unlisted, red: hidden) and list config problems |
| `button_style` | — | Default look for every listed or added button: `texture`, `hover_texture`, `disabled_texture`, `slice`, `smooth`, `label_color`, `label_hover_color` |
| `widgets` | — | Rules for the buttons already there — [Widgets](#widgets--the-buttons-already-there) |
| `elements` | — | Images, text and buttons to add — [Elements](#elements--images-text-buttons) |
| `effects` | — | Rain and fireflies — [Effects](#effects) |

**Button looks.** `texture` is stretched over the whole button unless `slice` (texture pixels) is set,
which keeps the corners at their size and stretches only edges and center (nine-slice) — use it when one
texture serves buttons of different widths. Label colors keep vanilla's gray for inactive buttons. Icon
buttons (language, accessibility) keep their icon on top of your texture.

## Widgets — the buttons already there

```json5
"widgets": {
  "multiplayer": { "hidden": true },
  "singleplayer": { "anchor": "center", "x": -65, "y": -13, "width": 130, "height": 21, "fade_in": 1.6 },
  "quit": { "hover_texture": "my_pack/button_red.png" },
  "copyright": {}
}
```

A rule takes the [placement keys](#placement-and-fades) (any of them moves the widget; none keeps it where
it is), the `button_style` keys, `hidden`, `label` and `tooltip` ([Text](#text-and-languages)). `{}` keeps
a widget unchanged — useful with `hide_unlisted_widgets`.

| Id | Widget |
|---|---|
| `singleplayer`, `multiplayer`, `realms` | The three main buttons |
| `mods` | Mod Menu's button (Fabric) or NeoForge's *Mods* button |
| `options`, `quit` | Bottom row |
| `language`, `accessibility` | The two icon buttons |
| `copyright` | "Copyright Mojang AB" (text only, so styles don't apply) |
| `play_demo`, `reset_demo` | Demo mode only |

Any other mod's widget is matched by its label's translation key, its full class name or its simple class
name — `debug` shows which one to use. Mods can also register short ids ([Addon API](#addon-api)).

## Elements — images, text, buttons

Listed in drawing order. Images and text draw behind the buttons unless `"front": true`.

```json5
{ "type": "image", "image": "my_pack/logo.png", "anchor": "top", "x": -90, "y": 28, "width": 179, "height": 42, "fade_in": 1 },
{ "type": "text", "text": { "en_us": "Season 3", "uk_ua": "Сезон 3" }, "anchor": "bottom", "y": -30, "align": "center", "color": "#FFD27A" },
{ "type": "button", "action": "link", "url": "https://example.com/shop", "tooltip": "Shop",
  "anchor": "center", "x": 74, "y": 45, "width": 20, "height": 20,
  "texture": "my_pack/coins.png", "hover_texture": "my_pack/coins_lit.png" }
```

| Type | Keys besides placement and fades |
|---|---|
| `image` | `image`, `fit` (`stretch`), `smooth` (`false`), `slice`, `front` |
| `text` | `text`, `color` (white), `shadow` (`true`), `scale` (`1`), `align` (`left` / `center` / `right` — which side of the text sits at x), `front` |
| `button` | `action` and its keys ([Button actions](#button-actions)), `label`, `tooltip`, the `button_style` keys |

## Button actions

| `action` | Extra keys | Does |
|---|---|---|
| `link` | `url`, `confirm` (`false`) | Opens the URL in the browser; `{player}` in it becomes the player's name. `confirm: true` asks first, like chat links |
| `join_server` | `address` | Connects straight to `host[:port]` (no server-list entry needed) |
| `singleplayer`, `multiplayer`, `realms`, `options`, `language`, `accessibility`, `quit` | — | Same as the vanilla buttons |
| `mods` | — | Mod Menu's list on Fabric (when installed), NeoForge's mod list |
| `<namespace>:<path>` | `argument` | An action another mod registered ([Addon API](#addon-api)) |

## Effects

```json5
"effects": [
  { "type": "rain", "color": "#CFE7FF", "thunder": true, "thunder_brightness": 0.2, "thunder_sound": "my_pack/thunder.ogg" },
  { "type": "fireflies", "color": "#FFE08A" }
]
```

| Key | Default | What |
|---|---|---|
| `type` | — | `rain` or `fireflies` |
| `intensity` | `1` | Amount (rain ≈ 250 drops, fireflies ≈ 24 on a 640×360 GUI) |
| `speed`, `scale` | `1` | Fall / drift speed; streak length or glow size |
| `color` | rain `#CFE7FF`, fireflies `#FFE08A` | |
| `layer` | `background` | `background`: right over the background, under images and buttons (the logo covers it) · `behind_buttons`: over images, under buttons · `front`: over everything |
| `hide_under_buttons` | `true` | Fade the effect out inside every visible button as it fades in, so see-through button textures don't show rain on top |
| `thunder` | `false` | Rain only: a double flash every 8–30 s |
| `thunder_brightness` | `0.3` | Peak flash strength |
| `thunder_sound`, `thunder_volume` | none, `1` | Rain only: an `.ogg` played just after each flash (weather volume slider) |
| `follow_mouse` | `true` | Fireflies only: drift toward a nearby cursor |

Effects cost a few kilobytes and one draw call each per frame; they load nothing. The thunder flash lights
the whole screen whatever the layer.

## Audio

```json5
"audio": [
  { "sound": "my_pack/menu_music.ogg", "source": "music", "loop": true },
  { "sound": "my_pack/rain.ogg", "source": "ambient", "volume": 0.6 }
]
```

| Key | Default | What |
|---|---|---|
| `sound` | — | An `.ogg` file in `config/menu_ld/` (streamed from disk), or a sound event id such as `minecraft:music.menu` |
| `source` | `music` | Volume slider it follows: `master`, `music`, `record`, `weather`, `block`, `hostile`, `neutral`, `player`, `ambient`, `voice` |
| `volume` | `1` | |
| `loop` | `true` | |
| `title_only` | `false` | Muted (fades out) while another menu such as Options or the server list is open, back on return — for ambience like rain that belongs to the title screen's effects |

Tracks start when the title screen first opens, keep playing through Options and the other menus
(`title_only` ones silently), and stop when you join a world. While any track uses `music`, vanilla's menu music stays silent. Only Ogg
Vorbis plays — convert other formats once, e.g. `ffmpeg -i music.wav -c:a libvorbis -q:a 4 music.ogg`.

## Loading screen

The overlay shown while resources load: at game start, on F3+T and after changing resource packs or
language. Vanilla keeps its timing and fades; the layout changes what it shows.

```json5
"loading_screen": {
  "color": "#070B1E",
  "background": { "image": "my_pack/background.gif", "fit": "cover" },
  "mojang_logo": false,
  "elements": [ { "type": "image", "image": "my_pack/logo.png", "anchor": "center", "x": -179, "y": -100, "width": 358, "height": 84 } ],
  "progress_bar": { "color": "#D3C9FF", "track_color": "#0B0F2AA0", "border_color": "#D3C9FF" }
}
```

| Key | Default | What |
|---|---|---|
| `color` | vanilla red / black | Background color |
| `background` | none | `image`, `color`, `fit`, `smooth` as in [Background](#background) |
| `mojang_logo` | `true` | The Mojang Studios logo |
| `elements` | — | `image` and `text` elements (no buttons, no fades — the overlay fades as a whole) |
| `progress_bar` | vanilla | `color`, `track_color`, `border_color`, `hidden`, and placement keys to move it (vanilla's position otherwise) |

Use files from `config/menu_ld/` here: resource-pack images are not readable while the first load runs.
**NeoForge:** NeoForge shows its own early loading window before the game window exists (mods cannot
change it); once the game window takes over, the layout covers NeoForge's overlay.

## Text and languages

Wherever text goes (`label`, `tooltip`, `text`):

```json5
"label": "Play"                                         // plain text
"label": { "en_us": "Play", "uk_ua": "Грати" }           // per language; falls back to en_us, then any
"label": { "translate": "menu.singleplayer" }           // a chat component: translate keys, colors, bold…
```

## Assets

| Format | Notes |
|---|---|
| PNG | Loaded when first shown |
| GIF | Animated. Streamed: only the file, one decoded frame and one texture stay in memory (a 9 MB, 213-frame 426×240 GIF uses ~10 MB, opens in a fraction of a second on a worker thread). Keep backgrounds small — 426×240 with `smooth` looks fine full-screen |
| ZIP | Frame archive: an animation as JPEG (or PNG) frames, see below. The better choice for full-screen footage |
| OGG | Audio, streamed from disk |

### Frame archives

GIFs only have 256 colors and compress footage where the whole picture moves very badly (a 14-second
1080p clip: 110 MB as a GIF, 12 MB as JPEG frames). A frame archive is a `.zip` of frames played in
file-name order, plus an optional `animation.json` with the speed (default 15 fps):

```json
{ "fps": 15 }
```

The archive stays on disk; the worker thread reads and decodes one frame ahead (natively, with the same
decoder Minecraft uses for PNGs), so memory holds one texture plus one decoded frame — 16 MB at 1080p,
whatever the frame count. All frames must have the same size. Zero-pad the numbers so names sort right.
Archives must be files under `config/menu_ld/`, not resource-pack ids. Making one from a GIF or video:

```bash
mkdir frames && ffmpeg -i background.gif -q:v 4 frames/%04d.jpg
echo '{ "fps": 15 }' > frames/animation.json
cd frames && zip -0 ../background.zip animation.json *.jpg
```

`-q:v` is the JPEG quality (2 = best, 31 = worst; 3–5 is a good range). `zip -0` stores without
recompressing — JPEGs don't shrink further and stored frames read faster. Use the source's frame rate
(`ffprobe background.gif` shows it as `r_frame_rate`).

A path like `my_pack/bg.gif` means `config/menu_ld/my_pack/bg.gif` (it cannot leave that folder).
`namespace:path` reads from resource packs instead (`minecraft:textures/gui/title/background/panorama_0.png`),
for images on the title screen and sounds in `audio`. Images are freed when you join a world and load
again when you are back in the menus.

## Coming from FancyMenu

| FancyMenu | Menu_LD |
|---|---|
| `menu_background` (image) | `background` |
| Vanilla button customization (`mc_titlescreen_*_button`) | `widgets.<id>` |
| `minecraft_logo_widget` / `minecraft_splash_widget` hidden | `"logo": false` / `"splash": false` |
| `minecraft_realms_notification_icons_widget` hidden | `"realms_notifications": false` |
| `image` element | `image` element (same anchors and offsets; `base_opacity` → `opacity`) |
| `custom_button` with `openlink` / `joinserver` | `button` with `link` / `join_server` |
| `fade_in_speed` with "every time" | `fade_in` seconds |
| `audio_v2` element | `audio` (convert WAV → OGG) |
| `rainfall` / `fireflies` decoration overlays | `effects` |
| `global_button_label_base_color` | `button_style.label_color` |

Positions copy over unchanged: FancyMenu's offsets are GUI pixels from the same anchor points.
Not ported: the editor, placeholders and requirements, slideshows, videos, shaders, per-screen layouts
other than the title screen.

## File locations

| What | Where |
|---|---|
| Layout | `config/menu_ld/menu.json5` |
| Images and sounds | `config/menu_ld/**` (or resource packs via `namespace:path`) |

## Troubleshooting

- **Red line on the title screen** — the config has problems; `"debug": true` lists them, the log has all.
- **A button is not affected** — turn on `debug` and use the id it shows.
- **No sound** — only `.ogg` plays; check the volume slider of the track's `source`.
- **Background black for a moment** — a GIF or frame archive is still opening; a `color` fill shows meanwhile.
- **Logo and buttons overlap on some screens** — positions are GUI pixels, and the GUI height varies with
  window size and GUI scale. Keep things that must not touch on the *same* anchor: a logo on `top` and
  buttons on `center` slide into each other on short screens. Minecraft's automatic GUI scale never goes
  below 240 GUI px tall (427×240 for a 16:9 window), so a `center`-anchored block that fits 240 px tall
  (about −110…+110, leaving room for the version text at the bottom) works everywhere.
- **Another mod's button sits in the wrong place** — turn on `debug`, read its id (class name for most
  mods, e.g. Create's `com.simibubi.create.infrastructure.gui.OpenCreateMenuButton`) and give it a rule.

## Addon API

Other mods can register button actions and widget ids through the `menu_ld` entrypoint (Fabric) or a
`ServiceLoader` provider (NeoForge) — see [`API.md`](API.md).

## Compatibility

| Mod | Notes |
|---|---|
| Mod Menu | Its *Mods* button is the `mods` widget; the `mods` action opens its list |
| FancyMenu | Both restyle the same screen — use one (declared as a conflict) |
| Other title-screen mods | Their buttons stay and can be moved, styled or hidden by class name or translation key |

## Notes for developers

MultiLoader layout (NPCs_LD template): `common/` holds all code (Mojmap, vanilla only), `fabric/` and
`neoforge/` are thin glue. Build with `./gradlew build`; dev clients with `./gradlew :fabric:runClient`
and `./gradlew :neoforge:runClient`. The [`26.1` branch](https://github.com/ledokua/Menu_LD/tree/26.1) has the
same packages and class names, so changes carry over file by file. Hooks: `TitleScreenMixin` (panorama,
logo, splash, Realms, front pass), `AbstractButtonMixin` + `SpriteIconButtonMixin` (skins, label color, icon
fade), `ScreenMixin` (`all_menus`), `LoadingOverlayMixin` (+ NeoForge-only `NeoForgeLoadingOverlayMixin`),
`MusicManagerMixin`, `SoundBufferLibraryMixin` (file streaming). Widget rules run after other mods' init
hooks (Fabric: a late `ScreenEvents.AFTER_INIT` phase; NeoForge: `ScreenEvent.Init.Post` at `LOWEST`).

## License

MIT — see [LICENSE.txt](LICENSE.txt). Written from scratch; no FancyMenu code is used.
