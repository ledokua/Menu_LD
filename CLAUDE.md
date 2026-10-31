# Menu_LD — notes for Claude Code sessions

Client-only title screen + loading screen customizer for Minecraft 1.21.1, Fabric and NeoForge, Mojmap,
MultiLoader layout (`common/` holds all the code; `fabric/` and `neoforge/` are thin glue). Replaces
FancyMenu (+ Konkrete, Melody) in the Fallen Eden pack. Written from scratch — FancyMenu is DSMSL
licensed: never copy or port its code, only its users' layout data.

## Read first
- `README.md` — user docs and the full config reference; update it with every config key change.
- `API.md` — addon API (`net.ledok.menuld.api`); keep in sync with `MenuLdApi`/`MenuLdAddon`.
- `CHANGELOG.md` — add an entry under `## Unreleased` for every behavior change.

## Two branches
- `1.21.1` (this checkout): Fabric + NeoForge MultiLoader, Java 21.
- `26.1` (git worktree `../Menu_LD-26.1`, the default branch on GitHub): NeoForge 26.1.2 only, standalone
  Gradle build at its root (Java 25, ModDevGradle 2.0.148, NeoForge 26.1.2.114). Same packages and class
  names as `common/`: **port every change to both branches**. That branch's CLAUDE.md lists the 26.x API
  differences (deferred `GuiGraphicsExtractor`, GPU textures, `Identifier`, fewer hooks).

## Build & test
- `./gradlew build`. Check the Fabric jar's mixins are remapped: `strings` on
  `net/ledok/menuld/mixin/*.class` must show `method_xxxx`/`class_xxxx` targets (MixinExtras
  `@WrapOperation`/`@WrapWithCondition`/`NEW` targets included — tiny-remapper 0.14 handles them).
- Dev clients: `fabric/run/config/menu_ld/` holds the Fallen Eden layout + converted assets (git-ignored;
  `docs/examples/fallen-eden.json5` is the tracked copy of the layout). Copy it to `neoforge/run/config/`
  for NeoForge runs.
- Headless: your own `Xvfb :97 -screen 0 1920x1080x24` (`:98`/`:99` belong to other sessions), then
  `env -u WAYLAND_DISPLAY DISPLAY=:97 ./gradlew :fabric:runClient --args="--width 1920 --height 1080"`;
  `fabric/run/options.txt` has `guiScale:3` (the layout's design size), `onboardAccessibility:false` and
  `soundCategory_master:0.01` — the dev client opens the owner's real audio device, keep it quiet.
  Screenshots: `DISPLAY=:97 import -window root x.png`; drive with `xdotool` (GUI px × 3 = screen px).
  Other sessions run clients on other displays (`:98`, `:1`) — only ever kill processes whose command
  line contains `Menu_LD/`.
- Test returning to the title screen (Options → Escape, Mods → Escape): that path runs `TitleScreen.added()`
  on an existing screen and caught a crash once.

## Conventions that matter
- The vanilla `TitleScreen` is edited in place, never replaced: other mods' buttons and mixins keep
  working. Layout rules run after everyone's init hooks (Fabric: late `ScreenEvents.AFTER_INIT` phase;
  NeoForge: `ScreenEvent.Init.Post` at `LOWEST`). Widgets are hidden with `visible = false`, never removed.
- Realms notifications are hidden by never constructing `RealmsNotificationsScreen` (`NEW` wrap in
  `TitleScreenMixin.init`); faking `realmsNotificationsEnabled()` instead crashes in `added()` on re-open.
- Button skins live on every `AbstractButton` via the `SkinnedButton` duck (`AbstractButtonMixin`).
- Images: `MenuImages` cache → `MenuImage` = one GL texture per image. Animations are a `FrameSource`:
  `GifDecoder` or `FrameArchive` (`.zip` of JPEG/PNG frames, decoded with STB through the
  `NativeImageInvoker` private-constructor invoker because `NativeImage.read` rejects non-PNGs). They
  stream on the single "Menu_LD image decoder" thread (one frame ahead; never touch a source from another
  thread). The owner asked to keep RAM and load time low — no full-GIF predecoding.
- Audio: `.ogg` files stream from config through made-up `menu_ld:audio/<n>` ids that
  `SoundBufferLibraryMixin.getStream` resolves; `MusicManagerMixin` silences vanilla menu music while a
  `music` track is configured.
- NeoForge's startup overlay (`NeoForgeLoadingOverlay`) overrides `render`, so the neoforge module's own
  mixin covers it at TAIL (restoring the GUI projection first); vanilla `LoadingOverlay` is edited in
  common.
- Effects (rain, fireflies) write all quads into one `RenderType.gui()` buffer and flush once.
- Config parsing is hand-rolled (`ConfigParser`): lenient, every bad value → a path-prefixed problem
  string + default, unknown keys reported. Colors are CSS `#RRGGBB[AA]`.
