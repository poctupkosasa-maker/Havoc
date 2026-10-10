package havoc.screen.screen.elements;

import havoc.Havoc;
import havoc.module.setting.BooleanSetting;
import havoc.render.RenderUtil;
import havoc.render.font.GFonts;
import havoc.screen.screen.AbstractHudElement;
import havoc.screen.screen.HudVisualStyle;
import havoc.utilites.animation.AnimatedValue;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ClientTheme;
import havoc.utilites.color.ColorUtil;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.scores.PlayerTeam;

public final class StaffListHudElement extends AbstractHudElement {
   private static final float MIN_WIDTH = 108.0F;
   private static final float HEADER_HEIGHT = 24.5F;
   private static final float ROW_HEIGHT = 9.0F;
   private static final float ROW_GAP = 3.5F;
   private final BooleanSetting alwaysShow = this.addSetting(new BooleanSetting("Always show", false));
   private final AnimatedValue visibilityAnim = new AnimatedValue();
   private final AnimatedValue widthAnim = new AnimatedValue();
   private final AnimatedValue heightAnim = new AnimatedValue();
   private List<StaffListHudElement.StaffEntry> cachedStaff = List.of();
   private Object cachedWorld;
   private long nextStaffRefreshAtMs;
   private boolean active = true;

   public StaffListHudElement() {
      super("staff_list", "StaffList", 104.0F, 42.0F, 108.0F, 24.5F);
   }

   @Override
   public void setActive(boolean active) {
      this.active = active;
   }

   @Override
   public boolean shouldRenderWhenInactive() {
      return true;
   }

