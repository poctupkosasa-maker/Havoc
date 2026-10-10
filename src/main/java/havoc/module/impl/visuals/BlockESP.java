package havoc.module.impl.visuals;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.vertex.VertexConsumer;
import havoc.Havoc;
import havoc.event.impl.EventRender3D;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.SliderSetting;
import havoc.render.Render3D;
import havoc.utilites.ChatUtil;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ColorUtil;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import org.lwjgl.opengl.GL11;

@ModuleInfo(name = "BlockESP", desc = "Highlights configured blocks", category = Category.VISUALS)
public final class BlockESP extends Module {
   private static final int MAX_RENDERED_BLOCKS = 1536;
   private final SliderSetting range = this.registerSetting(new SliderSetting("Радиус", 32.0F, 1.0F, 128.0F, 1.0F));
   private final BooleanSetting notifications = this.registerSetting(new BooleanSetting("Уведомления", false));
   private final Set<BlockPos> found = new LinkedHashSet<>();
   private final Set<BlockPos> notified = new HashSet<>();
   private long lastScan;
   private int scanX;
   private int scanY;
   private int scanZ;
   private int scanHorizontal;
   private int scanVertical;
   private double scanRangeSq;
   private BlockPos scanCenter;
   private Set<Block> scanBlocks = Set.of();
   private boolean scanning;

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.level != null) {
         if (!this.scanning && System.currentTimeMillis() - this.lastScan >= 3500L) {
            this.beginScan();
         }

         if (this.scanning) {
            this.scanBatch(12000);
         }
      } else {
         this.found.clear();
      }
   }

   private void beginScan() {
      Set<String> configured = Havoc.getInstance().getCommandManager().getBlockEspBlocks();
      this.found.clear();
      this.lastScan = System.currentTimeMillis();
      if (configured.isEmpty()) {
         this.scanning = false;
      } else {
         Set<Block> blocks = new HashSet<>();

         for (String value : configured) {
            Identifier id = Identifier.tryParse(value);
            if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) {
               blocks.add((Block)BuiltInRegistries.BLOCK.getValue(id));
            }
         }

         this.scanCenter = mc.player.blockPosition();
         this.scanHorizontal = Math.min(Math.round(this.range.getValue()), 40);
         this.scanVertical = Math.min(this.scanHorizontal, 32);
         this.scanRangeSq = this.range.getValue() * this.range.getValue();
         this.scanBlocks = blocks;
         this.scanX = -this.scanHorizontal;
         this.scanY = -this.scanVertical;
         this.scanZ = -this.scanHorizontal;
         this.scanning = true;
      }
   }

   private void scanBatch(int budget) {
      for (int checked = 0; checked < budget && this.scanning; checked++) {
         int x = this.scanX;
         int y = this.scanY;
         int z = this.scanZ;
         this.advanceCursor();
         if (!(x * x + y * y + z * z > this.scanRangeSq)) {
            BlockPos pos = this.scanCenter.offset(x, y, z);
            Block block = mc.level.getBlockState(pos).getBlock();
            if (this.scanBlocks.contains(block)) {
               BlockPos immutable = pos.immutable();
               this.found.add(immutable);
               if (this.notifications.isValue() && this.notified.add(immutable)) {
                  ChatUtil.sendMessage("Found " + BuiltInRegistries.BLOCK.getKey(block) + " at " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
               }
            }
         }
      }

      if (!this.scanning) {
         this.notified.retainAll(this.found);
      }
   }

   private void advanceCursor() {
      if (++this.scanY > this.scanVertical) {
         this.scanY = -this.scanVertical;
         if (++this.scanZ > this.scanHorizontal) {
            this.scanZ = -this.scanHorizontal;
            if (++this.scanX > this.scanHorizontal) {
               this.scanning = false;
            }
         }
      }
   }

   @Subscribe
   public void onRender(EventRender3D event) {
      if (!this.found.isEmpty()) {
         VertexConsumer consumer = event.getConsumers().getBuffer(RenderTypes.lines());
         int color = ColorUtil.withAlpha(ClientColors.CATEGORY_ACCENT_SECOND, 235);
         int rendered = 0;

         for (BlockPos pos : this.found) {
            Render3D.drawOutlinedBox(event.getMatrices(), consumer, new AABB(pos).move(event.getCameraPos().reverse()).inflate(0.002), color);
            if (++rendered >= 1536) {
               break;
            }
         }

         GL11.glDisable(2929);

         try {
            event.getConsumers().draw(RenderTypes.lines());
         } finally {
            GL11.glEnable(2929);
         }
      }
   }

   @Override
   public void onDisable() {
      this.found.clear();
      this.notified.clear();
      this.scanning = false;
      super.onDisable();
   }
}
