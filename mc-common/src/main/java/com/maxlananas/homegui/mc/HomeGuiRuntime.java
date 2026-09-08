package com.maxlananas.homegui.mc;

import com.maxlananas.homegui.core.ConfigStore;
import com.maxlananas.homegui.core.Home;
import com.maxlananas.homegui.core.HomesController;
import com.maxlananas.homegui.core.Position;
import com.maxlananas.homegui.core.ServerKey;
import com.maxlananas.homegui.ui.HomeGuiUi;
import com.maxlananas.homegui.ui.Localization;
import com.maxlananas.homegui.ui.UiHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.I18n;

import java.nio.file.Path;

/**
 * Wires the loader independent core and interface to the running client.
 *
 * <p>One instance per client. The loader entrypoint supplies the configuration
 * directory and a tick; everything else, including detecting that the player joined
 * or left a server, is derived from the client itself so that no loader specific
 * networking event is needed.
 */
public final class HomeGuiRuntime implements UiHost {

    private static HomeGuiRuntime instance;

    private final ConfigStore config;
    private final HomesController homes;
    private final Localization text;
    private final HomeGuiUi ui;
    private GuiPainter painter;

    private String serverKey = ServerKey.LOCAL;
    private boolean wasConnected;
    private boolean invalid = true;
    private Object openKey;

    private HomeGuiRuntime(Path configDir) {
        this.config = ConfigStore.open(configDir);
        this.text = new Localization();
        this.text.setSource(new MinecraftTranslations());
        this.homes = new HomesController(this::send);
        this.homes.setListener(new HomesController.Listener() {
            @Override
            public void onTeleportConfirmed(String home, Position destination, long now) {
                config.recordTeleport(home, destination.toCoordinates(), now);
            }

            @Override
            public void onRequestChanged(com.maxlananas.homegui.core.HomeRequest request) {
                if (request.state() == com.maxlananas.homegui.core.RequestState.READY) {
                    config.rememberReportedCoordinates(request.homes());
                }
            }
        });
        this.ui = new HomeGuiUi(config, homes, this, text);
        applyLanguage();
    }

    /** Creates the singleton. Called once from the loader entrypoint. */
    public static synchronized void initialize(Path configDir) {
        if (instance == null) instance = new HomeGuiRuntime(configDir);
    }

    public static HomeGuiRuntime get() {
        if (instance == null) throw new IllegalStateException("HomeGui has not been initialised");
        return instance;
    }

    public static boolean isReady() {
        return instance != null;
    }

    /**
     * The keybind that opens the interface, kept as an opaque object because the
     * {@code KeyMapping} matching API differs between Minecraft generations. Only the
     * generation specific screen ever casts it back.
     */
    public void setOpenKey(Object keyMapping) {
        this.openKey = keyMapping;
    }

    public Object openKey() { return openKey; }

    public ConfigStore config() { return config; }

    public HomesController homes() { return homes; }

    public HomeGuiUi ui() { return ui; }

    public Localization text() { return text; }

    /** Lazily built: the font is not guaranteed to exist during mod initialisation. */
    public GuiPainter painter(GuiGraphics graphics) {
        if (painter == null) painter = new GuiPainter(Minecraft.getInstance().font);
        return painter.bind(graphics);
    }

    /** Opens the interface and asks the server for its home list. */
    public void open(Minecraft minecraft) {
        if (minecraft.player == null) return;
        syncServer(minecraft);
        homes.requestHomes();
        ui.resize(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        minecraft.setScreen(new HomeGuiScreen(this));
    }

    /** Called once per client tick. */
    public void tick(Minecraft minecraft) {
        syncServer(minecraft);
        boolean connected = minecraft.player != null && minecraft.player.connection != null;
        if (connected) {
            homes.onPlayerPosition(currentPosition(minecraft));
        } else if (wasConnected) {
            homes.onDisconnected();
        }
        wasConnected = connected;
        homes.tick();
        ui.tick();
    }

    /** True when the widgets must be rebuilt. */
    public boolean consumeInvalidation() {
        boolean wasInvalid = invalid;
        invalid = false;
        return wasInvalid;
    }

    private void syncServer(Minecraft minecraft) {
        String key = serverKeyOf(minecraft);
        if (key.equals(serverKey)) return;
        serverKey = key;
        config.setCurrentServer(key);
        homes.setServer(key);
        applyLanguage();
        invalid = true;
    }

    private static String serverKeyOf(Minecraft minecraft) {
        if (minecraft.player == null) return ServerKey.LOCAL;
        ServerData server = minecraft.getCurrentServer();
        if (server != null) return ServerKey.fromAddress(server.ip);
        if (minecraft.isConnectedToRealms()) return "realms";
        return ServerKey.LOCAL;
    }

    private static Position currentPosition(Minecraft minecraft) {
        var position = minecraft.player.position();
        return new Position(position.x, position.y, position.z,
                minecraft.player.level().dimension().location().toString());
    }

    private boolean send(String command) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.connection == null) return false;
        minecraft.player.connection.sendCommand(command);
        return true;
    }

    private void applyLanguage() {
        text.setLanguage(config.preferences().language());
        text.setClientLocale(clientLocale());
    }

    /**
     * Best effort. The client language accessor has moved between generations, and
     * the language setting degrades to English rather than crashing the interface.
     */
    private static String clientLocale() {
        try {
            return Minecraft.getInstance().getLanguageManager().getSelected().getCode();
        } catch (Throwable ignored) {
            return "en_us";
        }
    }

    // ------------------------------------------------------------------ UiHost

    @Override
    public void close() {
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void refresh() {
        homes.requestHomes();
    }

    @Override
    public void announce(String message) {
        try {
            Minecraft.getInstance().getNarrator().saySystemNow(message);
        } catch (Throwable ignored) {
            // narration is a courtesy, never a requirement
        }
    }

    @Override
    public void invalidate() {
        invalid = true;
    }

    @Override
    public long now() {
        return System.currentTimeMillis();
    }

    /** Hands Minecraft's own translations to the interface, so resource packs win. */
    private static final class MinecraftTranslations implements Localization.Source {
        @Override
        public boolean has(String key) {
            return I18n.exists(key);
        }

        @Override
        public String get(String key) {
            return I18n.get(key);
        }
    }

    /** Reports a home list that arrived while the interface was closed. */
    public static void onChatLine(String line) {
        if (instance != null) instance.homes.onChatLine(line);
    }

    /** Reports a command the client is about to send. */
    public static void onCommandSent(String command) {
        if (instance == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        Position origin = minecraft.player == null ? null : currentPosition(minecraft);
        instance.homes.onCommandSent(command, origin);
    }

    /** Homes the last successful reply reported, for other UI surfaces. */
    public static java.util.List<Home> lastHomes() {
        return instance == null ? java.util.List.of() : instance.homes.homes();
    }
}