   @Override
   public void render(GuiGraphicsExtractor context, int screenWidth, int screenHeight, float tickDelta) {
      Minecraft mc = Minecraft.getInstance();
      long now = System.nanoTime();
      boolean chatOpen = mc != null && mc.gui.screen() instanceof ChatScreen;
      List<StaffListHudElement.StaffEntry> staff = this.getCachedStaff(mc, System.currentTimeMillis());
      if (staff.isEmpty() && chatOpen) {
         staff = List.of(
            new StaffListHudElement.StaffEntry("[ADMIN]", "ExampleAdmin", "Online"), new StaffListHudElement.StaffEntry("[HELPER]", "ExampleHelper", "Vanish")
         );
      }

      float textSize = 7.2F;
      float targetWidth = 108.0F;

      for (StaffListHudElement.StaffEntry entry : staff) {
         float rowWidth = 14.0F
            + RenderUtil.getTextWidth(entry.role(), GFonts.MEDIUM, 5.2F)
            + RenderUtil.getTextWidth(entry.name(), GFonts.MEDIUM, textSize)
            + RenderUtil.getTextWidth(entry.status(), GFonts.MEDIUM, 5.4F);
         targetWidth = Math.max(targetWidth, rowWidth);
      }

      float rowsHeight = staff.isEmpty() ? 0.0F : staff.size() * 9.0F + Math.max(0, staff.size() - 1) * 3.5F + 4.0F;
      float targetHeight = 24.5F + rowsHeight;
      boolean shouldShow = this.active && (this.alwaysShow.isValue() || !staff.isEmpty() || chatOpen);
      float visible = this.visibilityAnim.update(shouldShow ? 1.0F : 0.0F, 0.08F, now);
      float width = this.widthAnim.update(targetWidth, 0.09F, now);
      float height = this.heightAnim.update(targetHeight, 0.09F, now);
      this.setBounds(width, height);
      if (!(visible <= 0.01F)) {
         float x = this.getX();
         float y = this.getY();
         float panelAlpha = Math.min(1.0F, visible / 0.34F);
         HudVisualStyle.drawPanel(context, x, y, width, height, panelAlpha);
         RenderUtil.drawText(context, "Staff list", x + 7.0F, y + 6.5F, ColorUtil.multiplyAlpha(HudVisualStyle.PRIMARY_TEXT, visible), GFonts.MEDIUM, 8.5F);
         RenderUtil.drawImage(
            context,
            "textures/gui/icons/player.png",
            x + width - 16.0F,
            y + 6.0F,
            9.0F,
            9.0F,
            0.0F,
            ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT, visible),
            1.0F
         );
         int dividerColor = ClientTheme.isLightMode() ? -16777216 : -2500135;
         float dividerW = width * 0.47F;
         HudVisualStyle.drawHairline(context, x + (width - dividerW) * 0.5F, y + 19.0F, dividerW, dividerColor, visible * 0.92F);
         float rowY = y + 24.5F;

         for (int index = 0; index < staff.size(); index++) {
            StaffListHudElement.StaffEntry entry = staff.get(index);
            float currentY = rowY + index * 12.5F;
            float rectX = x + 7.0F;
            float rectW = width - 14.0F;
            int statusColor = entry.status().equals("Online") ? ColorUtil.rgba(92, 211, 128, 255) : ColorUtil.rgba(238, 171, 76, 255);
            RenderUtil.drawText(
               context, entry.role(), rectX, currentY + 1.0F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT, visible), GFonts.MEDIUM, 5.2F
            );
            float roleW = RenderUtil.getTextWidth(entry.role(), GFonts.MEDIUM, 5.2F);
            float statusW = RenderUtil.getTextWidth(entry.status(), GFonts.MEDIUM, 5.4F);
            RenderUtil.drawTextFadeRight(
               context,
               entry.name(),
               rectX + roleW + 3.0F,
               currentY + 0.5F,
               ColorUtil.multiplyAlpha(HudVisualStyle.PRIMARY_TEXT, visible),
               GFonts.MEDIUM,
               textSize,
               Math.max(12.0F, rectW - roleW - statusW - 14.0F),
               6.0F
            );
            RenderUtil.drawText(
               context, entry.status(), rectX + rectW - statusW, currentY + 1.0F, ColorUtil.multiplyAlpha(statusColor, visible), GFonts.MEDIUM, 5.4F
            );
         }
      }
   }

   private List<StaffListHudElement.StaffEntry> getCachedStaff(Minecraft mc, long nowMs) {
      if (mc != null && mc.level != null && mc.getConnection() != null) {
         if (this.cachedWorld != mc.level || nowMs >= this.nextStaffRefreshAtMs) {
            this.cachedWorld = mc.level;
            this.cachedStaff = this.collectStaff(mc);
            this.nextStaffRefreshAtMs = nowMs + 400L;
         }

         return this.cachedStaff;
      } else {
         this.cachedStaff = List.of();
         this.cachedWorld = null;
         this.nextStaffRefreshAtMs = 0L;
         return this.cachedStaff;
      }
   }

   private List<StaffListHudElement.StaffEntry> collectStaff(Minecraft mc) {
      if (mc != null && mc.level != null && mc.getConnection() != null) {
         Map<String, String> configured = new HashMap<>();
         Havoc havoc = Havoc.getInstance();
         if (havoc != null && havoc.getCommandManager() != null) {
            for (String name : havoc.getCommandManager().getStaffNames()) {
               configured.put(name.toLowerCase(Locale.ROOT), name);
            }
         }

         Set<String> online = new HashSet<>();
         mc.getConnection().getOnlinePlayers().forEach(entry -> online.add(entry.getProfile().name().toLowerCase(Locale.ROOT)));
         Map<String, StaffListHudElement.StaffEntry> result = new HashMap<>();

         for (Entry<String, String> staff : configured.entrySet()) {
            result.put(staff.getKey(), new StaffListHudElement.StaffEntry("[STAFF]", staff.getValue(), online.contains(staff.getKey()) ? "Online" : "Vanish"));
         }

         for (PlayerTeam team : mc.level.getScoreboard().getPlayerTeams()) {
            String role = team.getPlayerPrefix() == null ? "" : team.getPlayerPrefix().getString().trim();
            boolean staffRole = this.isStaffRole(role);

            for (String name : team.getPlayers()) {
               String key = name.toLowerCase(Locale.ROOT);
               if (staffRole || configured.containsKey(key)) {
                  String normalizedRole = role.isBlank() ? "[STAFF]" : this.trimRole(role);
                  result.put(key, new StaffListHudElement.StaffEntry(normalizedRole, name, online.contains(key) ? "Online" : "Vanish"));
               }
            }
         }

         return result.values()
            .stream()
            .sorted(
               Comparator.comparing(StaffListHudElement.StaffEntry::status)
                  .reversed()
                  .thenComparing(StaffListHudElement.StaffEntry::name, String.CASE_INSENSITIVE_ORDER)
            )
            .toList();
      } else {
         return List.of();
      }
   }

   private boolean isStaffRole(String role) {
      String value = role.toLowerCase(Locale.ROOT);
      return value.contains("admin")
         || value.contains("moder")
         || value.contains("helper")
         || value.contains("staff")
         || value.contains("owner")
         || value.contains("curator")
         || value.contains("админ")
         || value.contains("модер")
         || value.contains("хелпер");
   }

   private String trimRole(String role) {
      String clean = role.replaceAll("\\s+", " ").trim();
      if (clean.length() > 16) {
         clean = clean.substring(0, 16);
      }

      return clean;
   }

   private record StaffEntry(String role, String name, String status) {
   }
}
