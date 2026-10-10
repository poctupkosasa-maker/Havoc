package havoc.command;

import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import havoc.Havoc;
import havoc.config.ConfigManager;
import havoc.event.impl.EventScreenRender;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.bind.BindData;
import havoc.module.bind.BindMode;
import havoc.module.setting.Setting;
import havoc.render.RenderUtil;
import havoc.render.font.GFonts;
import havoc.utilites.ChatUtil;
import havoc.utilites.color.ClientColors;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public final class ClientCommandManager {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final String PREFIX = ".";
   private final Havoc client;
   private final Path statePath;
   private final Path macrosPath;
   private final Path friendsPath;
   private final Path staffPath;
   private final Path legacyStatePath;
   private final Path legacySkyCoreStatePath;
   private final Map<Integer, String> macros = new HashMap<>();
   private final Set<String> friends = new LinkedHashSet<>();
   private final Set<String> staff = new LinkedHashSet<>();
   private final Set<String> blockEspBlocks = new LinkedHashSet<>();
   private ClientCommandManager.GpsPoint gpsPoint;

   public ClientCommandManager(Havoc client, ConfigManager configManager) {
      this.client = client;
      Path root = ConfigManager.getRootDir();
      this.statePath = root.resolve("data.json");
      this.macrosPath = root.resolve("macros.json");
      this.friendsPath = root.resolve("friends.json");
      this.staffPath = root.resolve("staff.json");
      Path legacyRoot = FabricLoader.getInstance().getConfigDir().resolve("havoc");
      this.legacyStatePath = legacyRoot.resolve("data").resolve("commands.json");
      this.legacySkyCoreStatePath = legacyRoot.resolve("havoccommands.json");
      this.load();
      this.save();
   }

   public boolean handleChatMessage(String message) {
      if (message != null && message.startsWith(".") && message.length() > ".".length()) {
         String raw = message.substring(".".length()).trim();
         if (raw.isEmpty()) {
            return true;
         }

         String[] parts = raw.split("\\s+");
         String command = parts[0].toLowerCase(Locale.ROOT);
         String[] args = raw.length() == command.length() ? new String[0] : raw.substring(command.length()).trim().split("\\s+");
         switch (command) {
            case "help":
               this.help();
               break;
            case "macro":
               this.macro(args, raw);
               break;
            case "friend":
               this.namedList("friend", this.friends, args);
               break;
            case "staff":
               this.namedList("staff", this.staff, args);
               break;
            case "near":
               this.near();
               break;
            case "gps":
               this.gps(args);
               break;
            case "vclip":
            case "clip":
               this.vclip(args);
               break;
            case "cfg":
            case "config":
               this.config(args);
               break;
            case "bind":
               this.bind(args);
               break;
            case "rct":
               this.rct(args);
               break;
            default:
               ChatUtil.sendMessage("Unknown command. Use .help");
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean handleKeyPress(int key) {
      String macro = this.macros.get(key);
      if (macro != null && !macro.isBlank()) {
         this.sendPlayerMessage(macro);
         return true;
      } else {
         return false;
      }
   }

   public Set<String> getStaffNames() {
      return Set.copyOf(this.staff);
   }

   public List<ClientCommandManager.CommandSuggestion> getSuggestions(String input) {
      if (input != null && input.startsWith(".")) {
         String body = input.substring(1);
         boolean trailingSpace = body.endsWith(" ");
         String[] tokens = body.trim().isEmpty() ? new String[0] : body.trim().split("\\s+");
         if (tokens.length == 0) {
            return this.commandSuggestions("");
         }

         if (tokens.length == 1 && !trailingSpace) {
            return this.commandSuggestions(tokens[0]);
         }

         String command = tokens[0].toLowerCase(Locale.ROOT);
         String current = trailingSpace ? "" : tokens[tokens.length - 1];
         int argument = trailingSpace ? tokens.length : tokens.length - 1;

         List<String> variants = switch (command) {
            case "macro" -> argument == 1
               ? List.of("add", "remove", "list", "clear")
               : (
                  argument != 2 || tokens.length <= 1 || !tokens[1].equalsIgnoreCase("add") && !tokens[1].equalsIgnoreCase("remove")
                     ? List.of()
                     : this.keyboardNames()
               );
            case "friend", "staff" -> argument == 1 ? List.of("add", "remove", "list", "clear") : this.onlinePlayerNames();
            case "gps" -> argument == 1 ? List.of("remove", "clear", "player") : List.of();
            case "vclip", "clip" -> argument == 1 ? List.of("up", "down", "1", "-1") : List.of();
            case "cfg", "config" -> argument == 1
               ? List.of("save", "load", "list", "dir")
               : (argument == 2 && tokens.length > 1 && tokens[1].equalsIgnoreCase("load") ? this.client.getConfigManager().listConfigNames() : List.of());
            case "bind" -> argument == 1 ? List.of("list") : List.of();
            default -> List.of();
         };
         String prefix = input.substring(0, Math.max(0, input.length() - current.length()));
         return variants.stream()
            .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(current.toLowerCase(Locale.ROOT)))
            .limit(8L)
            .map(value -> new ClientCommandManager.CommandSuggestion(prefix + value + " ", this.hint(command, argument, value)))
            .toList();
      } else {
         return List.of();
      }
   }

   private List<ClientCommandManager.CommandSuggestion> commandSuggestions(String filter) {
      Map<String, String> commands = new LinkedHashMap<>();
      commands.put("help", "список команд");
      commands.put("macro", "макросы клавиш");
      commands.put("friend", "список друзей");
      commands.put("staff", "список персонала");
      commands.put("gps", "точка маршрута");
      commands.put("vclip", "вертикальный телепорт");
      commands.put("clip", "вертикальный телепорт");
      commands.put("cfg", "конфиги клиента");
      commands.put("config", "конфиги клиента");
      commands.put("bind", "список назначенных биндов");
      commands.put("rct", "перезаход на анархию");
      commands.put("near", "nearby players");
      String lower = filter.toLowerCase(Locale.ROOT);
      return commands.entrySet()
         .stream()
         .filter(entry -> entry.getKey().startsWith(lower))
         .limit(8L)
         .map(entry -> new ClientCommandManager.CommandSuggestion("." + entry.getKey() + " ", entry.getValue()))
         .toList();
   }

   private List<String> keyboardNames() {
      List<String> result = new ArrayList<>();

      for (Field field : GLFW.class.getFields()) {
         if (Modifier.isStatic(field.getModifiers()) && field.getType() == int.class && field.getName().startsWith("GLFW_KEY_")) {
            String key = field.getName().substring("GLFW_KEY_".length());
            if (!key.equals("UNKNOWN") && !key.equals("LAST")) {
               result.add(key);
            }
         }
      }

      return result;
   }

   private List<String> onlinePlayerNames() {
      Minecraft mc = Minecraft.getInstance();
      return mc != null && mc.getConnection() != null
         ? mc.getConnection().getOnlinePlayers().stream().map(entry -> entry.getProfile().name()).sorted().toList()
         : List.of();
   }

   private String hint(String command, int argument, String value) {
      return command.equals("macro") && argument == 2 ? "клавиша " + value : "аргумент для ." + command;
   }

   public boolean isFriend(String name) {
      return name == null ? false : this.friends.stream().anyMatch(friend -> friend.equalsIgnoreCase(name));
   }

   public Set<String> getFriends() {
      return Set.copyOf(this.friends);
   }

   public Set<String> getBlockEspBlocks() {
      return Set.copyOf(this.blockEspBlocks);
   }

   public boolean toggleFriend(String name) {
      if (name != null && !name.isBlank()) {
         String clean = name.trim();
         String existing = this.friends.stream().filter(friend -> friend.equalsIgnoreCase(clean)).findFirst().orElse(null);
         if (existing != null) {
            this.friends.remove(existing);
            this.save();
            return false;
         } else {
            this.friends.add(clean);
            this.save();
            return true;
         }
      } else {
         return false;
      }
   }

   @Subscribe
   public void onScreenRender(EventScreenRender event) {
      if (event != null && event.getStage() == EventScreenRender.Stage.ESP && this.gpsPoint != null) {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.player != null && mc.level != null) {
            GuiGraphicsExtractor context = event.getContext();
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();
            double targetY = this.gpsPoint.y == null ? mc.player.getY() : this.gpsPoint.y.intValue();
            Vec3 target = new Vec3(this.gpsPoint.x + 0.5, targetY, this.gpsPoint.z + 0.5);
            Vec3 playerPos = new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            double dx = target.x - playerPos.x;
            double dz = target.z - playerPos.z;
            int distance = (int)Math.sqrt(dx * dx + dz * dz);
            float targetYaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
            float cameraYaw = mc.gameRenderer.mainCamera().isInitialized() ? mc.gameRenderer.mainCamera().yRot() : mc.player.getYRot();
            float yawDiff = Mth.wrapDegrees(targetYaw - cameraYaw);
            this.renderGpsIndicator(context, screenWidth, screenHeight, yawDiff, distance);
         }
      }
   }

   private void help() {
      ChatUtil.sendMessage("Commands: .macro, .friend, .staff, .near, .gps, .vclip/.clip, .cfg/.config, .bind list, .rct");
   }

   private void bind(String[] args) {
      if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
         int total = 0;

         for (Category category : Category.values()) {
            List<ClientCommandManager.BoundEntry> entries = new ArrayList<>();

            for (Module module : this.client.getModuleManager().getModules(category)) {
               BindData moduleBind = module.getBind();
               if (moduleBind.hasKeys()) {
                  entries.add(new ClientCommandManager.BoundEntry(module.getName(), moduleBind, module.isEnable()));
               }

               for (Setting setting : module.getSettings()) {
                  BindData settingBind = setting.getBind();
                  if (settingBind.hasKeys()) {
                     entries.add(new ClientCommandManager.BoundEntry(module.getName() + " › " + setting.getName(), settingBind, settingBind.isActive()));
                  }
               }
            }

            if (!entries.isEmpty()) {
               total += entries.size();
               ChatUtil.sendMessage("&d" + category.getName() + " &8(" + entries.size() + ")");

               for (ClientCommandManager.BoundEntry entry : entries) {
                  String checkbox = entry.checked() ? "&a☑" : "&7☐";
                  String mode = entry.bind().getMode() == BindMode.HOLD ? "Hold" : "Toggle";
                  ChatUtil.sendMessage(checkbox + " &f" + entry.name() + " &8[&f" + formatBindKeys(entry.bind()) + "&8] &7" + mode);
               }
            }
         }

         if (total == 0) {
            ChatUtil.sendMessage("Назначенных биндов пока нет");
         } else {
            ChatUtil.sendMessage("&8Всего биндов: &f" + total);
         }
      } else {
         ChatUtil.sendMessage("Использование: .bind list");
      }
   }

   private static String formatBindKeys(BindData bind) {
      List<String> names = new ArrayList<>();

      for (int code : bind.getKeys()) {
         if (Havoc.isMouseBindCode(code)) {
            names.add("M" + (Havoc.bindCodeToMouseButton(code) + 1));
         } else {
            names.add(keyName(code));
         }
      }

      return String.join(" + ", names);
   }

   private void near() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         List<AbstractClientPlayer> players = mc.level
            .players()
            .stream()
            .filter(playerx -> playerx != mc.player && !playerx.isRemoved())
            .sorted(Comparator.comparingDouble(mc.player::distanceToSqr))
            .toList();
         if (players.isEmpty()) {
            ChatUtil.sendMessage("Players nearby: empty");
         } else {
            for (AbstractClientPlayer player : players) {
               ChatUtil.sendMessage(player.getName().getString() + " - " + Math.round(mc.player.distanceTo(player)) + "m");
            }
         }
      }
   }

   private void blockEsp(String[] args) {
      String action = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);
      if (action.equals("list")) {
         ChatUtil.sendMessage("BlockESP: " + (this.blockEspBlocks.isEmpty() ? "empty" : String.join(", ", this.blockEspBlocks)));
      } else if (action.equals("clear")) {
         this.blockEspBlocks.clear();
         this.save();
         ChatUtil.sendMessage("BlockESP list cleared");
      } else if (!action.equals("blocks") && !action.equals("allblocks")) {
         if (args.length < 2) {
            ChatUtil.sendMessage("Usage: .blockesp add/remove <block> | list | clear | blocks");
         } else {
            String raw = args[1].toLowerCase(Locale.ROOT);
            Identifier id = Identifier.tryParse(raw.contains(":") ? raw : "minecraft:" + raw);
            if (id != null && BuiltInRegistries.BLOCK.containsKey(id) && BuiltInRegistries.BLOCK.getValue(id) != Blocks.AIR) {
               String canonical = BuiltInRegistries.BLOCK.getKey((Block)BuiltInRegistries.BLOCK.getValue(id)).toString();
               boolean changed = action.equals("add")
                  ? this.blockEspBlocks.add(canonical)
                  : (!action.equals("remove") && !action.equals("del") && !action.equals("delete") ? false : this.blockEspBlocks.remove(canonical));
               if (changed) {
                  this.save();
               }

               ChatUtil.sendMessage(changed ? "BlockESP updated: " + canonical : "BlockESP list was not changed");
            } else {
               ChatUtil.sendMessage("Block not found: " + raw);
            }
         }
      } else {
         ChatUtil.sendMessage("Example: .blockesp add diamond_ore");
      }
   }

   private void macro(String[] args, String raw) {
      if (args.length == 0) {
         ChatUtil.sendMessage("Usage: .macro add <key> <text> | remove <key> | list | clear");
      } else {
         String action = args[0].toLowerCase(Locale.ROOT);
         if (!"list".equals(action)) {
            if ("clear".equals(action)) {
               this.macros.clear();
               this.save();
               ChatUtil.sendMessage("Macros cleared");
            } else if (args.length < 2) {
               ChatUtil.sendMessage("Usage: .macro " + action + " <key>");
            } else {
               int key = keyCode(args[1]);
               if (key == -1) {
                  ChatUtil.sendMessage("Key not found: " + args[1]);
               } else if ("remove".equals(action) || "del".equals(action) || "delete".equals(action)) {
                  this.macros.remove(key);
                  this.save();
                  ChatUtil.sendMessage("Macro removed from " + keyName(key));
               } else if (!"add".equals(action)) {
                  ChatUtil.sendMessage("Usage: .macro add <key> <text>");
               } else {
                  int textStart = nthTokenStart(raw, 3);
                  if (textStart >= 0 && textStart < raw.length()) {
                     String text = raw.substring(textStart).trim();
                     this.macros.put(key, text);
                     this.save();
                     ChatUtil.sendMessage("Macro added: " + keyName(key) + " -> " + text);
                  } else {
                     ChatUtil.sendMessage("Usage: .macro add <key> <text>");
                  }
               }
            }
         } else if (this.macros.isEmpty()) {
            ChatUtil.sendMessage("Macros list is empty");
         } else {
            StringBuilder out = new StringBuilder("Macros:");

            for (Entry<Integer, String> entry : this.macros.entrySet()) {
               out.append("\n").append(keyName(entry.getKey())).append(" -> ").append(entry.getValue());
            }

            ChatUtil.sendMessage(out.toString());
         }
      }
   }

   private void namedList(String label, Set<String> list, String[] args) {
      if (args.length == 0) {
         ChatUtil.sendMessage("Usage: ." + label + " add <name> | remove <name> | list | clear");
      } else {
         String action = args[0].toLowerCase(Locale.ROOT);
         if ("list".equals(action)) {
            ChatUtil.sendMessage(label + " list: " + (list.isEmpty() ? "empty" : String.join(", ", list)));
         } else if ("clear".equals(action)) {
            list.clear();
            this.save();
            ChatUtil.sendMessage(label + " list cleared");
         } else if (args.length < 2) {
            ChatUtil.sendMessage("Usage: ." + label + " " + action + " <name>");
         } else {
            String name = args[1];
            if ("add".equals(action)) {
               if (list.add(name)) {
                  this.save();
                  ChatUtil.sendMessage(name + " added to " + label + " list");
               } else {
                  ChatUtil.sendMessage(name + " is already in " + label + " list");
               }
            } else if (!"remove".equals(action) && !"del".equals(action) && !"delete".equals(action)) {
               ChatUtil.sendMessage("Usage: ." + label + " add <name> | remove <name> | list | clear");
            } else {
               if (list.removeIf(value -> value.equalsIgnoreCase(name))) {
                  this.save();
                  ChatUtil.sendMessage(name + " removed from " + label + " list");
               } else {
                  ChatUtil.sendMessage(name + " was not found in " + label + " list");
               }
            }
         }
      }
   }

   private void gps(String[] args) {
      if (args.length == 0) {
         if (this.gpsPoint == null) {
            ChatUtil.sendMessage("No active GPS. Usage: .gps <x> <z> | .gps player <name> <x> <z> | .gps remove");
         } else {
            ChatUtil.sendMessage("GPS: " + this.gpsPoint.name + " X:" + this.gpsPoint.x + " Z:" + this.gpsPoint.z);
         }
      } else if (!"remove".equalsIgnoreCase(args[0]) && !"clear".equalsIgnoreCase(args[0])) {
         try {
            if ("player".equalsIgnoreCase(args[0]) && args.length >= 5) {
               this.gpsPoint = new ClientCommandManager.GpsPoint(args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
            } else if ("player".equalsIgnoreCase(args[0]) && args.length >= 4) {
               this.gpsPoint = new ClientCommandManager.GpsPoint(args[1], Integer.parseInt(args[2]), null, Integer.parseInt(args[3]));
            } else if (args.length >= 3) {
               this.gpsPoint = new ClientCommandManager.GpsPoint("Point", Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
            } else {
               if (args.length < 2) {
                  ChatUtil.sendMessage("Usage: .gps <x> <z> | .gps <x> <y> <z> | .gps player <name> <x> <z>");
                  return;
               }

               this.gpsPoint = new ClientCommandManager.GpsPoint("Point", Integer.parseInt(args[0]), null, Integer.parseInt(args[1]));
            }

            this.save();
            String yText = this.gpsPoint.y == null ? "" : " Y:" + this.gpsPoint.y;
            ChatUtil.sendMessage("GPS created: " + this.gpsPoint.name + " X:" + this.gpsPoint.x + yText + " Z:" + this.gpsPoint.z);
         } catch (NumberFormatException ignored) {
            ChatUtil.sendMessage("GPS coordinates must be numbers");
         }
      } else {
         this.gpsPoint = null;
         this.save();
         ChatUtil.sendMessage("GPS removed");
      }
   }

   private void renderGpsIndicator(GuiGraphicsExtractor context, int screenWidth, int screenHeight, float yawDiff, int distance) {
      String distanceText = distance + "m";
      float arrowSize = 16.0F;
      float textSize = 7.0F;
      float textW = RenderUtil.getTextWidth(distanceText, GFonts.MEDIUM, textSize);
      float centerX = screenWidth * 0.5F;
      float centerY = screenHeight * 0.25F;
      int accent = ClientColors.CATEGORY_ACCENT;
      RenderUtil.drawRotatedImage(
         context,
         "textures/gui/icons/arrow.png",
         centerX - arrowSize * 0.5F,
         centerY - arrowSize * 0.5F,
         arrowSize,
         arrowSize,
         yawDiff + 180.0F,
         0.0F,
         accent,
         1.0F
      );
      RenderUtil.drawText(context, distanceText, centerX - textW * 0.5F, centerY + 9.0F, ClientColors.TEXT_PRIMARY, GFonts.MEDIUM, textSize);
   }

   private void vclip(String[] args) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.player != null && mc.level != null) {
         if (args.length == 0) {
            ChatUtil.sendMessage("Usage: .vclip <distance> | up | down");
         } else {
            double offset;
            if ("up".equalsIgnoreCase(args[0])) {
               offset = this.findOffset(mc.player.blockPosition(), true);
            } else if ("down".equalsIgnoreCase(args[0])) {
               offset = this.findOffset(mc.player.blockPosition(), false);
            } else {
               try {
                  offset = Double.parseDouble(args[0].replace(',', '.'));
               } catch (NumberFormatException ignored) {
                  ChatUtil.sendMessage("Distance must be a number");
                  return;
               }
            }

            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();

            for (int i = 0; i < 3; i++) {
               mc.player.connection.send(new StatusOnly(mc.player.onGround(), mc.player.horizontalCollision));
            }

            mc.player.connection.send(new Pos(x, y + offset, z, false, mc.player.horizontalCollision));
            mc.player.setPos(x, y + offset, z);
            ChatUtil.sendMessage("Teleported by " + offset + " blocks");
         }
      }
   }

   private double findOffset(BlockPos pos, boolean toUp) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.player != null && mc.level != null) {
         if (toUp) {
            for (int i = 3; i < 255; i++) {
               BlockPos base = pos.offset(0, i, 0);
               BlockPos head = base.above();
               if (mc.level.getBlockState(base).isAir() && mc.level.getBlockState(head).isAir()) {
                  return base.getY() - mc.player.getY();
               }
            }

            return 0.0;
         } else {
            for (int i = -1; i > -255; i--) {
               BlockPos base = pos.offset(0, i, 0);
               BlockPos head = base.below();
               BlockPos air2 = head.below();
               if (!mc.level.getBlockState(base).isAir() && mc.level.getBlockState(head).isAir() && mc.level.getBlockState(air2).isAir()) {
                  return air2.getY() - mc.player.getY();
               }
            }

            return 0.0;
         }
      } else {
         return 0.0;
      }
   }

   private void config(String[] args) {
      ConfigManager configManager = this.client.getConfigManager();
      if (args.length == 0) {
         ChatUtil.sendMessage("Usage: .cfg save <name> | load <name> | list | dir");
      } else {
         String action = args[0].toLowerCase(Locale.ROOT);
         if ("list".equals(action)) {
            ChatUtil.sendMessage("Configs: " + String.join(", ", configManager.listConfigNames()));
         } else if (!"dir".equals(action) && !"folder".equals(action)) {
            if (args.length < 2) {
               ChatUtil.sendMessage("Usage: .cfg " + action + " <name>");
            } else {
               String name = args[1];
               if ("save".equals(action)) {
                  ChatUtil.sendMessage(configManager.saveConfig(name) ? "Config saved" : "Config save failed");
               } else if ("load".equals(action)) {
                  ChatUtil.sendMessage(configManager.loadConfig(name) ? "Config loaded" : "Config load failed");
               } else {
                  ChatUtil.sendMessage("Usage: .cfg save <name> | load <name> | list | dir");
               }
            }
         } else {
            configManager.openConfigFolder();
            ChatUtil.sendMessage("Opening config folder");
         }
      }
   }

   private void rct(String[] args) {
      String anarchy = args.length > 0 ? args[0] : "";
      if (!anarchy.isBlank()) {
         this.sendPlayerMessage("/hub");
         ChatUtil.sendMessage("RCT requested for anarchy " + anarchy + ". SkyCore server auto-reconnect logic is not available here.");
      } else {
         this.sendPlayerMessage("/hub");
         ChatUtil.sendMessage("RCT requested. SkyCore server auto-reconnect logic is not available here.");
      }
   }

   private void sendPlayerMessage(String text) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.player != null && mc.player.connection != null && text != null && !text.isBlank()) {
         if (!this.handleChatMessage(text)) {
            if (text.startsWith("/")) {
               mc.player.connection.sendCommand(text.substring(1));
            } else {
               mc.player.connection.sendChat(text);
            }
         }
      }
   }

   private void load() {
      ClientCommandManager.State state = this.read(this.statePath, ClientCommandManager.State.class);
      if (state == null) {
         state = this.read(this.legacyStatePath, ClientCommandManager.State.class);
      }

      if (state == null) {
         state = this.read(this.legacySkyCoreStatePath, ClientCommandManager.State.class);
      }

      if (state != null) {
         if (state.macros != null) {
            this.macros.putAll(state.macros);
         }

         if (state.friends != null) {
            this.friends.addAll(state.friends);
         }

         if (state.staff != null) {
            this.staff.addAll(state.staff);
         }

         if (state.blockEspBlocks != null) {
            this.blockEspBlocks.addAll(state.blockEspBlocks);
         }

         this.gpsPoint = state.gpsPoint;
      }

      ClientCommandManager.MacroFile macroFile = this.read(this.macrosPath, ClientCommandManager.MacroFile.class);
      if (macroFile != null && macroFile.macros != null) {
         this.macros.clear();
         this.macros.putAll(macroFile.macros);
      }

      ClientCommandManager.NameListFile friendFile = this.read(this.friendsPath, ClientCommandManager.NameListFile.class);
      if (friendFile != null && friendFile.names != null) {
         this.friends.clear();
         this.friends.addAll(friendFile.names);
      }

      ClientCommandManager.NameListFile staffFile = this.read(this.staffPath, ClientCommandManager.NameListFile.class);
      if (staffFile != null && staffFile.names != null) {
         this.staff.clear();
         this.staff.addAll(staffFile.names);
      }
   }

   private void save() {
      ClientCommandManager.State state = new ClientCommandManager.State();
      state.blockEspBlocks = new ArrayList<>(this.blockEspBlocks);
      state.gpsPoint = this.gpsPoint;

      try {
         Files.createDirectories(this.statePath.getParent());
         this.write(this.statePath, state);
         ClientCommandManager.MacroFile macroFile = new ClientCommandManager.MacroFile();
         macroFile.macros = new HashMap<>(this.macros);
         this.write(this.macrosPath, macroFile);
         ClientCommandManager.NameListFile friendFile = new ClientCommandManager.NameListFile();
         friendFile.names = new ArrayList<>(this.friends);
         this.write(this.friendsPath, friendFile);
         ClientCommandManager.NameListFile staffFile = new ClientCommandManager.NameListFile();
         staffFile.names = new ArrayList<>(this.staff);
         this.write(this.staffPath, staffFile);
      } catch (Exception var5) {
      }
   }

   private <T> T read(Path path, Class<T> type) {
      if (path != null && Files.exists(path)) {
         try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return (T)GSON.fromJson(reader, type);
         } catch (Exception ignored) {
            return null;
         }
      } else {
         return null;
      }
   }

   private void write(Path path, Object value) throws Exception {
      try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
         GSON.toJson(value, writer);
      }
   }

   private static int nthTokenStart(String value, int tokenIndex) {
      int current = 0;
      boolean inToken = false;

      for (int i = 0; i < value.length(); i++) {
         boolean ws = Character.isWhitespace(value.charAt(i));
         if (!ws && !inToken) {
            if (current == tokenIndex) {
               return i;
            }

            current++;
            inToken = true;
         } else if (ws) {
            inToken = false;
         }
      }

      return -1;
   }

   private static int keyCode(String raw) {
      if (raw == null) {
         return -1;
      }

      String normalized = normalizeKey(raw);

      for (Field field : GLFW.class.getFields()) {
         int modifiers = field.getModifiers();
         if (Modifier.isStatic(modifiers) && field.getType() == int.class && field.getName().startsWith("GLFW_KEY_")) {
            String name = normalizeKey(field.getName().substring("GLFW_KEY_".length()));
            if (name.equals(normalized)) {
               try {
                  return field.getInt(null);
               } catch (IllegalAccessException var9) {
               }
            }
         }
      }

      return -1;
   }

   private static String keyName(int key) {
      for (Field field : GLFW.class.getFields()) {
         int modifiers = field.getModifiers();
         if (Modifier.isStatic(modifiers) && field.getType() == int.class && field.getName().startsWith("GLFW_KEY_")) {
            try {
               if (field.getInt(null) == key) {
                  return field.getName().substring("GLFW_KEY_".length()).replace('_', ' ');
               }
            } catch (IllegalAccessException var7) {
            }
         }
      }

      return String.valueOf(key);
   }

   private static String normalizeKey(String value) {
      return value.trim().toUpperCase(Locale.ROOT).replace(" ", "_").replace("-", "_");
   }

   private record BoundEntry(String name, BindData bind, boolean checked) {
   }

   public record CommandSuggestion(String completion, String hint) {
   }

   private static final class GpsPoint {
      String name;
      int x;
      Integer y;
      int z;

      GpsPoint(String name, int x, Integer y, int z) {
         this.name = name;
         this.x = x;
         this.y = y;
         this.z = z;
      }
   }

   private static final class MacroFile {
      Map<Integer, String> macros;
   }

   private static final class NameListFile {
      List<String> names;
   }

   private static final class State {
      Map<Integer, String> macros;
      List<String> friends;
      List<String> staff;
      List<String> blockEspBlocks;
      ClientCommandManager.GpsPoint gpsPoint;
   }
}
