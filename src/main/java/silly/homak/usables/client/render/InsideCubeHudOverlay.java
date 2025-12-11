package silly.homak.usables.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import silly.homak.usables.UsablesMain;
import silly.homak.usables.common.barrier.CollisionBox;
import silly.homak.usables.common.barrier.CollisionBoxManagerClient;

public class InsideCubeHudOverlay implements HudRenderCallback {
    private static final Identifier OVERLAY_TEXTURE = Identifier.of(UsablesMain.MOD_ID,
            "textures/overlay/cube_overlay.png");

    // Pulsation timing variables
    private static final float PULSE_SPEED = 0.15f;
    private static final float MIN_ALPHA = 0.75f;
    private static final float MAX_ALPHA = 1.0f;

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            for (CollisionBox box : CollisionBoxManagerClient.CLIENT_BOXES) {
                if (CollisionBoxManagerClient.isPlayerInsideBox(box)) {
                    int width = client.getWindow().getScaledWidth();
                    int height = client.getWindow().getScaledHeight();

                    float time = (System.currentTimeMillis() % 1000000) / 1000f;
                    float pulse = (MathHelper.sin(time * PULSE_SPEED * (float)Math.PI * 2) + 1) / 2; // 0 to 1
                    float alpha = MathHelper.lerp(pulse, MIN_ALPHA, MAX_ALPHA);

                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();

                    RenderSystem.setShader(GameRenderer::getPositionTexProgram);
                    RenderSystem.setShaderColor(1, 1, 1, alpha);
                    RenderSystem.setShaderTexture(0, OVERLAY_TEXTURE);
                    drawContext.drawTexture(OVERLAY_TEXTURE, 0, 0, 0, 0, width, height, width, height);

                    RenderSystem.setShaderColor(1, 1, 1, 1);
                    RenderSystem.disableBlend();
                }
            }
        }
    }
}