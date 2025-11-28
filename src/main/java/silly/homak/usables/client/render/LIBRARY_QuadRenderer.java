package silly.homak.usables.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class LIBRARY_QuadRenderer {

    private static final List<Quad> queuedQuads = new ArrayList<>();
    private static final List<Quad> quadsToRemove = new ArrayList<>();

    public static void schedule(Vec3d pos, float width, float height,
                                Vec3d rotation, float scale, Identifier texture,
                                int duration, boolean fade, int fadeStart,
                                boolean scaleUp, int scaleStart, float scaleFactor,
                                float alpha) {
        if (duration <= 0) return;
        synchronized (queuedQuads) {
            queuedQuads.add(new Quad(pos, width, height, rotation, scale, texture, duration,
                    fade, fadeStart, scaleUp, scaleStart, scaleFactor, alpha));
        }
    }

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            synchronized (queuedQuads) {
                if (queuedQuads.isEmpty()) return;

                MatrixStack matrices = context.matrixStack();
                Vec3d camPos = context.camera().getPos();

                RenderSystem.disableCull();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.enableDepthTest();

                RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);

                quadsToRemove.clear();

                for (Quad q : queuedQuads) {
                    float alpha = q.baseAlpha;
                    if (q.fade) {
                        int ticksLived = q.maxDuration - q.duration;
                        if (ticksLived >= q.fadeStart) {
                            int fadeTicks = q.maxDuration - q.fadeStart;
                            alpha = q.baseAlpha * ((float) q.duration / (float) fadeTicks);
                            if (alpha < 0f) alpha = 0f;
                        }
                    }

                    float scale = q.scale;
                    if (q.scaleUp) {
                        int ticksLived = q.maxDuration - q.duration;
                        if (ticksLived >= q.scaleStart) {
                            float t = (ticksLived - q.scaleStart) / (float) (q.maxDuration - q.scaleStart);
                            if (t > 1f) t = 1f;
                            scale = q.scale * (1f + (q.scaleFactor - 1f) * t * t);
                        }
                    }
                    RenderSystem.setShaderTexture(0, q.texture);
                    RenderSystem.setShaderColor(1f, 1f, 1f, alpha);

                    matrices.push();
                    matrices.translate(q.position.x - camPos.x, q.position.y - camPos.y, q.position.z - camPos.z);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) q.rotation.y));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) q.rotation.x));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) q.rotation.z));

                    matrices.scale(scale, scale, scale);
                    Matrix4f matrix = matrices.peek().getPositionMatrix();
                    float hw = q.width / 2f;
                    float hh = q.height / 2f;
                    int r = 255, g = 255, b = 255;
                    int a = (int) (alpha * 255);

                    net.minecraft.client.render.BufferBuilder buffer = net.minecraft.client.render.Tessellator.getInstance().getBuffer();
                    buffer.begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

                    buffer.vertex(matrix, -hw, -hh, 0).texture(0f, 1f).color(r, g, b, a).next();
                    buffer.vertex(matrix, hw, -hh, 0).texture(1f, 1f).color(r, g, b, a).next();
                    buffer.vertex(matrix, hw, hh, 0).texture(1f, 0f).color(r, g, b, a).next();
                    buffer.vertex(matrix, -hw, hh, 0).texture(0f, 0f).color(r, g, b, a).next();

                    Tessellator.getInstance().draw();
                    matrices.pop();

                    q.duration--;
                    if (q.duration <= 0) {
                        quadsToRemove.add(q);
                    }
                }

                // Remove expired quads
                queuedQuads.removeAll(quadsToRemove);

                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                RenderSystem.enableCull();
                RenderSystem.disableBlend();
            }
        });
    }

    private static class Quad {
        Vec3d position;
        float width, height;
        Vec3d rotation;
        float scale;
        Identifier texture;
        int duration, maxDuration;
        boolean fade;
        int fadeStart;
        boolean scaleUp;
        int scaleStart;
        float scaleFactor;
        float baseAlpha;

        Quad(Vec3d pos, float w, float h, Vec3d rot, float s, Identifier tex,
             int duration, boolean fade, int fadeStart,
             boolean scaleUp, int scaleStart, float scaleFactor, float baseAlpha) {
            this.position = pos;
            this.width = w;
            this.height = h;
            this.rotation = rot;
            this.scale = s;
            this.texture = tex;
            this.duration = duration;
            this.maxDuration = duration;
            this.fade = fade;
            this.fadeStart = fadeStart;
            this.scaleUp = scaleUp;
            this.scaleStart = scaleStart;
            this.scaleFactor = scaleFactor;
            this.baseAlpha = baseAlpha;
        }
    }
}
