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

import java.util.List;

public class HistoryScreen extends Screen {
    
    private final Screen parent;
    private final HomesScreen.Theme theme;
    private final LangManager lang;
    private final long openTime;
    private int hoveredIndex = -1;
    
    public HistoryScreen(Screen parent, HomesScreen.Theme theme, LangManager lang) {
        super(Text.literal("History"));
        this.parent = parent;
        this.theme = theme;
        this.lang = lang;
        this.openTime = System.currentTimeMillis();
    }
    
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
        drawRoundedBorder(context, panelX, animatedPanelY, panelWidth, panelHeight, 16, withAlpha(theme.secondary, alpha / 3));
        
        int centerX = panelX + panelWidth / 2;
        int y = animatedPanelY + 20;
        
        // Titre
        float bounce = (float) Math.sin(System.currentTimeMillis() / 400.0) * 2;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("§l# " + lang.get("title.history") + " #"), centerX, y + (int) bounce, theme.secondary);
        y += 40;
        
        // Liste de l'historique
        List<ModConfig.HistoryEntry> history = ModConfig.getInstance().getHistory();
        int cardWidth = panelWidth - 40;
        int cardHeight = 32;
        int cardX = panelX + 20;
        
        hoveredIndex = -1;
        
        if (history.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("§7" + lang.get("message.no_history")), centerX, y + 40, 0xFF888888);
        } else {
            for (int i = 0; i < Math.min(10, history.size()); i++) {
                ModConfig.HistoryEntry entry = history.get(i);
                
                float cardDelay = i * 0.05f;
                float cardEase = easeOutBack(Math.max(0, Math.min(1, (timeSinceOpen * 3 - cardDelay) * 2f)));
                
                int cardY = y + (int) ((1 - cardEase) * 15);
                int cardAlpha = (int) (cardEase * 255);
                
                boolean hovered = mouseX >= cardX && mouseX <= cardX + cardWidth && 
                                 mouseY >= cardY && mouseY <= cardY + cardHeight;
                
                if (hovered) hoveredIndex = i;
                
                int bgColor = hovered ? lightenColor(theme.background, 0.35f) : lightenColor(theme.background, 0.15f);
                int borderColor = hovered ? theme.secondary : withAlpha(theme.secondary, 80);
                
                int drawCardY = hovered ? cardY - 2 : cardY;
                
                if (hovered) {
                    drawRoundedRect(context, cardX + 2, drawCardY + 3, cardWidth, cardHeight, 8, withAlpha(theme.secondary, 40));
                }
                
                drawRoundedRect(context, cardX, drawCardY, cardWidth, cardHeight, 8, withAlpha(bgColor, cardAlpha));
                drawRoundedBorder(context, cardX, drawCardY, cardWidth, cardHeight, 8, withAlpha(borderColor, cardAlpha));
                
                // Numéro
                context.drawTextWithShadow(textRenderer, Text.literal("§7" + (i + 1) + "."), cardX + 10, drawCardY + cardHeight/2 - 4, withAlpha(0xFF888888, cardAlpha));
                
                // Nom du home
                String displayName = entry.homeName.length() > 18 ? entry.homeName.substring(0, 16) + ".." : entry.homeName;
                context.drawTextWithShadow(textRenderer, Text.literal("§f" + displayName), cardX + 30, drawCardY + cardHeight/2 - 4, withAlpha(0xFFFFFFFF, cardAlpha));
                
                // Temps
                String timeAgo = entry.getTimeAgo() + " " + lang.get("history.ago");
                context.drawTextWithShadow(textRenderer, Text.literal("§8" + timeAgo), cardX + cardWidth - textRenderer.getWidth(timeAgo) - 35, drawCardY + cardHeight/2 - 4, withAlpha(0xFF666666, cardAlpha));
                
                // Flèche si hover
                if (hovered) {
                    float arrowBounce = (float) Math.sin(System.currentTimeMillis() / 150.0) * 2;
                    context.drawTextWithShadow(textRenderer, Text.literal("§e>"), cardX + cardWidth - 15 + (int) arrowBounce, drawCardY + cardHeight/2 - 4, 0xFFFFD700);
                }
                
                y += cardHeight + 6;
            }
        }
        
        // Boutons
        int btnWidth = 90;
        int btnHeight = 26;
        int btnSpacing = 15;
        int btnY = animatedPanelY + panelHeight - 50;
        
        // Bouton Clear
        int clearX = centerX - btnWidth - btnSpacing/2;
        boolean clearHovered = mouseX >= clearX && mouseX <= clearX + btnWidth && mouseY >= btnY && mouseY <= btnY + btnHeight;
        drawRoundedRect(context, clearX, clearHovered ? btnY - 1 : btnY, btnWidth, btnHeight, 8, clearHovered ? 0xFFFF6B6B : withAlpha(0xFFFF6B6B, 150));
        drawRoundedBorder(context, clearX, clearHovered ? btnY - 1 : btnY, btnWidth, btnHeight, 8, 0xFFFF6B6B);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(lang.get("button.clear")), clearX + btnWidth/2, (clearHovered ? btnY - 1 : btnY) + btnHeight/2 - 4, 0xFFFFFFFF);
        
        // Bouton Back
        int backX = centerX + btnSpacing/2;
        boolean backHovered = mouseX >= backX && mouseX <= backX + btnWidth && mouseY >= btnY && mouseY <= btnY + btnHeight;
        drawRoundedRect(context, backX, backHovered ? btnY - 1 : btnY, btnWidth, btnHeight, 8, backHovered ? theme.accent : withAlpha(theme.accent, 150));
        drawRoundedBorder(context, backX, backHovered ? btnY - 1 : btnY, btnWidth, btnHeight, 8, theme.accent);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(lang.get("button.back")), backX + btnWidth/2, (backHovered ? btnY - 1 : btnY) + btnHeight/2 - 4, 0xFFFFFFFF);
        
        // Crédit
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("§8" + lang.get("credits") + " §dmaxlananas"), centerX, animatedPanelY + panelHeight - 15, 0xFF555555);
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelWidth = Math.min(400, this.width - 60);
        int panelHeight = this.height - 80;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = 40;
        int centerX = panelX + panelWidth / 2;
        
        int btnWidth = 90;
        int btnHeight = 26;
        int btnSpacing = 15;
        int btnY = panelY + panelHeight - 50;
        
        // Clear
        int clearX = centerX - btnWidth - btnSpacing/2;
        if (mouseX >= clearX && mouseX <= clearX + btnWidth && mouseY >= btnY && mouseY <= btnY + btnHeight) {
            playSound();
            ModConfig.getInstance().clearHistory();
            return true;
        }
        
        // Back
        int backX = centerX + btnSpacing/2;
        if (mouseX >= backX && mouseX <= backX + btnWidth && mouseY >= btnY && mouseY <= btnY + btnHeight) {
            playSound();
            MinecraftClient.getInstance().setScreen(parent);
            return true;
        }
        
        // Clic sur un home de l'historique
        if (hoveredIndex >= 0) {
            List<ModConfig.HistoryEntry> history = ModConfig.getInstance().getHistory();
            if (hoveredIndex < history.size()) {
                playSuccessSound();
                String homeName = history.get(hoveredIndex).homeName;
                ModConfig.getInstance().incrementUseCount(homeName);
                HomesManager.getInstance().teleportToHome(homeName);
                return true;
            }
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
    
    private void playSound() {
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }
    
    private void playSuccessSound() {
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2f));
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
            context.fill((int) x, (int) y, (int) x + 3, (int) y + 3, withAlpha(theme.secondary, alpha));
        }
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