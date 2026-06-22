@Override
public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (super.mouseClicked(mouseX, mouseY, button)) return true;

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
        MinecraftClient.getInstance().setScreen(parent);
        return true;
    }

    return false;
}

@Override
public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == 256) {
        MinecraftClient.getInstance().setScreen(parent);
        return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
}
