package com.example.homegui.screen;

import com.example.homegui.HomesManager;
import com.example.homegui.config.LangManager;
import com.example.homegui.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HomesScreen extends Screen {

    // ═══════════════════════════════════════════
    // CONSTANTES
    // ═══════════════════════════════════════════
    private static final int CARD_HEIGHT = 50;
    private static final int CARD_HEIGHT_COMPACT = 28;
    private static final int CARD_SPACING = 8;
    private static final int COLUMNS = 3;
    private static final int COLUMNS_COMPACT = 1;

    // ═══════════════════════════════════════════
    // THÈMES
    // ═══════════════════════════════════════════
    public enum Theme {
        VIOLET(0xFF6B4EE6, 0xFF9D4EDD, 0xFF4ECDC4, 0xFF101020, "Violet"),
        OCEAN(0xFF0077B6, 0xFF00B4D8, 0xFF90E0EF, 0xFF001219, "Ocean"),
        FOREST(0xFF2D6A4F, 0xFF40916C, 0xFF95D5B2, 0xFF081C15, "Forest"),
        SUNSET(0xFFE85D04, 0xFFFF8C42, 0xFFFFD166, 0xFF1A0A0A, "Sunset"),
        CHERRY(0xFFFF006E, 0xFFFF5C8D, 0xFFFFB3C6, 0xFF1A0510, "Cherry");

        public final int primary;
        public final int secondary;
        public final int accent;
        public final int background;
        public final String name;

        Theme(int primary, int secondary, int accent, int background, String name) {
            this.primary = primary;
            this.secondary = secondary;
            this.accent = accent;
            this.background = background;
            this.name = name;
        }
    }

    // ═══════════════════════════════════════════
    // VARIABLES
    // ═══════════════════════════════════════════
    private float animationProgress = 0f;
    private long openTime;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private boolean compactMode = false;

    private Theme currentTheme = Theme.VIOLET;
    private int themeIndex = 0;

    private TextFieldWidget searchField;
    private String searchQuery = "";

    private final List<HomeButton> homeButtons = new ArrayList<>();
    private HomeButton hoveredButton = null;

    private String notificationText = "";
    private long notificationTime = 0;
    private int notificationColor = 0xFFFFFFFF;
    
    private LangManager lang;

    // ═══════════════════════════════════════════
    // CONSTRUCTEUR
    // ═══════════════════════════════════════════
    public HomesScreen() {
        super(Text.literal("Homes"));
        this.openTime = System.currentTimeMillis();
        this.themeIndex = ModConfig.getInstance().getThemeIndex();
        this.currentTheme = Theme.values()[themeIndex % Theme.values().length];
        this.compactMode = ModConfig.getInstance().isCompactMode();
        this.lang = LangManager.getInstance();
        this.lang.loadFromConfig();
    }

    @Override
    protected void init() {
        super.init();

        int searchWidth = 180;
        this.searchField = new TextFieldWidget(
            this.textRenderer,
            this.width / 2 - searchWidth / 2,
            55,
            searchWidth,
            18,
            Text.literal("")
        );
        this.searchField.setPlaceholder(Text.literal("§7Search..."));
        this.searchField.setChangedListener(query -> {
            this.searchQuery = query.toLowerCase();
            rebuildButtons();
        });
        this.addDrawableChild(searchField);

        rebuildButtons();
    }

    private void rebuildButtons() {
        homeButtons.clear();

        List<String> allHomes = HomesManager.getInstance().getHomes();

        List<String> filteredHomes = allHomes.stream()
            .filter(home -> searchQuery.isEmpty() || home.toLowerCase().contains(searchQuery))
            .collect(Collectors.toList());

        filteredHomes.sort((a, b) -> {
            boolean aFav = ModConfig.getInstance().isFavorite(a);
            boolean bFav = ModConfig.getInstance().isFavorite(b);
            if (aFav && !bFav) return -1;
            if (!aFav && bFav) return 1;
            return a.compareToIgnoreCase(b);
        });

        int panelWidth = Math.min(520, this.width - 40);
        int panelX = (this.width - panelWidth) / 2;
        int contentX = panelX + 20;
        int contentWidth = panelWidth - 40;

        int cols = compactMode ? COLUMNS_COMPACT : COLUMNS;
        int cardH = compactMode ? CARD_HEIGHT_COMPACT : CARD_HEIGHT;
        int cardW = compactMode ? contentWidth : (contentWidth - (cols - 1) * CARD_SPACING) / cols;

        // ═══════════════════════════════════════════
        // CORRECTION: Position plus basse pour éviter que les homes soient coupés
        // ═══════════════════════════════════════════
        int startY = 115; // Augmenté de 85 à 95

        for (int i = 0; i < filteredHomes.size(); i++) {
            String homeName = filteredHomes.get(i);

            int col = i % cols;
            int row = i / cols;

            int x = contentX + col * (cardW + CARD_SPACING);
            int y = startY + row * (cardH + CARD_SPACING);

            homeButtons.add(new HomeButton(x, y, cardW, cardH, homeName, i));
        }

        int rows = (int) Math.ceil(filteredHomes.size() / (float) cols);
        int contentHeight = rows * (cardH + CARD_SPACING);
        int visibleHeight = this.height - 210;
        maxScroll = Math.max(0, contentHeight - visibleHeight);

        scrollOffset = MathHelper.clamp(scrollOffset, 0, Math.max(0, maxScroll));
    }

    // ═══════════════════════════════════════════
    // DÉSACTIVER LE BLUR
    // ═══════════════════════════════════════════
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // VIDE = PAS DE BLUR
    }

    // ═══════════════════════════════════════════
    // RENDU PRINCIPAL
    // ═══════════════════════════════════════════
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float timeSinceOpen = (System.currentTimeMillis() - openTime) / 1000f;
        animationProgress = Math.min(1f, timeSinceOpen * 3f);
        float ease = easeOutBack(animationProgress);

        // Fond
        context.fill(0, 0, this.width, this.height, 0xDD000000);
        drawGradientBackground(context);
        renderParticles(context);

        // Panneau
        int panelWidth = Math.min(520, this.width - 40);
        int panelHeight = this.height - 50;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = 25;

        int animatedPanelY = (int) (panelY + (1 - ease) * 50);
        int alpha = (int) (ease * 255);

        drawRoundedRect(context, panelX + 5, animatedPanelY + 5, panelWidth, panelHeight, 16, 0x80000000);
        drawRoundedRect(context, panelX, animatedPanelY, panelWidth, panelHeight, 16, withAlpha(currentTheme.background, alpha));
        drawRoundedBorder(context, panelX, animatedPanelY, panelWidth, panelHeight, 16, withAlpha(currentTheme.primary, alpha / 3));

        // Header
        renderHeader(context, panelX, animatedPanelY, panelWidth);

        // Barre de recherche
        searchField.setY(animatedPanelY + 35);

        // Boutons d'options
        renderOptionButtons(context, panelX, animatedPanelY, panelWidth, mouseX, mouseY);

        // Contenu - AJUSTÉ
        int contentY = animatedPanelY + 90;
        int contentHeight = panelHeight - 155;

        context.enableScissor(panelX + 10, contentY, panelX + panelWidth - 10, contentY + contentHeight);
        renderHomes(context, mouseX, mouseY, contentY, contentHeight);
        context.disableScissor();

        if (maxScroll > 0) {
            renderScrollBar(context, panelX + panelWidth - 14, contentY, 8, contentHeight);
        }

        renderFooter(context, panelX, animatedPanelY, panelWidth, panelHeight, mouseX, mouseY);
        renderNotification(context);

        if (hoveredButton != null) {
            renderTooltip(context, mouseX, mouseY, hoveredButton);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawGradientBackground(DrawContext context) {
        for (int y = 0; y < this.height; y++) {
            float progress = (float) y / this.height;
            int r = (int) (16 * (1 - progress) + 26 * progress);
            int g = (int) (10 * (1 - progress) + 15 * progress);
            int b = (int) (32 * (1 - progress) + 46 * progress);
            int color = 0xCC000000 | (r << 16) | (g << 8) | b;
            context.fill(0, y, this.width, y + 1, color);
        }
    }

    private void renderHeader(DrawContext context, int panelX, int panelY, int panelWidth) {
        int centerX = panelX + panelWidth / 2;
        float bounce = (float) Math.sin(System.currentTimeMillis() / 400.0) * 2;

        context.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal("§l# " + lang.get("title.homes") + " #"),
            centerX,
            panelY + 12 + (int) bounce,
            currentTheme.primary
        );
    }

    private void renderOptionButtons(DrawContext context, int panelX, int panelY, int panelWidth, int mouseX, int mouseY) {
        int btnY = panelY + 35;
        int btnSize = 18;

        // Bouton Thème
        int themeX = panelX + 15;
        boolean themeHovered = isInBounds(mouseX, mouseY, themeX, btnY, btnSize, btnSize);
        drawRoundedRect(context, themeX, btnY, btnSize, btnSize, 4,
            themeHovered ? currentTheme.primary : withAlpha(currentTheme.primary, 150));
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("T"), themeX + btnSize/2, btnY + 5, 0xFFFFFFFF);

        // Bouton Langue
        int langX = themeX + btnSize + 5;
        boolean langHovered = isInBounds(mouseX, mouseY, langX, btnY, btnSize, btnSize);
        drawRoundedRect(context, langX, btnY, btnSize, btnSize, 4,
            langHovered ? 0xFF4CAF50 : withAlpha(0xFF4CAF50, 150));
        String langIcon = lang.getCurrentLang() == LangManager.Language.FRENCH ? "FR" : "EN";
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(langIcon), langX + btnSize/2, btnY + 5, 0xFFFFFFFF);

        // Bouton Mode Compact
        int compactX = panelX + panelWidth - 15 - btnSize;
        boolean compactHovered = isInBounds(mouseX, mouseY, compactX, btnY, btnSize, btnSize);
        drawRoundedRect(context, compactX, btnY, btnSize, btnSize, 4,
            compactHovered ? currentTheme.secondary : withAlpha(currentTheme.secondary, 150));
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(compactMode ? "=" : "#"), compactX + btnSize/2, btnY + 5, 0xFFFFFFFF);

        // Bouton Stats
        int statsX = compactX - btnSize - 5;
        boolean statsHovered = isInBounds(mouseX, mouseY, statsX, btnY, btnSize, btnSize);
        drawRoundedRect(context, statsX, btnY, btnSize, btnSize, 4,
            statsHovered ? currentTheme.accent : withAlpha(currentTheme.accent, 150));
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("S"), statsX + btnSize/2, btnY + 5, 0xFFFFFFFF);
    }

    private void renderHomes(DrawContext context, int mouseX, int mouseY, int contentY, int contentHeight) {
        List<String> homes = HomesManager.getInstance().getHomes();

        if (homes.isEmpty() && !HomesManager.getInstance().isWaiting()) {
            renderEmptyState(context, contentY, contentHeight);
            return;
        }

        if (HomesManager.getInstance().isWaiting() && homeButtons.isEmpty()) {
            renderLoadingState(context, contentY, contentHeight);
            return;
        }

        if (homeButtons.isEmpty() && !searchQuery.isEmpty()) {
            renderNoResults(context, contentY, contentHeight);
            return;
        }

        hoveredButton = null;

        for (int i = 0; i < homeButtons.size(); i++) {
            HomeButton btn = homeButtons.get(i);
            int drawY = btn.y - scrollOffset;

            float cardDelay = i * 0.03f;
            float cardEase = easeOutBack(Math.max(0, Math.min(1, (animationProgress - cardDelay) * 2.5f)));
            if (cardEase <= 0) continue;

            int animatedY = drawY + (int) ((1 - cardEase) * 20);
            int cardAlpha = (int) (cardEase * 255);

            boolean hovered = isInBounds(mouseX, mouseY, btn.x, animatedY, btn.width, btn.height)
                             && mouseY >= contentY && mouseY <= contentY + contentHeight;

            boolean isFavorite = ModConfig.getInstance().isFavorite(btn.homeName);
            int useCount = ModConfig.getInstance().getUseCount(btn.homeName);

            renderHomeCard(context, btn.x, animatedY, btn.width, btn.height,
                          btn.homeName, btn.index, hovered, cardAlpha, isFavorite, useCount);

            if (hovered) {
                hoveredButton = new HomeButton(btn.x, animatedY, btn.width, btn.height, btn.homeName, btn.index);
            }
        }
    }

    private void renderHomeCard(DrawContext context, int x, int y, int width, int height,
                                String homeName, int index, boolean hovered, int alpha,
                                boolean isFavorite, int useCount) {

        int bgColor = hovered ? lightenColor(currentTheme.background, 0.4f) : lightenColor(currentTheme.background, 0.2f);
        int borderColor = isFavorite ? 0xFFFFD700 : (hovered ? currentTheme.primary : withAlpha(currentTheme.primary, 120));

        int drawY = hovered ? y - 2 : y;

        if (hovered) {
            drawRoundedRect(context, x + 2, drawY + 3, width, height, 8, withAlpha(currentTheme.primary, 60));
        }

        drawRoundedRect(context, x, drawY, width, height, 8, withAlpha(bgColor, alpha));
        drawRoundedBorder(context, x, drawY, width, height, 8, withAlpha(borderColor, alpha));

        if (isFavorite) {
            context.drawTextWithShadow(textRenderer, Text.literal("*"), x + width - 12, drawY + 4, 0xFFFFD700);
        }

        if (compactMode) {
            String displayName = homeName.length() > 25 ? homeName.substring(0, 23) + ".." : homeName;
            context.drawTextWithShadow(textRenderer, Text.literal(displayName), x + 10, drawY + height/2 - 4, withAlpha(0xFFFFFFFF, alpha));
            if (hovered) {
                context.drawTextWithShadow(textRenderer, Text.literal(">"), x + width - 20, drawY + height/2 - 4, 0xFFFFD700);
            }
        } else {
            String displayName = homeName.length() > 12 ? homeName.substring(0, 10) + ".." : homeName;
            context.drawTextWithShadow(textRenderer, Text.literal("§l" + displayName), x + 12, drawY + 10, withAlpha(0xFFFFFFFF, alpha));

            String visitText = useCount > 0 ? useCount + " " + (useCount > 1 ? lang.get("stats.visits_plural") : lang.get("stats.visits")) : lang.get("message.click_to_tp");
            context.drawTextWithShadow(textRenderer, Text.literal("§7" + visitText), x + 12, drawY + 24, withAlpha(0xFF888888, alpha));

            if (index < 9) {
                context.drawTextWithShadow(textRenderer, Text.literal("§8[" + (index + 1) + "]"), x + width - 20, drawY + height - 12, withAlpha(0xFF666666, alpha));
            }

            if (hovered) {
                context.drawTextWithShadow(textRenderer, Text.literal("§e>"), x + width - 14, drawY + height/2 - 4, 0xFFFFD700);
            }
        }
    }

    private void renderEmptyState(DrawContext context, int contentY, int contentHeight) {
        int centerX = this.width / 2;
        int centerY = contentY + contentHeight / 2;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("§7" + lang.get("message.no_homes")), centerX, centerY - 5, 0xFF888888);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("§8" + lang.get("message.sethome")), centerX, centerY + 10, 0xFF666666);
    }

    private void renderLoadingState(DrawContext context, int contentY, int contentHeight) {
        int centerX = this.width / 2;
        int centerY = contentY + contentHeight / 2;
        String dots = ".".repeat((int) ((System.currentTimeMillis() / 300) % 4));
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("§e" + lang.get("message.loading") + dots), centerX, centerY, currentTheme.accent);
    }

    private void renderNoResults(DrawContext context, int contentY, int contentHeight) {
        int centerX = this.width / 2;
        int centerY = contentY + contentHeight / 2;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("§7" + lang.get("message.no_results") + " \"" + searchQuery + "\""), centerX, centerY, 0xFF888888);
    }

    private void renderScrollBar(DrawContext context, int x, int y, int width, int height) {
        drawRoundedRect(context, x, y, width, height, 4, withAlpha(0xFFFFFFFF, 40));
        float scrollProgress = maxScroll > 0 ? (float) scrollOffset / maxScroll : 0;
        int cursorHeight = Math.max(30, height * height / (height + maxScroll));
        int cursorY = y + (int) ((height - cursorHeight) * scrollProgress);
        drawRoundedRect(context, x, cursorY, width, cursorHeight, 4, currentTheme.primary);
    }

    private void renderFooter(DrawContext context, int panelX, int panelY, int panelWidth, int panelHeight, int mouseX, int mouseY) {
        int footerY = panelY + panelHeight - 55;
        int centerX = panelX + panelWidth / 2;

        int buttonWidth = 85;
        int buttonHeight = 24;
        int buttonSpacing = 8;

        int refreshX = centerX - buttonWidth - buttonSpacing - buttonWidth/2;
        boolean refreshHovered = isInBounds(mouseX, mouseY, refreshX, footerY, buttonWidth, buttonHeight);
        renderButton(context, refreshX, footerY, buttonWidth, buttonHeight, lang.get("button.refresh"), refreshHovered, currentTheme.accent);

        int historyX = centerX - buttonWidth/2;
        boolean historyHovered = isInBounds(mouseX, mouseY, historyX, footerY, buttonWidth, buttonHeight);
        renderButton(context, historyX, footerY, buttonWidth, buttonHeight, lang.get("button.recent"), historyHovered, currentTheme.secondary);

        int closeX = centerX + buttonWidth/2 + buttonSpacing;
        boolean closeHovered = isInBounds(mouseX, mouseY, closeX, footerY, buttonWidth, buttonHeight);
        renderButton(context, closeX, footerY, buttonWidth, buttonHeight, lang.get("button.close"), closeHovered, 0xFFFF6B6B);

        int totalHomes = HomesManager.getInstance().getHomes().size();
        int totalFavorites = (int) HomesManager.getInstance().getHomes().stream()
            .filter(h -> ModConfig.getInstance().isFavorite(h)).count();
        int totalTeleports = ModConfig.getInstance().getTotalTeleports();

        String statsText = totalHomes + " homes | " + totalFavorites + " fav | " + totalTeleports + " TP";
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("§8" + statsText), centerX, footerY + 30, 0xFF666666);

        context.drawCenteredTextWithShadow(textRenderer, Text.literal("§8" + lang.get("credits") + " §dmaxlananas"), centerX, panelY + panelHeight - 12, 0xFF555555);
    }

    private void renderButton(DrawContext context, int x, int y, int width, int height, String text, boolean hovered, int accentColor) {
        int bgColor = hovered ? accentColor : withAlpha(accentColor, 80);
        int drawY = hovered ? y - 1 : y;

        if (hovered) {
            drawRoundedRect(context, x + 1, drawY + 2, width, height, 6, withAlpha(accentColor, 80));
        }

        drawRoundedRect(context, x, drawY, width, height, 6, bgColor);
        drawRoundedBorder(context, x, drawY, width, height, 6, withAlpha(accentColor, hovered ? 255 : 180));
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(text), x + width/2, drawY + height/2 - 4, 0xFFFFFFFF);
    }

    private void renderNotification(DrawContext context) {
        if (System.currentTimeMillis() - notificationTime < 3000) {
            float progress = (System.currentTimeMillis() - notificationTime) / 3000f;
            int alpha = (int) ((1 - progress) * 255);

            int notifWidth = textRenderer.getWidth(notificationText) + 30;
            int notifX = (this.width - notifWidth) / 2;
            int notifY = this.height - 60;

            float slideProgress = Math.min(1, (System.currentTimeMillis() - notificationTime) / 200f);
            int animatedY = notifY + (int) ((1 - easeOutBack(slideProgress)) * 20);

            drawRoundedRect(context, notifX, animatedY, notifWidth, 24, 8, withAlpha(0xFF1A1A2E, alpha));
            drawRoundedBorder(context, notifX, animatedY, notifWidth, 24, 8, withAlpha(notificationColor, alpha));
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(notificationText), this.width / 2, animatedY + 8, withAlpha(notificationColor, alpha));
        }
    }

    private void renderTooltip(DrawContext context, int mouseX, int mouseY, HomeButton btn) {
        boolean isFavorite = ModConfig.getInstance().isFavorite(btn.homeName);
        int useCount = ModConfig.getInstance().getUseCount(btn.homeName);

        List<String> lines = new ArrayList<>();
        lines.add("§f§l" + btn.homeName);
        lines.add("§7" + lang.get("stats.visits") + ": §e" + useCount);
        lines.add(isFavorite ? "§e" + lang.get("favorite.is_favorite") : "§8" + lang.get("favorite.right_click"));

        int tooltipWidth = 130;
        int tooltipHeight = lines.size() * 12 + 10;

        int tooltipX = Math.min(mouseX + 10, this.width - tooltipWidth - 5);
        int tooltipY = Math.max(mouseY - tooltipHeight - 5, 5);

        drawRoundedRect(context, tooltipX, tooltipY, tooltipWidth, tooltipHeight, 6, 0xF0101020);
        drawRoundedBorder(context, tooltipX, tooltipY, tooltipWidth, tooltipHeight, 6, currentTheme.primary);

        for (int i = 0; i < lines.size(); i++) {
            context.drawTextWithShadow(textRenderer, Text.literal(lines.get(i)), tooltipX + 8, tooltipY + 6 + i * 12, 0xFFFFFFFF);
        }
    }

    private void renderParticles(DrawContext context) {
        long time = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            float phase = i * 0.628f;
            float x = this.width * 0.05f + (float) Math.sin(time / 4000.0 + phase) * 40 + i * (this.width * 0.09f);
            float y = this.height * 0.2f + (float) Math.cos(time / 3000.0 + phase) * 50;
            int alpha = (int) (25 + Math.sin(time / 800.0 + phase) * 15);
            context.fill((int) x, (int) y, (int) x + 3, (int) y + 3, withAlpha(currentTheme.primary, alpha));
        }
    }

    // ═══════════════════════════════════════════
    // UTILITAIRES
    // ═══════════════════════════════════════════

    private void drawRoundedRect(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        context.fill(x + radius, y, x + width - radius, y + height, color);
        context.fill(x, y + radius, x + width, y + height - radius, color);
        fillCircle(context, x + radius, y + radius, radius, color);
        fillCircle(context, x + width - radius, y + radius, radius, color);
        fillCircle(context, x + radius, y + height - radius, radius, color);
        fillCircle(context, x + width - radius, y + height - radius, radius, color);
    }

    private void drawRoundedBorder(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        context.fill(x + radius, y, x + width - radius, y + 1, color);
        context.fill(x + radius, y + height - 1, x + width - radius, y + height, color);
        context.fill(x, y + radius, x + 1, y + height - radius, color);
        context.fill(x + width - 1, y + radius, x + width, y + height - radius, color);
    }

    private void fillCircle(DrawContext context, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dy * dy <= radius * radius) {
                    context.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
                }
            }
        }
    }

    private int withAlpha(int color, int alpha) {
        return (MathHelper.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    private int lightenColor(int color, float amount) {
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) + 255 * amount));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) + 255 * amount));
        int b = Math.min(255, (int) ((color & 0xFF) + 255 * amount));
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return (float) (1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2));
    }

    private boolean isInBounds(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void showNotification(String text, int color) {
        this.notificationText = text;
        this.notificationColor = color;
        this.notificationTime = System.currentTimeMillis();
    }

    private void playClickSound() {
        MinecraftClient.getInstance().getSoundManager().play(
            PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f)
        );
    }

    private void playSuccessSound() {
        MinecraftClient.getInstance().getSoundManager().play(
            PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2f)
        );
    }

    // ═══════════════════════════════════════════
    // ÉVÉNEMENTS
    // ═══════════════════════════════════════════

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;

        int panelWidth = Math.min(520, this.width - 40);
        int panelX = (this.width - panelWidth) / 2;
        int panelHeight = this.height - 50;
        int panelY = 25;

        int btnY = panelY + 35;
        int btnSize = 18;

        // Thème
        if (isInBounds(mx, my, panelX + 15, btnY, btnSize, btnSize)) {
            playClickSound();
            themeIndex = (themeIndex + 1) % Theme.values().length;
            currentTheme = Theme.values()[themeIndex];
            ModConfig.getInstance().setThemeIndex(themeIndex);
            showNotification(lang.get("theme.label") + ": " + currentTheme.name, currentTheme.primary);
            return true;
        }

        // Langue
        int langX = panelX + 15 + btnSize + 5;
        if (isInBounds(mx, my, langX, btnY, btnSize, btnSize)) {
            playClickSound();
            lang.toggleLanguage();
            showNotification(lang.get("language.changed"), 0xFF4CAF50);
            return true;
        }

        // Compact
        int compactX = panelX + panelWidth - 15 - btnSize;
        if (isInBounds(mx, my, compactX, btnY, btnSize, btnSize)) {
            playClickSound();
            compactMode = !compactMode;
            ModConfig.getInstance().setCompactMode(compactMode);
            rebuildButtons();
            showNotification(compactMode ? lang.get("mode.compact") : lang.get("mode.grid"), currentTheme.secondary);
            return true;
        }

        // Stats
        int statsX = compactX - btnSize - 5;
        if (isInBounds(mx, my, statsX, btnY, btnSize, btnSize)) {
            playClickSound();
            MinecraftClient.getInstance().setScreen(new StatsScreen(this, currentTheme, lang));
            return true;
        }

        // Home click
        if (hoveredButton != null) {
            if (button == 0) {
                playSuccessSound();
                ModConfig.getInstance().incrementUseCount(hoveredButton.homeName);
                ModConfig.getInstance().addToHistory(hoveredButton.homeName);
                HomesManager.getInstance().teleportToHome(hoveredButton.homeName);
                showNotification(lang.get("message.tp_to") + " " + hoveredButton.homeName, currentTheme.accent);
                return true;
            } else if (button == 1) {
                playClickSound();
                boolean nowFav = ModConfig.getInstance().toggleFavorite(hoveredButton.homeName);
                rebuildButtons();
                showNotification(nowFav ? lang.get("favorite.added") : lang.get("favorite.removed"), nowFav ? 0xFFFFD700 : 0xFF888888);
                return true;
            }
        }

        // Footer buttons
        int footerY = panelY + panelHeight - 55;
        int centerX = this.width / 2;
        int buttonWidth = 85;
        int buttonSpacing = 8;

        int refreshX = centerX - buttonWidth - buttonSpacing - buttonWidth/2;
        if (isInBounds(mx, my, refreshX, footerY, buttonWidth, 24)) {
            playClickSound();
            HomesManager.getInstance().requestHomes();
            showNotification(lang.get("message.refreshing"), currentTheme.accent);
            return true;
        }

        int historyX = centerX - buttonWidth/2;
        if (isInBounds(mx, my, historyX, footerY, buttonWidth, 24)) {
            playClickSound();
            MinecraftClient.getInstance().setScreen(new HistoryScreen(this, currentTheme, lang));
            return true;
        }

        int closeX = centerX + buttonWidth/2 + buttonSpacing;
        if (isInBounds(mx, my, closeX, footerY, buttonWidth, 24)) {
            playClickSound();
            this.close();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset = MathHelper.clamp(scrollOffset - (int) (verticalAmount * 25), 0, maxScroll);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode >= 49 && keyCode <= 57) {
            int index = keyCode - 49;
            if (index < homeButtons.size()) {
                HomeButton btn = homeButtons.get(index);
                playSuccessSound();
                ModConfig.getInstance().incrementUseCount(btn.homeName);
                HomesManager.getInstance().teleportToHome(btn.homeName);
                return true;
            }
        }

        if (keyCode == 256) {
            this.close();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static class HomeButton {
        int x, y, width, height;
        String homeName;
        int index;

        HomeButton(int x, int y, int width, int height, String homeName, int index) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.homeName = homeName;
            this.index = index;
        }
    }
}