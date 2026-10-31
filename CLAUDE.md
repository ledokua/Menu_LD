# Menu_LD (26.1 branch) — notes for Claude Code sessions

Client-only title screen + loading screen customizer for Minecraft 26.1.2, NeoForge only, standalone Gradle
build (Java 25, ModDevGradle 2.0.148, NeoForge 26.1.2.114). The `1.21.1` branch (local checkout
`../Menu_LD`, this is its git worktree) holds the 1.21.1 Fabric + NeoForge MultiLoader build with the same
packages and class names: **port every change to both branches**. Written from scratch — FancyMenu is DSMSL
licensed: never copy or port its code, only its users' layout data.

## Read first
- `README.md` — user docs and the full config reference; update it with every config key change.
- `API.md` — addon API (`net.ledok.menuld.api`); keep in sync with `MenuLdApi`/`MenuLdAddon`.
- `CHANGELOG.md` — add an entry under `## Unreleased` for every behavior change (both branches).

## 26.x vs 1.21.1
- GUI: `GuiGraphicsExtractor` + `extract*` render methods, deferred (no shader color — opacity is the ARGB
  tint of each blit/fill), `Matrix3x2fStack` pose. Effects draw with batched `fill`/`fillGradient`.
- Textures: `GpuDevice.createTexture` + `SamplerCache` samplers + `CommandEncoder.writeToTexture`
  (`MenuImage.ImageTexture`). `Identifier`, `net.minecraft.util.Util`, NativeImage ARGB `setPixel`.
- Hooks: skins via `AbstractButton.extractDefaultSprite` HEAD, label color via NeoForge's `getFGColor()` in
  `extractDefaultLabel`; `MenuButton` implements `extractContents`. No SpriteIconButton mixin (icons fade
  already) and no NeoForge overlay mixin (NeoForge 26.x uses vanilla `LoadingOverlay` after its early window).
- Reference sources: `../emi/neoforge/build/moddev/artifacts/minecraft-patched-26.1.2.114-sources.jar`
  and the NeoForge `-sources.jar` in the Gradle cache.

## Build & test
- `./gradlew build` → `build/libs/Menu_LD-neoforge-<version>+26.1.2.jar`.
- Dev client: `run/config/menu_ld/` (git-ignored) — copy the Fallen Eden layout + assets from
  `../Menu_LD/fabric/run/config/menu_ld/`; `run/options.txt`: `onboardAccessibility:false`, `guiScale:3`,
  `soundCategory_master:0.01` (the dev client opens the owner's real audio device).
- Headless: own `Xvfb :97 -screen 0 1920x1080x24` (`:98`/`:99` belong to other sessions), then
  `env -u WAYLAND_DISPLAY DISPLAY=:97 ./gradlew runClient`; resize with `xdotool windowsize`, screenshot with
  `import -window <id>`. Only kill processes whose command line contains `Menu_LD-26.1/` — by PID, `pgrep -f`
  also matches your own shell. The dev-only "Create Test World" button is vanilla's (IDE runs only).
- Test returning to the title screen (Options → Escape): that path runs `TitleScreen.added()` on an existing
  screen and caught a crash once on 1.21.1.

## Conventions that matter
- The vanilla `TitleScreen` is edited in place, never replaced: other mods' buttons keep working. Layout rules
  run after everyone's init hooks (`ScreenEvent.Init.Post` at `LOWEST`). Widgets are hidden with
  `visible = false`, never removed.
- Realms notifications are hidden by never constructing `RealmsNotificationsScreen` (`NEW` wrap in
  `TitleScreenMixin.init`).
- Images: `MenuImages` cache → `MenuImage` = one GPU texture per image. Animations are a `FrameSource`:
  `GifDecoder` or `FrameArchive` (`.zip` of JPEG/PNG frames, decoded with STB and wrapped with
  the public pointer constructor because `NativeImage.read` rejects non-PNGs; 1.21.1 needs an invoker). They
  stream on the single "Menu_LD image decoder" thread (one frame ahead). The owner asked to keep RAM and
  load time low — no full-GIF predecoding.
- Audio: `.ogg` files stream from config through made-up `menu_ld:audio/<n>` ids that
  `SoundBufferLibraryMixin.getStream` resolves; `MusicManagerMixin` silences vanilla menu music while a
  `music` track is configured.
- Config parsing is hand-rolled (`ConfigParser`): lenient, every bad value → a path-prefixed problem string +
  default, unknown keys reported. Colors are CSS `#RRGGBB[AA]`.
