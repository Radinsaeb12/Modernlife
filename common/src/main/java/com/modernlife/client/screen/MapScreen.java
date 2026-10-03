package com.modernlife.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;

public class MapScreen extends Screen {
    private final Minecraft minecraft;
    private final DynamicTexture mapTexture;
    private final NativeImage mapImage;
    private final ResourceLocation textureLocation;
    
    private int zoomLevel = 2; 
    private static final int MAP_SIZE = 256; 

    public MapScreen() {
        super(Component.translatable("modernlife.map.title"));
        this.minecraft = Minecraft.getInstance();
        this.mapImage = new NativeImage(MAP_SIZE, MAP_SIZE, false);
        this.mapTexture = new DynamicTexture(mapImage);
        this.textureLocation = minecraft.getTextureManager().register("dynamic_map", mapTexture);
        generateMap();
    }

    private void generateMap() {
        if (minecraft.level == null || minecraft.player == null) return;
        BlockPos playerPos = minecraft.player.blockPosition();
        
        boolean isNether = minecraft.level.dimension() == Level.NETHER;
        boolean isEnd = minecraft.level.dimension() == Level.END;
        boolean isUnderground = isNether || (!isEnd && !minecraft.level.canSeeSky(playerPos));

        for (int x = 0; x < MAP_SIZE; x++) {
            for (int z = 0; z < MAP_SIZE; z++) {
                int worldX = playerPos.getX() - (MAP_SIZE / 2 * zoomLevel) + (x * zoomLevel);
                int worldZ = playerPos.getZ() - (MAP_SIZE / 2 * zoomLevel) + (z * zoomLevel);
                
                int targetY;
                BlockState targetState;

                if (!isUnderground) {
                    targetY = minecraft.level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ) - 1;
                    
                    if (targetY < minecraft.level.getMinBuildHeight()) {
                        mapImage.setPixelRGBA(x, z, 0xFF050505);
                        continue;
                    }
                    targetState = minecraft.level.getBlockState(new BlockPos(worldX, targetY, worldZ));
                    
                    BlockState aboveState = minecraft.level.getBlockState(new BlockPos(worldX, targetY + 1, worldZ));
                    if (aboveState.is(Blocks.LAVA) || aboveState.is(Blocks.WATER) || !aboveState.getFluidState().isEmpty()) {
                        targetState = aboveState;
                    }
                } else {
                    int startY = isNether ? Math.min(playerPos.getY() + 10, 120) : playerPos.getY() + 6;
                    targetY = startY;
                    targetState = minecraft.level.getBlockState(new BlockPos(worldX, targetY, worldZ));

                    while (targetY > minecraft.level.getMinBuildHeight()) {
                        if (targetState.is(Blocks.LAVA) || targetState.is(Blocks.WATER) || !targetState.getFluidState().isEmpty()) {
                            break;
                        }
                        if (!targetState.isAir() && targetState.blocksMotion()) {
                            break;
                        }
                        targetY--;
                        targetState = minecraft.level.getBlockState(new BlockPos(worldX, targetY, worldZ));
                    }

                    int yDiff = Math.abs(targetY - playerPos.getY());
                    int maxAllowedDiff = isNether ? 65 : 22;
                    if (yDiff > maxAllowedDiff) {
                        mapImage.setPixelRGBA(x, z, 0xFF101010);
                        continue;
                    }
                }

                int r, g, b;

                if (targetState.is(Blocks.LAVA) || targetState.getFluidState().is(Fluids.LAVA) || targetState.getFluidState().is(Fluids.FLOWING_LAVA)) {
                    r = 255;
                    g = 106;
                    b = 0;
                } else if (targetState.is(Blocks.WATER) || targetState.getFluidState().is(Fluids.WATER) || targetState.getFluidState().is(Fluids.FLOWING_WATER)) {
                    r = 30;
                    g = 110;
                    b = 255;
                } else {
                    int mapCol = targetState.getMapColor(minecraft.level, new BlockPos(worldX, targetY, worldZ)).col;
                    r = (mapCol >> 16) & 0xFF;
                    g = (mapCol >> 8) & 0xFF;
                    b = mapCol & 0xFF;

                    if (isNether) {
                        int diff = targetY - playerPos.getY();
                        float factor = Math.max(0.6f, Math.min(1.2f, 1.0f + (diff * 0.03f)));
                        r = (int) (r * factor);
                        g = (int) (g * factor);
                        b = (int) (b * factor);
                    }
                }

                int finalColor = (0xFF << 24) | ((b & 0xFF) << 16) | ((g & 0xFF) << 8) | (r & 0xFF);
                mapImage.setPixelRGBA(x, z, finalColor);
            }
        }
        mapTexture.upload();
    }

    private int getPlayerColor(Player player) {
        int hash = player.getUUID().hashCode();
        int r = (Math.abs(hash) % 180) + 75;
        int g = (Math.abs(hash >> 8) % 180) + 75;
        int b = (Math.abs(hash >> 16) % 180) + 75;
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    private int getMobColor(Entity e) {
        if (e instanceof Monster) return 0xFFFF3333;
        return 0xFF33FF33;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (scrollDelta > 0) zoomLevel = Math.max(1, zoomLevel - 1);
        else zoomLevel = Math.min(8, zoomLevel + 1);
        generateMap();
        return super.mouseScrolled(mouseX, mouseY, scrollDelta);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        int centerX = width / 2;
        int centerY = height / 2;
        int mapX = centerX - MAP_SIZE / 2;
        int mapY = centerY - MAP_SIZE / 2;

        guiGraphics.blit(textureLocation, mapX, mapY, 0, 0, MAP_SIZE, MAP_SIZE, MAP_SIZE, MAP_SIZE);

        Entity hoveredEntity = null;

        if (minecraft.level != null && minecraft.player != null) {
            for (Entity e : minecraft.level.entitiesForRendering()) {
                if (e instanceof LivingEntity && e != minecraft.player) {
                    
                    double diffY = e.getY() - minecraft.player.getY();
                    
                    if (Math.abs(diffY) <= 15.0) {
                        double relX = (e.getX() - minecraft.player.getX()) / zoomLevel;
                        double relZ = (e.getZ() - minecraft.player.getZ()) / zoomLevel;
                        
                        int screenEntX = centerX + (int) relX;
                        int screenEntY = centerY + (int) relZ;

                        if (Math.abs(relX) < (MAP_SIZE / 2.0) && Math.abs(relZ) < (MAP_SIZE / 2.0)) {
                            if (e instanceof Player otherPlayer) {
                                int playerColor = getPlayerColor(otherPlayer);
                                
                                guiGraphics.pose().pushPose();
                                guiGraphics.pose().translate(screenEntX, screenEntY, 0);
                                guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(otherPlayer.getYRot() + 180));
                                
                                guiGraphics.fill(-1, -5, 2, -2, playerColor);
                                guiGraphics.fill(-2, -2, 3, 2, playerColor);
                                guiGraphics.fill(-3, 2, 4, 3, playerColor);
                                guiGraphics.pose().popPose();
                            } else {
                                int mobColor = getMobColor(e);
                                if (e instanceof Monster) {
                                    guiGraphics.fill(screenEntX, screenEntY - 3, screenEntX + 1, screenEntY + 4, mobColor);
                                    guiGraphics.fill(screenEntX - 3, screenEntY, screenEntX + 4, screenEntY + 1, mobColor);
                                } else if (e instanceof net.minecraft.world.entity.animal.Animal) {
                                    guiGraphics.fill(screenEntX, screenEntY - 2, screenEntX + 1, screenEntY - 1, mobColor);
                                    guiGraphics.fill(screenEntX - 2, screenEntY, screenEntX + 3, screenEntY + 1, mobColor);
                                    guiGraphics.fill(screenEntX, screenEntY + 2, screenEntX + 1, screenEntY + 3, mobColor);
                                } else {
                                    guiGraphics.fill(screenEntX - 1, screenEntY - 1, screenEntX + 2, screenEntY + 2, mobColor);
                                }
                            }

                            if (diffY > 1.2) {
                                guiGraphics.drawString(font, "↑", screenEntX - 2, screenEntY - 10, 0xFFFFFF, false);
                            } else if (diffY < -1.2) {
                                guiGraphics.drawString(font, "↓", screenEntX - 2, screenEntY + 4, 0xFFFFFF, false);
                            }
                            
                            if (Math.abs(mouseX - screenEntX) < 6 && Math.abs(mouseY - screenEntY) < 6) {
                                hoveredEntity = e;
                            }
                        }
                    }
                }
            }
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, centerY, 0);
        guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(minecraft.player.getYRot() + 180));
        
        int selfColor = 0xFF00AAFF;
        guiGraphics.fill(-1, -6, 2, -3, selfColor);
        guiGraphics.fill(-2, -3, 3, 2, selfColor);
        guiGraphics.fill(-3, 2, 4, 4, selfColor);
        guiGraphics.pose().popPose();

        if (hoveredEntity != null) {
            guiGraphics.drawString(font, hoveredEntity.getName().getString(), mouseX + 10, mouseY - 10, 0xFFFFFF);
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() { 
        mapTexture.close(); 
        mapImage.close(); 
        super.onClose(); 
    }
    
    @Override
    public boolean isPauseScreen() { 
        return false; 
    }
}