package com.example.homegui.screen;

import com.example.homegui.HomesManager;
import com.example.homegui.config.LangManager;
import com.example.homegui.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.*;

public class StatsScreen extends Screen {
    
    private final Screen parent;
    private final HomesScreen.Theme theme;
    private final LangManager lang;
    private final long openTime;
    
    public StatsScreen(Screen parent, HomesScreen.Theme theme, LangManager lang) {
        super(Text.literal("Stats"));
        this.parent = parent;
        this.theme = theme;
        this.lang = lang;
        this.openTime = System.currentTimeMillis();
    }
    
    // ═══════════════════════════════════════════
    // DÉSACTIVER LE BLUR
    // ═══════════════════════════════════════════
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // VIDE = PAS DE BLUR
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float timeSinceOpen = (System.currentTimeMillis() - openTime) / 1000f;
        float ease = easeOutBack(Math.min(1f, timeSinceOpen * 3f));
        
        // Fond
        context.fill(0, 0, this.width, this.height, 0xDD000000);
        drawGradientBackground(context);
        renderParticles(context);
        
        // Panneau
        int panelWidth = Math.min(400, this.width - 60);
        int panelHeight = this.height - 80;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = 40;
        
        int animatedPanelY = (int) (panelY + (1 - ease) * 30);
        int alpha = (int) (ease * 255);
        
        drawRoundedRect(context, panelX + 4, animatedPanelY + 4, panelWidth, panelHeight, 16, 0x80000000);
        drawRoundedRect(context, panelX, animatedPanelY, panelWidth, panelHeight, 16, withAlpha(theme.background, alpha));
        drawRoundedBorder(context, panelX, animatedPanelY, panelWidth, panelHeight, 16, withAlpha(theme.primary, alpha / 3));
        
        int centerX = panelX + panelWidth / 2;
        int y = animatedPanelY + 20;
        
