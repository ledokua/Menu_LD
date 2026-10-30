# Menu_LD addon API

Same API on every build; on 26.1.2 `ResourceLocation` is called `Identifier` (as in Minecraft itself).

Other mods can give layouts **button actions** of their own (`"action": "mymod:open_hub"`) and give their
title-screen widgets **short ids** for the layout's `widgets` map. Everything is client-side; the API is
the same on both loaders.

## Setup

Compile against the Menu_LD jar (`compileOnly`/`modCompileOnly`) and declare one class implementing
`net.ledok.menuld.api.MenuLdAddon`. Menu_LD loads it only when Menu_LD itself is installed, so it can stay
an optional dependency.

**Fabric** — `fabric.mod.json`:

```json
"entrypoints": {
  "menu_ld": ["com.example.mymod.client.MyMenuAddon"]
}
```

**NeoForge** — a `ServiceLoader` provider file in your jar:

```
META-INF/services/net.ledok.menuld.api.MenuLdAddon   → com.example.mymod.client.MyMenuAddon
```

`register()` runs once on the client while Menu_LD starts, before any screen opens.

## Button actions

```java
public final class MyMenuAddon implements MenuLdAddon {
    @Override
    public void register() {
        MenuLdApi.registerAction(ResourceLocation.fromNamespaceAndPath("mymod", "open_news"),
                (screen, argument) -> Minecraft.getInstance().setScreen(new NewsScreen(screen, argument)));
    }
}
```

A layout then uses it like any built-in action; `argument` is passed through as a string (empty when the
layout gives none):

```json5
{ "type": "button", "action": "mymod:open_news", "argument": "season3",
  "label": { "en_us": "News", "uk_ua": "Новини" }, "anchor": "center", "x": -100, "y": 100, "width": 200, "height": 20 }
```

`screen` is the title screen — use it as the parent of screens you open so Escape comes back to the menu.
An unregistered action id logs a warning when pressed and does nothing. Registering the same id again
replaces the action.

## Widget ids

Buttons your mod adds to the title screen are matched by translation key or class name unless you give
them an id:

```java
MenuLdApi.registerWidgetId("mymod:news", widget -> widget instanceof NewsButton);
```

Layouts can then write `"widgets": { "mymod:news": { "anchor": "top_right", "x": -24, "y": 4 } }`.
Matchers are checked in registration order before the built-in ids; keep them cheap (they run on every
title screen init and debug frame).

## Other calls

- `MenuLdApi.reloadLayout()` — re-reads `config/menu_ld/menu.json5` if it changed on disk; returns whether
  it did. The title screen picks the new layout up the next time it opens.

## Stability

Pre-1.0: `MenuLdAddon`, `registerAction`, `registerWidgetId` and `reloadLayout` are the intended long-term
surface; everything outside `net.ledok.menuld.api` is internal.
