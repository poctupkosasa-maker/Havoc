package havoc.screen.library;

import havoc.mixin.ConsoleServerEntryAccessor;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.EventLoopGroupHolder;

/** Retains vanilla LAN discovery, server persistence, confirmation dialogs and connection flow. */
public final class ConsoleServerScreen extends JoinMultiplayerScreen {
   private final Screen parent;
   private ConsoleLibraryView view;
   private String rememberedSelection;
   private final Map<ServerData, Long> pingStarted = new IdentityHashMap<>();
   private List<ServerSelectionList.Entry> displayed = List.of();
   private final Map<ServerSelectionList.OnlineServerEntry, byte[]> icons = new IdentityHashMap<>();
   private ExecutorService pingWorkers;
   private volatile int generation;

   public ConsoleServerScreen(Screen parent) { super(parent); this.parent = parent; }

   @Override
   protected void init() {
      // super.init invokes repositionElements; do not rebuild recursively.
      this.view = null;
      super.init();
      this.pingWorkers = Executors.newFixedThreadPool(3, runnable -> {
         Thread thread = new Thread(runnable, "havoc-library-ping");
         thread.setDaemon(true);
         return thread;
      });
      this.generation++;
      List<Button> actions = this.children().stream().filter(Button.class::isInstance).map(Button.class::cast).toList();
      this.clearWidgets();
      this.setFocused(null);
      this.view = new ConsoleLibraryView(this, true, actions, button -> this.addRenderableWidget(button),
         () -> this.minecraft.gui.setScreen(new ConsoleWorldScreen(this.parent)), () -> { });
      this.view.restoreSelection(this.rememberedSelection);
      this.displayed = List.of();
      this.syncCards();
   }

   @Override
   protected void repositionElements() {
      if (view != null) {
         // A screen rebuild also starts LAN detection: close the previous session first.
         this.removed();
         this.rebuildWidgets();
      } else super.repositionElements();
   }

   @Override
   public void tick() {
      super.tick();
      this.syncCards();
      for (ServerSelectionList.Entry entry : displayed) {
         if (entry instanceof ServerSelectionList.OnlineServerEntry online) {
            if (view.isVisible(online.getServerData().ip)) this.startPing(online);
            ServerData data = online.getServerData();
            Long started = pingStarted.get(data);
            if (started != null && data.state() == ServerData.State.PINGING && System.nanoTime() - started > 12_000_000_000L) {
               data.setState(ServerData.State.UNREACHABLE);
               data.motd = Component.translatable("multiplayer.status.cannot_connect");
            }
            byte[] bytes = online.getServerData().getIconBytes();
            if (!Arrays.equals(bytes, icons.get(online))) {
               if (((ConsoleServerEntryAccessor)(Object)online).havoc$uploadIcon(bytes)) {
                  icons.put(online, bytes);
               } else {
                  online.getServerData().setIconBytes(null);
                  icons.remove(online);
               }
            }
         }
      }
   }

   private void startPing(ServerSelectionList.OnlineServerEntry entry) {
      ServerData data = entry.getServerData();
      if (data.state() != ServerData.State.INITIAL || pingWorkers == null || pingWorkers.isShutdown()) return;
      data.setState(ServerData.State.PINGING);
      data.motd = Component.translatable("multiplayer.status.pinging");
      data.status = Component.empty();
      pingStarted.put(data, System.nanoTime());
      final int session = generation;
      var pinger = this.getPinger();
      var client = this.minecraft;
      pingWorkers.submit(() -> {
         if (session != generation) return;
         try {
            pinger.pingServer(data, () -> client.execute(() -> {
               if (session == generation) this.getServers().save();
            }), () -> client.execute(() -> {
               if (session == generation) {
                  data.setState(data.protocol == SharedConstants.getCurrentVersion().protocolVersion()
                     ? ServerData.State.SUCCESSFUL : ServerData.State.INCOMPATIBLE);
                  ((ConsoleServerEntryAccessor)(Object)entry).havoc$refreshStatus();
               }
            }), EventLoopGroupHolder.remote(client.options.useNativeTransport()));
         } catch (Exception exception) {
            client.execute(() -> {
               if (session == generation) {
                  data.setState(ServerData.State.UNREACHABLE);
                  data.motd = Component.translatable("multiplayer.status.cannot_connect");
               }
            });
         } finally {
            // A DNS lookup may finish after the user leaves. Clean up any late connection.
            client.execute(() -> { if (session != generation && this.view == null) pinger.removeAll(); });
         }
      });
   }