        // Titre
        float bounce = (float) Math.sin(System.currentTimeMillis() / 400.0) * 2;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("§l# " + lang.get("title.stats") + " #"), centerX, y + (int) bounce, theme.primary);
        y += 35;
        
        // Stats globales
        int totalHomes = HomesManager.getInstance().getHomes().size();
        int totalFavorites = (int) HomesManager.getInstance().getHomes().stream()
            .filter(h -> ModConfig.getInstance().isFavorite(h)).count();
        int totalTP = ModConfig.getInstance().getTotalTeleports();
        
        // Cartes de stats
        int cardWidth = panelWidth - 40;
        int cardHeight = 45;
        int cardX = panelX + 20;
        
        // Homes
        drawStatCard(context, cardX, y, cardWidth, cardHeight, "Homes", String.valueOf(totalHomes), theme.primary, alpha);
        y += cardHeight + 10;
        
        // Favoris
        drawStatCard(context, cardX, y, cardWidth, cardHeight, lang.get("stats.favorites"), String.valueOf(totalFavorites), 0xFFFFD700, alpha);
        y += cardHeight + 10;
        
        // Téléportations
        drawStatCard(context, cardX, y, cardWidth, cardHeight, lang.get("stats.total_tp"), String.valueOf(totalTP), theme.accent, alpha);
        y += cardHeight + 25;
        
        // Top 5
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("§l" + lang.get("stats.top_homes")), centerX, y, theme.secondary);
        y += 20;
        
        List<Map.Entry<String, Integer>> sortedHomes = new ArrayList<>(ModConfig.getInstance().getAllUseCounts().entrySet());
        sortedHomes.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        String[] medals = {"§6#1", "§7#2", "§c#3", "§8#4", "§8#5"};
        int[] medalColors = {0xFFFFD700, 0xFFC0C0C0, 0xFFCD7F32, 0xFF888888, 0xFF888888};
        
        for (int i = 0; i < Math.min(5, sortedHomes.size()); i++) {
            Map.Entry<String, Integer> entry = sortedHomes.get(i);
            String homeName = entry.getKey();
            int count = entry.getValue();
            
            if (homeName.length() > 15) homeName = homeName.substring(0, 13) + "..";
            
            String visitText = count + " " + (count > 1 ? lang.get("stats.visits_plural") : lang.get("stats.visits"));
            
            drawRoundedRect(context, cardX, y, cardWidth, 22, 6, withAlpha(lightenColor(theme.background, 0.15f), alpha));
            
            context.drawTextWithShadow(textRenderer, Text.literal(medals[i]), cardX + 10, y + 7, medalColors[i]);
            context.drawTextWithShadow(textRenderer, Text.literal("§f" + homeName), cardX + 35, y + 7, withAlpha(0xFFFFFFFF, alpha));
            context.drawTextWithShadow(textRenderer, Text.literal("§7" + visitText), cardX + cardWidth - textRenderer.getWidth(visitText) - 10, y + 7, withAlpha(0xFF888888, alpha));
            
            y += 26;
        }
        
        if (sortedHomes.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("§7No data yet"), centerX, y + 10, 0xFF888888);
        }
        
        // Bouton retour
        int btnWidth = 100;
        int btnHeight = 26;
        int btnX = centerX - btnWidth / 2;
        int btnY = animatedPanelY + panelHeight - 45;
        
        boolean btnHovered = mouseX >= btnX && mouseX <= btnX + btnWidth && mouseY >= btnY && mouseY <= btnY + btnHeight;
        
        drawRoundedRect(context, btnX, btnHovered ? btnY - 1 : btnY, btnWidth, btnHeight, 8, btnHovered ? theme.accent : withAlpha(theme.accent, 150));
        drawRoundedBorder(context, btnX, btnHovered ? btnY - 1 : btnY, btnWidth, btnHeight, 8, theme.accent);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(lang.get("button.back")), centerX, (btnHovered ? btnY - 1 : btnY) + btnHeight/2 - 4, 0xFFFFFFFF);
        
        // Crédit
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("§8" + lang.get("credits") + " §dmaxlananas"), centerX, animatedPanelY + panelHeight - 15, 0xFF555555);
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void drawStatCard(DrawContext context, int x, int y, int width, int height, String label, String value, int color, int alpha) {
        drawRoundedRect(context, x, y, width, height, 10, withAlpha(lightenColor(theme.background, 0.2f), alpha));
        drawRoundedBorder(context, x, y, width, height, 10, withAlpha(color, alpha / 2));
        
        context.drawTextWithShadow(textRenderer, Text.literal("§7" + label), x + 15, y + 10, withAlpha(0xFFAAAAAA, alpha));
        context.drawTextWithShadow(textRenderer, Text.literal("§l" + value), x + 15, y + 24, withAlpha(color, alpha));
        
        // Icône
        context.drawTextWithShadow(textRenderer, Text.literal(">>"), x + width - 25, y + height/2 - 4, withAlpha(color, alpha / 2));
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
    
    private void renderParticles(DrawContext context) {
        long time = System.currentTimeMillis();
        for (int i = 0; i < 8; i++) {
            float phase = i * 0.785f;
            float x = this.width * 0.1f + (float) Math.sin(time / 4000.0 + phase) * 30 + i * (this.width * 0.1f);
            float y = this.height * 0.3f + (float) Math.cos(time / 3000.0 + phase) * 40;
            int alpha = (int) (20 + Math.sin(time / 800.0 + phase) * 10);
            context.fill((int) x, (int) y, (int) x + 3, (int) y + 3, withAlpha(theme.primary, alpha));
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelWidth = Math.min(400, this.width - 60);
        int panelHeight = this.height - 80;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = 40;
        int centerX = panelX + panelWidth / 2;
        
        int btnWidth = 100;
        int btnHeight = 26;
        int btnX = centerX - btnWidth / 2;
        int btnY = panelY + panelHeight - 45;
        
        if (mouseX >= btnX && mouseX <= btnX + btnWidth && mouseY >= btnY && mouseY <= btnY + btnHeight) {
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            MinecraftClient.getInstance().setScreen(parent);
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            MinecraftClient.getInstance().setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    // Utilitaires
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
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}