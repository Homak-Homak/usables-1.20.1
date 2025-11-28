package silly.homak.usables.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
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

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            synchronized (queuedCubes) {
                if (queuedCubes.isEmpty()) return;

                MatrixStack matrices = context.matrixStack();
                Vec3d camPos = context.camera().getPos();

                RenderSystem.disableCull();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.enableDepthTest();

                RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);

                cubesToRemove.clear();

                for (Cube cube : queuedCubes) {
                    float alpha = cube.baseAlpha;
                    if (cube.fade) {
                        int ticksLived = cube.maxDuration - cube.duration;
                        if (ticksLived >= cube.fadeStart) {
                            int fadeTicks = cube.maxDuration - cube.fadeStart;
                            alpha = cube.baseAlpha * ((float) cube.duration / (float) fadeTicks);
                            if (alpha < 0f) alpha = 0f;
                        }
                    }

                    float scale = cube.scale;
                    if (cube.scaleUp) {
                        int ticksLived = cube.maxDuration - cube.duration;
                        if (ticksLived >= cube.scaleStart) {
                            float t = (ticksLived - cube.scaleStart) / (float) (cube.maxDuration - cube.scaleStart);
                            if (t > 1f) t = 1f;
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

                    cube.duration--;
                    if (cube.duration <= 0) {
                        cubesToRemove.add(cube);
                    }
                }

                // Remove expired cubes
                queuedCubes.removeAll(cubesToRemove);

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
