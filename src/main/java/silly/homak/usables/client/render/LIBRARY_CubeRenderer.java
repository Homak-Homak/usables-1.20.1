package silly.homak.usables.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class LIBRARY_CubeRenderer {

    private static final List<Cube> queuedCubes = new ArrayList<>();
    private static final List<Cube> cubesToRemove = new ArrayList<>();
    private static int tickCounter = 0;
    private static float partialTicks = 0f;

    static {
        init();
    }

    public static void schedule(Vec3d pos, float width, float height, float depth,
                                Vec3d rotation, float scale, Identifier texture,
                                int duration, boolean fade, int fadeStart,
                                boolean scaleUp, int scaleStart, float scaleFactor,
                                float alpha) {
        if (duration <= 0) return;
        synchronized (queuedCubes) {
            queuedCubes.add(new Cube(pos, width, height, depth, rotation, scale, texture, duration,
                    fade, fadeStart, scaleUp, scaleStart, scaleFactor, alpha));
        }
    }

    public static void clientTick() {
        synchronized (queuedCubes) {
            if (queuedCubes.isEmpty()) return;

            tickCounter++;
            cubesToRemove.clear();
            for (Cube cube : queuedCubes) {
                if (!MinecraftClient.getInstance().isPaused() || !MinecraftClient.getInstance().isInSingleplayer()) {
                    cube.prevDuration = cube.duration;
                    cube.duration--;
                }
                if (cube.duration <= 0) {
                    cubesToRemove.add(cube);
                }
            }
            queuedCubes.removeAll(cubesToRemove);
        }
    }

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            synchronized (queuedCubes) {
                if (queuedCubes.isEmpty()) return;

                // Get partial ticks from the render context
                partialTicks = context.tickDelta();

                MatrixStack matrices = context.matrixStack();
                Vec3d camPos = context.camera().getPos();

                RenderSystem.disableCull();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.enableDepthTest();

                RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);

                for (Cube cube : queuedCubes) {
                    // Calculate interpolated duration for smooth effects
                    // At the start of a tick: partialTicks = 0, we use current duration
                    // At the end of a tick: partialTicks = 1, we use next duration
                    float interpolatedDuration = cube.prevDuration + (cube.duration - cube.prevDuration) * partialTicks;
                    float interpolatedTicksLived = cube.maxDuration - interpolatedDuration;

                    float alpha = cube.baseAlpha;
                    if (cube.fade) {
                        if (interpolatedTicksLived >= cube.fadeStart) {
                            int fadeTicks = cube.maxDuration - cube.fadeStart;
                            float remainingDuration = Math.max(0, interpolatedDuration - cube.fadeStart);
                            alpha = cube.baseAlpha * (remainingDuration / fadeTicks);
                            if (alpha < 0f) alpha = 0f;
                        }
                    }

                    float scale = cube.scale;
                    if (cube.scaleUp) {
                        if (interpolatedTicksLived >= cube.scaleStart) {
                            float t = (interpolatedTicksLived - cube.scaleStart) / (cube.maxDuration - cube.scaleStart);
                            if (t > 1f) t = 1f;
                            // Smooth easing using t²
                            scale = cube.scale * (1f + (cube.scaleFactor - 1f) * t * t);
                        }
                    }

                    RenderSystem.setShaderTexture(0, cube.texture);
                    RenderSystem.setShaderColor(1f, 1f, 1f, alpha);

                    matrices.push();
                    matrices.translate(cube.position.x - camPos.x, cube.position.y - camPos.y, cube.position.z - camPos.z);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) cube.rotation.y));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) cube.rotation.x));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) cube.rotation.z));

                    matrices.scale(scale, scale, scale);

                    Matrix4f matrix = matrices.peek().getPositionMatrix();
                    float hw = cube.width / 2f;
                    float hh = cube.height / 2f;
                    float hd = cube.depth / 2f;
                    int r = 255, g = 255, b = 255;
                    int a = (int) (alpha * 255);

                    BufferBuilder buffer = Tessellator.getInstance().getBuffer();
                    buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

                    // Front face
                    buffer.vertex(matrix, -hw, -hh, -hd).texture(0f, 1f).color(r, g, b, a).next();
                    buffer.vertex(matrix, hw, -hh, -hd).texture(1f, 1f).color(r, g, b, a).next();
                    buffer.vertex(matrix, hw, hh, -hd).texture(1f, 0f).color(r, g, b, a).next();
                    buffer.vertex(matrix, -hw, hh, -hd).texture(0f, 0f).color(r, g, b, a).next();

                    // Back face
                    buffer.vertex(matrix, -hw, -hh, hd).texture(1f, 1f).color(r, g, b, a).next();
                    buffer.vertex(matrix, -hw, hh, hd).texture(1f, 0f).color(r, g, b, a).next();
                    buffer.vertex(matrix, hw, hh, hd).texture(0f, 0f).color(r, g, b, a).next();
                    buffer.vertex(matrix, hw, -hh, hd).texture(0f, 1f).color(r, g, b, a).next();

                    // Top face
                    buffer.vertex(matrix, -hw, hh, -hd).texture(0f, 1f).color(r, g, b, a).next();
                    buffer.vertex(matrix, hw, hh, -hd).texture(1f, 1f).color(r, g, b, a).next();
                    buffer.vertex(matrix, hw, hh, hd).texture(1f, 0f).color(r, g, b, a).next();
                    buffer.vertex(matrix, -hw, hh, hd).texture(0f, 0f).color(r, g, b, a).next();

                    // Bottom face
                    buffer.vertex(matrix, -hw, -hh, -hd).texture(0f, 0f).color(r, g, b, a).next();
                    buffer.vertex(matrix, -hw, -hh, hd).texture(0f, 1f).color(r, g, b, a).next();
                    buffer.vertex(matrix, hw, -hh, hd).texture(1f, 1f).color(r, g, b, a).next();
                    buffer.vertex(matrix, hw, -hh, -hd).texture(1f, 0f).color(r, g, b, a).next();

                    // Right face
                    buffer.vertex(matrix, hw, -hh, -hd).texture(0f, 1f).color(r, g, b, a).next();
                    buffer.vertex(matrix, hw, -hh, hd).texture(1f, 1f).color(r, g, b, a).next();
                    buffer.vertex(matrix, hw, hh, hd).texture(1f, 0f).color(r, g, b, a).next();
                    buffer.vertex(matrix, hw, hh, -hd).texture(0f, 0f).color(r, g, b, a).next();

                    // Left face
                    buffer.vertex(matrix, -hw, -hh, -hd).texture(1f, 1f).color(r, g, b, a).next();
                    buffer.vertex(matrix, -hw, hh, -hd).texture(1f, 0f).color(r, g, b, a).next();
                    buffer.vertex(matrix, -hw, hh, hd).texture(0f, 0f).color(r, g, b, a).next();
                    buffer.vertex(matrix, -hw, -hh, hd).texture(0f, 1f).color(r, g, b, a).next();

                    Tessellator.getInstance().draw();
                    matrices.pop();
                }

                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                RenderSystem.enableCull();
                RenderSystem.disableBlend();
            }
        });
    }

    private static class Cube {
        Vec3d position;
        float width, height, depth;
        Vec3d rotation;
        float scale;
        Identifier texture;
        int duration, maxDuration;
        int prevDuration; // For interpolation
        boolean fade;
        int fadeStart;
        boolean scaleUp;
        int scaleStart;
        float scaleFactor;
        float baseAlpha;

        Cube(Vec3d pos, float w, float h, float d, Vec3d rot, float s, Identifier tex,
             int duration, boolean fade, int fadeStart,
             boolean scaleUp, int scaleStart, float scaleFactor, float baseAlpha) {
            this.position = pos;
            this.width = w;
            this.height = h;
            this.depth = d;
            this.rotation = rot;
            this.scale = s;
            this.texture = tex;
            this.duration = duration;
            this.prevDuration = duration;
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