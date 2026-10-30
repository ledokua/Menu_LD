package net.ledok.menuld.title;

import com.mojang.realmsclient.RealmsMainScreen;
import net.ledok.menuld.MenuLd;
import net.ledok.menuld.api.MenuLdApi;
import net.ledok.menuld.config.MenuConfig;
import net.ledok.menuld.platform.Platform;
import net.ledok.menuld.render.Rect;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** A button the layout adds to the title screen. */
public final class MenuButton extends Button {
    MenuButton(Rect rect, Component label, OnPress onPress) {
        super(rect.x(), rect.y(), rect.width(), rect.height(), label, onPress, DEFAULT_NARRATION);
    }

    static void run(MenuConfig.Button spec, Screen screen) {
        Minecraft mc = Minecraft.getInstance();
        switch (spec.action()) {
            case LINK -> {
                String url = spec.target().replace("{player}",
                        URLEncoder.encode(mc.getUser().getName(), StandardCharsets.UTF_8));
                if (spec.confirm()) ConfirmLinkScreen.confirmLinkNow(screen, url);
                else Util.getPlatform().openUri(url);
            }
            case JOIN_SERVER -> {
                if (!ServerAddress.isValidAddress(spec.target())) {
                    MenuLd.LOGGER.warn("join_server button: '{}' is not a valid server address", spec.target());
                    return;
                }
                ServerData server = new ServerData(spec.target(), spec.target(), ServerData.Type.OTHER);
                ConnectScreen.startConnecting(screen, mc, ServerAddress.parseString(spec.target()), server, false, null);
            }
            case SINGLEPLAYER -> mc.setScreen(new SelectWorldScreen(screen));
            case MULTIPLAYER -> mc.setScreen(mc.options.skipMultiplayerWarning
                    ? new JoinMultiplayerScreen(screen) : new SafetyScreen(screen));
            case REALMS -> mc.setScreen(new RealmsMainScreen(screen));
            case OPTIONS -> mc.setScreen(new OptionsScreen(screen, mc.options));
            case MODS -> {
                Screen mods = Platform.INSTANCE.modsScreen(screen);
                if (mods != null) mc.setScreen(mods);
            }
            case LANGUAGE -> mc.setScreen(new LanguageSelectScreen(screen, mc.options, mc.getLanguageManager()));
            case ACCESSIBILITY -> mc.setScreen(new AccessibilityOptionsScreen(screen, mc.options));
            case QUIT -> mc.stop();
            case CUSTOM -> {
                MenuLdApi.MenuAction action = spec.customAction() == null ? null : MenuLdApi.action(spec.customAction());
                if (action == null) {
                    MenuLd.LOGGER.warn("No mod registered the button action '{}'", spec.customAction());
                    return;
                }
                action.run(screen, spec.target());
            }
        }
    }
}
