package me.voidxwalker.worldpreview.mixin.client.render;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.systems.RenderSystem;
import me.voidxwalker.worldpreview.WorldPreview;
import me.voidxwalker.worldpreview.mixin.access.PlayerEntityAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin extends Screen {

    @Unique
    private static final ButtonWidget.PressAction NO_OP = button -> {};

    @Unique
    private boolean showMenu = true;
    @Unique
    private boolean freezePreview = false;

    protected LevelLoadingScreenMixin(Text title) {
        super(title);
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/LevelLoadingScreen;renderBackground(Lnet/minecraft/client/util/math/MatrixStack;)V"))
    private boolean stopRenderingBackground(LevelLoadingScreen screen, MatrixStack matrixStack) {
        return !WorldPreview.inPreview;
    }

    @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 2)
    private int moveChunkMapX(int i) {
        return 45;
    }

    @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 3)
    private int moveChunkMapY(int i) {
        return this.height - 75;
    }

    @SuppressWarnings("deprecation")
    @Inject(method = "render", at = @At("HEAD"))
    private void renderWorldPreview(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!WorldPreview.inPreview) {
            synchronized (WorldPreview.LOCK) {
                WorldPreview.inPreview = WorldPreview.world != null && WorldPreview.player != null && WorldPreview.camera != null && WorldPreview.playerListEntry != null;

                if (WorldPreview.inPreview) {
                    WorldPreview.worldRenderer.setWorld(WorldPreview.world);
                    WorldPreview.renderingPreview = false;
                }
            }
        }

        if (!WorldPreview.inPreview || this.freezePreview) {
            return;
        }

        assert this.client != null;

        // clip the player into swimming/crawling mode if necessary
        ((PlayerEntityAccessor) WorldPreview.player).callUpdateSize();

        Window window = this.client.getWindow();

        RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
        RenderSystem.loadIdentity();
        RenderSystem.ortho(0.0, window.getFramebufferWidth(), window.getFramebufferHeight(), 0.0, 1000.0, 3000.0);
        RenderSystem.loadIdentity();
        RenderSystem.translatef(0.0F, 0.0F, 0.0F);
        DiffuseLighting.disableGuiDepthLighting();

        this.client.gameRenderer.renderWorld(0.0F, Util.getMeasuringTimeNano(), new MatrixStack());
        WorldPreview.worldRenderer.drawEntityOutlinesFramebuffer();

        RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
        RenderSystem.matrixMode(5889);
        RenderSystem.loadIdentity();
        RenderSystem.ortho(0.0D, (double) window.getFramebufferWidth() / window.getScaleFactor(), (double) window.getFramebufferHeight() / window.getScaleFactor(), 0.0D, 1000.0D, 3000.0D);
        RenderSystem.matrixMode(5888);
        RenderSystem.loadIdentity();
        RenderSystem.translatef(0.0F, 0.0F, -2000.0F);
        DiffuseLighting.enableGuiDepthLighting();
        RenderSystem.defaultAlphaFunc();

        this.client.inGameHud.render(matrices, 0.0F);

        RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);

        this.worldpreview$renderPauseMenu(matrices, mouseX, mouseY, delta);
    }

    @Unique
    private void worldpreview$renderPauseMenu(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (this.showMenu) {
            this.fillGradient(matrices, 0, 0, this.width, this.height, -1072689136, -804253680);
            super.render(matrices, mouseX, mouseY, delta);
        } else {
            this.drawCenteredText(matrices, this.textRenderer, new TranslatableText("menu.paused"), this.width / 2, 10, 16777215);
        }
    }

    @Unique
    private void worldpreview$initPauseMenuWidgets() {
        this.addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 24 - 16, 204, 20, new TranslatableText("menu.returnToGame"), button -> {
            this.showMenu = false;
            this.children.clear();
        }));
        this.addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 48 - 16, 98, 20, new TranslatableText("gui.advancements"), NO_OP));
        this.addButton(new ButtonWidget(this.width / 2 + 4, this.height / 4 + 48 - 16, 98, 20, new TranslatableText("gui.stats"), NO_OP));
        this.addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 72 - 16, 98, 20, new TranslatableText("menu.sendFeedback"), NO_OP));
        this.addButton(new ButtonWidget(this.width / 2 + 4, this.height / 4 + 72 - 16, 98, 20, new TranslatableText("menu.reportBugs"), NO_OP));
        this.addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 96 - 16, 98, 20, new TranslatableText("menu.options"), NO_OP));
        this.addButton(new ButtonWidget(this.width / 2 + 4, this.height / 4 + 96 - 16, 98, 20, new TranslatableText("menu.shareToLan"), NO_OP));
        this.addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 120 - 16, 204, 20, new TranslatableText("menu.returnToMenu"), button -> {
            WorldPreview.kill = true;
            button.active = false;
        }));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (this.showMenu) {
                if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_F3)) {
                    this.showMenu = false;
                    this.children.clear();
                }
            } else {
                this.showMenu = true;
                this.worldpreview$initPauseMenuWidgets();
            }
            return true;
        }
        if (WorldPreview.freezeKey.matchesKey(keyCode, scanCode)) {
            this.freezePreview = !this.freezePreview;
            return true;
        }
        if (WorldPreview.resetKey.matchesKey(keyCode, scanCode)) {
            WorldPreview.kill = true;
            return true;
        }
        return false;
    }

    @Override
    protected void init() {
        super.init();
        if (this.showMenu) {
            this.worldpreview$initPauseMenuWidgets();
        }
    }

    @Override
    public void removed() {
        WorldPreview.clear();

        WorldPreview.kill = false;
        WorldPreview.inPreview = false;
        WorldPreview.renderingPreview = false;

        WorldPreview.worldRenderer.setWorld(null);
    }
}
