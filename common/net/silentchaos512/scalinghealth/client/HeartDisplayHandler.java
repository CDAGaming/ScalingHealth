package net.silentchaos512.scalinghealth.client;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.silentchaos512.scalinghealth.ScalingHealth;
import net.silentchaos512.scalinghealth.config.ConfigScalingHealth;

public class HeartDisplayHandler extends Gui {

  public static final ResourceLocation TEXTURE = new ResourceLocation(ScalingHealth.MOD_ID,
      "textures/gui/hud.png");
  public static final int[] colors = new int[] { 0xE60000, 0xE64F12, 0xFF571D, 0xE6B000, 0xC8E600,
      0x4BB300, 0x2AD92D, 0x09D96B, 0x0EE6E2, 0x00ACE6, 0x1880E6, 0x184CE6, 0x6289FF, 0x7676FF,
      0x8363E6, 0xA563E6, 0xCE38FF, 0xFF38FF, 0xFF4BA6, 0xFF4BA6 };

  long lastSystemTime = 0;
  long healthUpdateCounter = 0;
  int updateCounter = 0;
  int playerHealth = 0;
  int lastPlayerHealth = 0;
  Random rand = new Random();

  @SubscribeEvent
  public void onHealthBar(RenderGameOverlayEvent.Pre event) {

    if (!ConfigScalingHealth.CHANGE_HEART_RENDERING || event.getType() != ElementType.HEALTH)
      return;
    event.setCanceled(true);

    Minecraft mc = Minecraft.getMinecraft();
    EntityPlayer player = mc.thePlayer;
    final boolean hardcoreMode = mc.theWorld.getWorldInfo().isHardcoreModeEnabled();

    int width = event.getResolution().getScaledWidth();
    int height = event.getResolution().getScaledHeight();
    GlStateManager.enableBlend();

    int health = MathHelper.ceiling_float_int(player.getHealth());
//    boolean highlight = healthUpdateCounter > updateCounter
//        && (healthUpdateCounter - updateCounter) / 3 % 2 == 1;
    boolean highlight = player.hurtResistantTime / 3 % 2 == 1;

    if (health < playerHealth && player.hurtResistantTime > 0) {
      lastSystemTime = Minecraft.getSystemTime();
      healthUpdateCounter = updateCounter + 20;
    } else if (health > playerHealth && player.hurtResistantTime > 0) {
      lastSystemTime = Minecraft.getSystemTime();
      healthUpdateCounter = updateCounter + 10;
    }

    if (Minecraft.getSystemTime() - lastSystemTime > 1000) {
      playerHealth = health;
      lastPlayerHealth = health;
      lastSystemTime = Minecraft.getSystemTime();
    }

    playerHealth = health;
    int healthLast = lastPlayerHealth;

    IAttributeInstance attrMaxHealth = player
        .getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
    float healthMax = Math.min((float) attrMaxHealth.getAttributeValue(), 20);
    float absorb = MathHelper.ceiling_float_int(player.getAbsorptionAmount());

    int healthRows = MathHelper.ceiling_float_int((healthMax + absorb) / 2f / 10f);
    int rowHeight = Math.max(10 - (healthRows - 2), 3); // TODO: Remove?

    rand.setSeed((long) (updateCounter * 312871));

    int left = width / 2 - 91;
    int top = height - GuiIngameForge.left_height;
    GuiIngameForge.left_height += healthRows * rowHeight; // TODO: Remove?
    if (rowHeight != 10)
      GuiIngameForge.left_height += 10 - rowHeight;

    int regen = -1;
    if (player.isPotionActive(MobEffects.REGENERATION))
      regen = updateCounter % 25;

    final int TOP = 9 * (hardcoreMode ? 5 : 0);
    final int BACKGROUND = (highlight ? 25 : 16);
    int MARGIN = 16;
    if (player.isPotionActive(MobEffects.POISON))
      MARGIN += 36;
    else if (player.isPotionActive(MobEffects.WITHER))
      MARGIN += 72;
    float absorbRemaining = absorb;

    // Draw vanilla hearts
    for (int i = MathHelper.ceiling_float_int((healthMax + absorb) / 2f) - 1; i >= 0; --i) {
      int row = MathHelper.ceiling_float_int((i + 1) / 10f) - 1;
      int x = left + i % 10 * 8;
      int y = top - row * rowHeight;

      if (health <= 4)
        y += rand.nextInt(2);
      if (i == regen)
        y -= 2;

      // ScalingHealth.logHelper.debug(row, String.format("%X", rowColor));

      drawTexturedModalRect(x, y, BACKGROUND, TOP, 9, 9);

      if (highlight) {
        if (i * 2 + 1 < healthLast)
          drawTexturedModalRect(x, y, MARGIN + 54, TOP, 9, 9);
        else if (i * 2 + 1 == healthLast)
          drawTexturedModalRect(x, y, MARGIN + 63, TOP, 9, 9);
      }

      if (absorbRemaining > 0f) {
        if (absorbRemaining == absorb && absorb % 2f == 1f) {
          drawTexturedModalRect(x, y, MARGIN + 153, TOP, 9, 9);
          absorbRemaining -= 1f;
        } else {
          if (i * 2 + 1 < health)
            drawTexturedModalRect(x, y, MARGIN + 144, TOP, 9, 9);
          absorbRemaining -= 2f;
        }
      } else {
        if (i * 2 + 1 < health)
          drawTexturedModalRect(x, y, MARGIN + 36, TOP, 9, 9);
        else if (i * 2 + 1 == health)
          drawTexturedModalRect(x, y, MARGIN + 45, TOP, 9, 9);
      }
    }

    int potionOffset = player.isPotionActive(MobEffects.WITHER) ? 18
        : (player.isPotionActive(MobEffects.POISON) ? 9 : 0) + (hardcoreMode ? 27 : 0);

    mc.renderEngine.bindTexture(TEXTURE);

    health = MathHelper.ceiling_float_int(player.getHealth());
    for (int i = 0; i < health / 20; ++i) {
      int renderHearts = Math.min((health - 20 * (i + 1)) / 2, 10);
      int rowColor = getColorForRow(i);

      for (int j = 0; j < renderHearts; ++j) {
        int y = 0 + (i == regen ? -2 : 0);
        drawTexturedModalRect(left + 8 * j, top + y, 0, potionOffset, 9, 9, rowColor);
      }

      if (health % 2 == 1 && renderHearts < 10)
        drawTexturedModalRect(left + 8 * renderHearts, top, 9, potionOffset, 9, 9, rowColor);
    }

    GlStateManager.disableBlend();
    mc.renderEngine.bindTexture(Gui.ICONS);
  }

  protected void drawTexturedModalRect(int x, int y, int textureX, int textureY, int width,
      int height, int color) {

    float r = ((color >> 16) & 255) / 255f;
    float g = ((color >> 8) & 255) / 255f;
    float b = (color & 255) / 255f;
    GlStateManager.color(r, g, b);
    drawTexturedModalRect(x, y, textureX, textureY, width, height);
    GlStateManager.color(1f, 1f, 1f);
  }

  protected int getColorForRow(int row) {

    return colors[row % colors.length];
  }
}