   private void syncCards() {
      if (view == null || serverSelectionList.children().equals(displayed)) return;
      // Close replaced icons; native refresh can rebuild entries while remaining on this screen.
      for (ServerSelectionList.Entry old : displayed) {
         if (!serverSelectionList.children().contains(old)) old.close();
      }
      icons.keySet().removeIf(entry -> !serverSelectionList.children().contains(entry));
      displayed = List.copyOf(serverSelectionList.children());
      List<LibraryCard> cards = displayed.stream().filter(e -> !(e instanceof ServerSelectionList.LANHeader))
         .map(entry -> (LibraryCard)new LibraryCard() {
            public String id() {
               return entry instanceof ServerSelectionList.OnlineServerEntry online ? online.getServerData().ip : entry.getNarration().getString();
            }
            public String title() {
               return entry instanceof ServerSelectionList.OnlineServerEntry online ? online.getServerData().name : "Локальная сеть";
            }
            public String description() {
               if (entry instanceof ServerSelectionList.OnlineServerEntry online) {
                  Component motd = online.getServerData().motd;
                  return motd == null ? "Проверяем соединение…" : motd.getString();
               }
               return entry.getNarration().getString();
            }
            public String detail() {
               if (!(entry instanceof ServerSelectionList.OnlineServerEntry online)) return "LAN · Локальная сеть";
               ServerData data = online.getServerData();
               return switch (data.state()) {
                  case SUCCESSFUL -> (data.status == null ? "Онлайн" : data.status.getString()) + "  ·  " + data.ping + " мс";
                  case INCOMPATIBLE -> "Другая версия: " + data.version.getString();
                  case UNREACHABLE -> "Сервер недоступен";
                  default -> "Проверяем соединение…";
               };
            }
            public Identifier image() {
               return entry instanceof ServerSelectionList.OnlineServerEntry online && online.getServerData().getIconBytes() != null
                  ? ((ConsoleServerEntryAccessor)(Object)online).havoc$icon().textureLocation() : LibraryArtwork.SERVERS;
            }
            public void select() { serverSelectionList.setSelected(entry); }
         }).toList();
      if (cards.isEmpty()) serverSelectionList.setSelected(null);
      view.setCards(cards);
   }

   @Override
   public void removed() {
      if (this.view != null) this.rememberedSelection = this.view.selectedId();
      this.generation++;
      if (pingWorkers != null) { pingWorkers.shutdownNow(); pingWorkers = null; }
      super.removed();
      for (ServerSelectionList.Entry entry : displayed) entry.close();
      displayed = List.of();
      icons.clear();
      pingStarted.clear();
      this.view = null;
   }

   @Override
   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) { }

   @Override
   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
      if (view != null) view.render(graphics, mouseX, mouseY, delta);
      super.extractRenderState(graphics, mouseX, mouseY, delta);
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (view != null && view.keyPressed(event)) return true;
      return super.keyPressed(event);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
      if (view != null && view.dismissOptions(event.x(), event.y())) return true;
      return super.mouseClicked(event, doubled);
   }

   @Override
   public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
      if (view != null) { view.scrolled(vertical != 0 ? vertical : horizontal); return true; }
      return super.mouseScrolled(x, y, horizontal, vertical);
   }
}
