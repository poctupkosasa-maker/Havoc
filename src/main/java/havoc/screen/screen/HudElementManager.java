package havoc.screen.screen;

import havoc.module.setting.BooleanSetting;
import havoc.module.setting.InputSetting;
import havoc.module.setting.MultiSelectSetting;
import havoc.module.setting.SelectSetting;
import havoc.module.setting.Setting;
import havoc.module.setting.SliderSetting;
import havoc.render.RenderUtil;
import havoc.render.font.GFonts;
import havoc.screen.menu.MenuScreen;
import havoc.screen.menu.UiAnimationEngine;
import havoc.screen.menu.sections.settings.BooleanSettingRenderer;
import havoc.screen.menu.sections.settings.InputSettingRenderer;
import havoc.screen.menu.sections.settings.MultiSelectSettingRenderer;
import havoc.screen.menu.sections.settings.SelectSettingRenderer;
import havoc.screen.menu.sections.settings.SliderSettingRenderer;
import havoc.utilites.animation.Animator;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ColorUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

public final class HudElementManager {
   private static final float MENU_PANEL_WIDTH = 500.0F;
   private static final float MENU_PANEL_HEIGHT = 300.0F;
   private static final float POPUP_WIDTH = 114.0F;
   private static final float POPUP_PAD = 6.0F;
   private static final float POPUP_HEADER_H = 16.0F;
   private static final float POPUP_SETTINGS_GAP = 2.0F;
   private static final float GRID_SNAP_THRESHOLD = 5.0F;
   private static final float SELECT_EXPAND_SPEED = 18.0F;
   private static final float MULTI_SELECT_EXPAND_SPEED = 18.0F;
   private final Map<String, HudElement> elements = new LinkedHashMap<>();
   private final Animator animator = new Animator();
   private final UiAnimationEngine settingUiAnimation = new UiAnimationEngine();
   private final Map<String, Boolean> selectExpanded = new HashMap<>();
   private final Map<String, Float> selectExpandProgress = new HashMap<>();
   private final Map<String, Boolean> multiExpanded = new HashMap<>();
   private final Map<String, Float> multiExpandProgress = new HashMap<>();
   private HudElement draggingElement;
   private float dragOffsetX;
   private float dragOffsetY;
   private SliderSetting draggingSlider;
   private float sliderX;
   private float sliderY;
   private float sliderW;
   private HudElement popupElement;
   private HudElement popupRenderElement;
   private HudElementManager.PopupLayout lastPopupLayout;
   private boolean hasRenderableElements;
   private InputSetting focusedInputSetting;
   private String focusedInputKey;
   private int focusedInputCaret;
   private boolean prevLmbDown;
   private boolean prevRmbDown;
   private boolean prevEscDown;

   public void register(HudElement element) {
      if (element != null) {
         this.elements.put(element.getId(), element);
      }
   }

   public List<HudElement> getElements() {
      return new ArrayList<>(this.elements.values());
   }

   public List<Setting> collectAllSettings() {
      List<Setting> out = new ArrayList<>();

      for (HudElement element : this.elements.values()) {
         out.addAll(element.getSettings());
      }

      return out;
   }

   public boolean hasRenderableElements() {
      return this.hasRenderableElements;
   }

   public void render(GuiGraphicsExtractor context, float tickDelta, Set<String> enabledNames, boolean editorEnabled) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.getWindow() != null) {
         this.animator.beginFrame(System.nanoTime());
         int fps = mc.getFps();
         if (fps <= 0) {
            fps = 60;
         }

         this.settingUiAnimation.beginFrame(fps);
         int screenW = mc.getWindow().getGuiScaledWidth();
         int screenH = mc.getWindow().getGuiScaledHeight();
         long window = mc.getWindow().handle();
         float mouseX = (float)(mc.mouseHandler.xpos() * screenW / mc.getWindow().getScreenWidth());
         float mouseY = (float)(mc.mouseHandler.ypos() * screenH / mc.getWindow().getScreenHeight());
         boolean lmbDown = GLFW.glfwGetMouseButton(window, 0) == 1;
         boolean rmbDown = GLFW.glfwGetMouseButton(window, 1) == 1;
         boolean ctrlDown = GLFW.glfwGetKey(window, 341) == 1 || GLFW.glfwGetKey(window, 345) == 1;
         boolean escDown = GLFW.glfwGetKey(window, 256) == 1;
         boolean chatOpen = mc.gui.screen() instanceof ChatScreen;
         boolean lmbPressed = lmbDown && !this.prevLmbDown;
         boolean rmbPressed = rmbDown && !this.prevRmbDown;
         boolean escPressed = escDown && !this.prevEscDown;
         List<HudElement> visible = editorEnabled ? new ArrayList<>() : null;
         this.hasRenderableElements = false;

         for (HudElement element : this.elements.values()) {
            boolean enabled = enabledNames == null || enabledNames.contains(element.getDisplayName());
            element.setActive(enabled);
            if (enabled || element.shouldRenderWhenInactive()) {
               this.clampElementToScreen(element, screenW, screenH);
               element.render(context, screenW, screenH, tickDelta);
               if (!(element.getWidth() <= 1.0F) && !(element.getHeight() <= 1.0F)) {
                  this.hasRenderableElements = true;
                  if (visible != null) {
                     visible.add(element);
                  }
               }
            }
         }

         if (!editorEnabled) {
            this.draggingElement = null;
            this.draggingSlider = null;
            this.closePopup();
            this.prevLmbDown = lmbDown;
            this.prevRmbDown = rmbDown;
            this.prevEscDown = escDown;
         } else {
            boolean mouseOverMenuPanel = this.isMouseOverMenuPanel(mouseX, mouseY, screenW, screenH, mc);
            HudElement hoveredElement = this.getTopHovered(visible, mouseX, mouseY, mc, mouseOverMenuPanel);

            for (HudElement element : visible) {
               if (element.canInteractInEditor(mc)) {
                  boolean hovered = element == hoveredElement;
                  float hover = this.animator.animate("hud:element:hover:" + element.getId(), hovered ? 1.0F : 0.0F, 7.0F);
                  if (hover > 0.01F) {
                     float eased = hover * hover * (3.0F - 2.0F * hover);
                     int fadeOverlay = ColorUtil.rgba(0, 0, 0, Math.round(120.0F * eased));
                     RenderUtil.drawRoundRect(context, element.getX(), element.getY(), element.getWidth(), element.getHeight(), 4.0F, fadeOverlay);
                  }
               }
            }

            if (this.draggingElement != null && lmbDown) {
               this.drawSnapGrid(context, screenW, screenH);
               float targetX = mouseX - this.dragOffsetX;
               float targetY = mouseY - this.dragOffsetY;
               if (!this.draggingElement.allowHorizontalDrag()) {
                  targetX = this.draggingElement.getX();
               }

               if (!this.draggingElement.allowVerticalDrag()) {
                  targetY = this.draggingElement.getY();
               }

               targetX = this.clamp(targetX, 0.0F, Math.max(0.0F, screenW - this.draggingElement.getWidth()));
               targetY = this.clamp(targetY, 0.0F, Math.max(0.0F, screenH - this.draggingElement.getHeight()));
               if (!ctrlDown) {
                  targetX = this.snapElementToQuarterGuides(targetX, this.draggingElement.getWidth(), screenW, 5.0F);
                  targetY = this.snapElementToQuarterGuides(targetY, this.draggingElement.getHeight(), screenH, 5.0F);
                  targetX = this.clamp(targetX, 0.0F, Math.max(0.0F, screenW - this.draggingElement.getWidth()));
                  targetY = this.clamp(targetY, 0.0F, Math.max(0.0F, screenH - this.draggingElement.getHeight()));
               }

               this.draggingElement.setPosition(targetX, targetY);
            }

            if (!lmbDown) {
               this.draggingElement = null;
            }

            if (this.draggingSlider != null && lmbDown) {
               SliderSettingRenderer.setFromMouse(this.draggingSlider, this.sliderX, this.sliderY, this.sliderW, mouseX);
            }

            if (!lmbDown) {
               this.draggingSlider = null;
            }

            boolean popupHovered = this.lastPopupLayout != null
               && this.isHovered(mouseX, mouseY, this.lastPopupLayout.x, this.lastPopupLayout.y, this.lastPopupLayout.width, this.lastPopupLayout.height);
            if (lmbPressed) {
               boolean consumedPopup = false;
               if (popupHovered && this.lastPopupLayout != null) {
                  consumedPopup = this.handlePopupLeftClick(mouseX, mouseY);
                  if (!consumedPopup) {
                     consumedPopup = true;
                  }
               }

               if (!consumedPopup) {
                  if (hoveredElement != null && hoveredElement.canInteractInEditor(mc)) {
                     if (!hoveredElement.handleLeftClick(mc, mouseX, mouseY, screenW, screenH)) {
                        this.draggingElement = hoveredElement;
                        this.dragOffsetX = mouseX - hoveredElement.getX();
                        this.dragOffsetY = mouseY - hoveredElement.getY();
                     }
                  } else if (!popupHovered) {
                     this.closePopup();
                  }
               }
            }

            if (rmbPressed) {
               if (hoveredElement != null && hoveredElement.canInteractInEditor(mc)) {
                  if (hoveredElement.getSettings().isEmpty()) {
                     this.closePopup();
                  } else if (this.popupElement == hoveredElement) {
                     this.closePopup();
                  } else {
                     this.popupElement = hoveredElement;
                     this.popupRenderElement = hoveredElement;
                  }
               } else if (!popupHovered) {
                  this.closePopup();
               }
            }

            if (escPressed) {
               this.closePopup();
            }

            this.renderPopup(context, mouseX, mouseY);
            this.prevLmbDown = lmbDown;
            this.prevRmbDown = rmbDown;
            this.prevEscDown = escDown;
         }
      }
   }

   private void renderPopup(GuiGraphicsExtractor context, float mouseX, float mouseY) {
      HudElement activeElement = this.popupElement != null ? this.popupElement : this.popupRenderElement;
      if (activeElement == null) {
         this.lastPopupLayout = null;
      } else {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.getWindow() != null) {
            int screenW = mc.getWindow().getGuiScaledWidth();
            int screenH = mc.getWindow().getGuiScaledHeight();
            float popupX = activeElement.getX() + activeElement.getWidth() + 6.0F;
            float popupY = activeElement.getY();
            List<HudElementManager.PopupSettingLayout> settingLayouts = this.buildSettingLayouts(activeElement, popupX, popupY);
            float popupHeight = 28.0F;
            if (!settingLayouts.isEmpty()) {
               HudElementManager.PopupSettingLayout last = settingLayouts.get(settingLayouts.size() - 1);
               popupHeight = last.y + last.height - popupY + 6.0F;
            }

            popupX = this.clamp(popupX, 4.0F, Math.max(4.0F, screenW - 114.0F - 4.0F));
            popupY = this.clamp(popupY, 4.0F, Math.max(4.0F, screenH - popupHeight - 4.0F));
            settingLayouts = this.buildSettingLayouts(activeElement, popupX, popupY);
            if (!settingLayouts.isEmpty()) {
               HudElementManager.PopupSettingLayout last = settingLayouts.get(settingLayouts.size() - 1);
               popupHeight = last.y + last.height - popupY + 6.0F;
            }

            float popupTarget = this.popupElement != null ? 1.0F : 0.0F;
            float popupAlpha = this.animator.animate("hud:popup:alpha:" + activeElement.getId(), popupTarget, 18.0F);
            if (popupTarget <= 0.0F && popupAlpha <= 0.01F) {
               this.popupRenderElement = null;
               this.lastPopupLayout = null;
            } else {
               this.lastPopupLayout = new HudElementManager.PopupLayout(popupX, popupY, 114.0F, popupHeight, settingLayouts);
               if (!(popupAlpha <= 0.01F)) {
                  int text = ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, popupAlpha);
                  HudVisualStyle.drawPanel(context, popupX, popupY, 114.0F, popupHeight, popupAlpha);
                  RenderUtil.drawText(context, activeElement.getDisplayName() + " Settings", popupX + 6.0F, popupY + 5.0F, text, GFonts.MEDIUM, 7);
                  int scX1 = (int)Math.floor(popupX);
                  int scY1 = (int)Math.floor(popupY);
                  int scX2 = (int)Math.ceil(popupX + 114.0F);
                  int scY2 = (int)Math.ceil(popupY + popupHeight);
                  context.enableScissor(scX1, scY1, scX2, scY2);

                  for (HudElementManager.PopupSettingLayout layout : settingLayouts) {
                     Setting setting = layout.setting;
                     if (setting instanceof BooleanSetting booleanSetting) {
                        BooleanSettingRenderer.render(
                           context,
                           this.settingUiAnimation,
                           layout.key,
                           booleanSetting,
                           layout.x,
                           layout.y,
                           layout.width,
                           (int)mouseX,
                           (int)mouseY,
                           popupAlpha,
                           false
                        );
                     } else if (setting instanceof SliderSetting sliderSetting) {
                        SliderSettingRenderer.render(
                           context, this.settingUiAnimation, layout.key, sliderSetting, layout.x, layout.y, layout.width, (int)mouseX, (int)mouseY, popupAlpha
                        );
                     } else if (setting instanceof SelectSetting selectSetting) {
                        SelectSettingRenderer.render(
                           context,
                           this.settingUiAnimation,
                           layout.key,
                           selectSetting,
                           layout.x,
                           layout.y,
                           layout.width,
                           (int)mouseX,
                           (int)mouseY,
                           popupAlpha,
                           layout.expandProgress
                        );
                     } else if (setting instanceof MultiSelectSetting multiSelectSetting) {
                        MultiSelectSettingRenderer.render(
                           context,
                           this.settingUiAnimation,
                           layout.key,
                           multiSelectSetting,
                           layout.x,
                           layout.y,
                           layout.width,
                           (int)mouseX,
                           (int)mouseY,
                           popupAlpha,
                           layout.expandProgress
                        );
                     } else if (setting instanceof InputSetting inputSetting) {
                        boolean focused = inputSetting == this.focusedInputSetting && layout.key.equals(this.focusedInputKey);
                        InputSettingRenderer.render(
                           context,
                           layout.key,
                           inputSetting,
                           layout.x,
                           layout.y,
                           layout.width,
                           (int)mouseX,
                           (int)mouseY,
                           popupAlpha,
                           focused,
                           this.focusedInputCaret
                        );
                     }
                  }

                  context.disableScissor();
               }
            }
         } else {
            this.lastPopupLayout = null;
         }
      }
   }

   private List<HudElementManager.PopupSettingLayout> buildSettingLayouts(HudElement element, float popupX, float popupY) {
      List<HudElementManager.PopupSettingLayout> layouts = new ArrayList<>();
      float settingX = popupX + 6.0F;
      float settingW = 102.0F;
      float settingY = popupY + 16.0F + 6.0F;

      for (Setting setting : element.getSettings()) {
         if (setting != null && setting.isVisible()) {
            String key = this.settingKey(element, setting);
            if (setting instanceof BooleanSetting booleanSetting) {
               float h = BooleanSettingRenderer.measureHeight(booleanSetting, settingW);
               layouts.add(new HudElementManager.PopupSettingLayout(setting, key, settingX, settingY, settingW, h, 0.0F));
               settingY += h + 2.0F;
            } else if (setting instanceof SliderSetting sliderSetting) {
               float h = SliderSettingRenderer.measureHeight(sliderSetting, settingW);
               layouts.add(new HudElementManager.PopupSettingLayout(setting, key, settingX, settingY, settingW, h, 0.0F));
               settingY += h + 2.0F;
            } else if (setting instanceof SelectSetting selectSetting) {
               boolean expanded = this.selectExpanded.getOrDefault(key, false);
               float progress = this.animator.animate("hud:popup:select:open:" + key, expanded ? 1.0F : 0.0F, 18.0F);
               this.selectExpandProgress.put(key, progress);
               float h = SelectSettingRenderer.measureHeight(selectSetting, settingW, progress);
               layouts.add(new HudElementManager.PopupSettingLayout(setting, key, settingX, settingY, settingW, h, progress));
               settingY += h + 2.0F;
            } else if (setting instanceof MultiSelectSetting multiSelectSetting) {
               boolean expanded = this.multiExpanded.getOrDefault(key, false);
               float progress = this.animator.animate("hud:popup:multi:open:" + key, expanded ? 1.0F : 0.0F, 18.0F);
               this.multiExpandProgress.put(key, progress);
               float h = MultiSelectSettingRenderer.measureHeight(multiSelectSetting, settingW, progress);
               layouts.add(new HudElementManager.PopupSettingLayout(setting, key, settingX, settingY, settingW, h, progress));
               settingY += h + 2.0F;
            } else if (setting instanceof InputSetting inputSetting) {
               float h = InputSettingRenderer.measureHeight(inputSetting, settingW);
               layouts.add(new HudElementManager.PopupSettingLayout(setting, key, settingX, settingY, settingW, h, 0.0F));
               settingY += h + 2.0F;
            }
         }
      }

      return layouts;
   }

   private boolean handlePopupLeftClick(float mouseX, float mouseY) {
      if (this.lastPopupLayout != null && this.popupElement != null) {
         for (HudElementManager.PopupSettingLayout layout : this.lastPopupLayout.settings) {
            Setting setting = layout.setting;
            if (setting instanceof BooleanSetting booleanSetting) {
               if (BooleanSettingRenderer.hitCheckbox(booleanSetting, layout.x, layout.y, layout.width, mouseX, mouseY, false)) {
                  booleanSetting.toggle();
                  return true;
               }
            } else if (setting instanceof SliderSetting sliderSetting) {
               if (SliderSettingRenderer.hitTrack(sliderSetting, layout.x, layout.y, layout.width, mouseX, mouseY)) {
                  SliderSettingRenderer.setFromMouse(sliderSetting, layout.x, layout.y, layout.width, mouseX);
                  this.draggingSlider = sliderSetting;
                  this.sliderX = layout.x;
                  this.sliderY = layout.y;
                  this.sliderW = layout.width;
                  return true;
               }
            } else if (setting instanceof SelectSetting selectSetting) {
               boolean expanded = this.selectExpanded.getOrDefault(layout.key, false);
               if (SelectSettingRenderer.hitSelectBox(selectSetting, layout.x, layout.y, layout.width, mouseX, mouseY)) {
                  this.selectExpanded.put(layout.key, !expanded);
                  return true;
               }

               int optionIndex = SelectSettingRenderer.hitOptionIndex(selectSetting, layout.x, layout.y, layout.width, mouseX, mouseY, layout.expandProgress);
               if (optionIndex >= 0 && optionIndex < selectSetting.getOptions().size()) {
                  selectSetting.setIndex(optionIndex);
                  return true;
               }
            } else if (setting instanceof MultiSelectSetting multiSelectSetting) {
               boolean expanded = this.multiExpanded.getOrDefault(layout.key, false);
               if (MultiSelectSettingRenderer.hitSelectBox(multiSelectSetting, layout.x, layout.y, layout.width, mouseX, mouseY)) {
                  this.multiExpanded.put(layout.key, !expanded);
                  return true;
               }

               int optionIndex = MultiSelectSettingRenderer.hitOptionIndex(
                  multiSelectSetting, layout.x, layout.y, layout.width, mouseX, mouseY, layout.expandProgress
               );
               if (optionIndex >= 0 && optionIndex < multiSelectSetting.getOptions().size()) {
                  multiSelectSetting.toggle(multiSelectSetting.getOptions().get(optionIndex));
                  return true;
               }
            } else if (setting instanceof InputSetting inputSetting && InputSettingRenderer.hitInputBox(layout.x, layout.y, layout.width, mouseX, mouseY)) {
               this.focusedInputSetting = inputSetting;
               this.focusedInputKey = layout.key;
               String value = inputSetting.getValue();
               this.focusedInputCaret = value == null ? 0 : value.length();
               return true;
            }
         }

         this.focusedInputSetting = null;
         this.focusedInputKey = null;
         this.focusedInputCaret = 0;
         return false;
      } else {
         return false;
      }
   }

   private void clampElementToScreen(HudElement element, int screenW, int screenH) {
      if (!Float.isFinite(element.getX()) || !Float.isFinite(element.getY())) {
         element.setPosition(0.0F, 0.0F);
      }

      float clampedX = this.clamp(element.getX(), 0.0F, Math.max(0.0F, screenW - element.getWidth()));
      float clampedY = this.clamp(element.getY(), 0.0F, Math.max(0.0F, screenH - element.getHeight()));
      if (Math.abs(clampedX - element.getX()) > 0.01F || Math.abs(clampedY - element.getY()) > 0.01F) {
         element.setPosition(clampedX, clampedY);
      }
   }

   private HudElement getTopHovered(List<HudElement> visible, float mouseX, float mouseY, Minecraft mc, boolean mouseOverMenuPanel) {
      if (mouseOverMenuPanel) {
         return null;
      }

      for (int i = visible.size() - 1; i >= 0; i--) {
         HudElement element = visible.get(i);
         if (element.canInteractInEditor(mc) && this.isHovered(mouseX, mouseY, element.getX(), element.getY(), element.getWidth(), element.getHeight())) {
            return element;
         }
      }

      return null;
   }

   private boolean isMouseOverMenuPanel(float mouseX, float mouseY, int screenW, int screenH, Minecraft mc) {
      if (mc != null && mc.gui.screen() instanceof MenuScreen) {
         float panelWidth = Math.min(screenW - 48.0F, 500.0F);
         float panelHeight = Math.min(screenH - 56.0F, 300.0F);
         float panelX = (screenW - panelWidth) * 0.5F;
         float panelY = (screenH - panelHeight) * 0.5F;
         return this.isHovered(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight);
      } else {
         return false;
      }
   }

   private String settingKey(HudElement element, Setting setting) {
      return element.getId() + ":" + setting.getName();
   }

   private void closePopup() {
      if (this.popupElement != null) {
         this.popupRenderElement = this.popupElement;
      }

      this.popupElement = null;
      this.focusedInputSetting = null;
      this.focusedInputKey = null;
      this.focusedInputCaret = 0;
   }

   public boolean handleKeyPressed(KeyEvent key) {
      if (this.focusedInputSetting == null || this.focusedInputKey == null || key == null) {
         return false;
      }

      if (!this.focusedInputSetting.isVisible()) {
         this.focusedInputSetting = null;
         this.focusedInputKey = null;
         this.focusedInputCaret = 0;
         return false;
      }

      String value = this.focusedInputSetting.getValue();
      if (value == null) {
         value = "";
      }

      int code = key.key();
      boolean ctrl = this.isCtrlDown();
      switch (code) {
         case 86:
            if (ctrl) {
               String clip = this.getClipboard();
               if (clip != null && !clip.isEmpty()) {
                  this.insertIntoFocused(clip.replaceAll("[\\r\\n\\t]", " "));
               }

               return true;
            }
         default:
            return false;
         case 256:
            this.focusedInputSetting = null;
            this.focusedInputKey = null;
            this.focusedInputCaret = 0;
            return true;
         case 257:
         case 335:
            this.focusedInputSetting = null;
            this.focusedInputKey = null;
            this.focusedInputCaret = 0;
            return true;
         case 259:
            if (this.focusedInputCaret > 0) {
               this.focusedInputSetting.setValue(value.substring(0, this.focusedInputCaret - 1) + value.substring(this.focusedInputCaret));
               this.focusedInputCaret--;
            }

            return true;
         case 261:
            if (this.focusedInputCaret < value.length()) {
               this.focusedInputSetting.setValue(value.substring(0, this.focusedInputCaret) + value.substring(this.focusedInputCaret + 1));
            }

            return true;
         case 262:
            this.focusedInputCaret = Math.min(value.length(), this.focusedInputCaret + 1);
            return true;
         case 263:
            this.focusedInputCaret = Math.max(0, this.focusedInputCaret - 1);
            return true;
         case 268:
            this.focusedInputCaret = 0;
            return true;
         case 269:
            this.focusedInputCaret = value.length();
            return true;
      }
   }

   public boolean handleCharTyped(int codePoint) {
      if (this.focusedInputSetting == null || this.focusedInputKey == null) {
         return false;
      } else if (!this.focusedInputSetting.isVisible()) {
         this.focusedInputSetting = null;
         this.focusedInputKey = null;
         this.focusedInputCaret = 0;
         return false;
      } else if (!Character.isValidCodePoint(codePoint)) {
         return false;
      } else {
         char chr = (char)codePoint;
         if (chr >= ' ' && chr != 127) {
            this.insertIntoFocused(String.valueOf(chr));
            return true;
         } else {
            return false;
         }
      }
   }

   private void insertIntoFocused(String text) {
      if (this.focusedInputSetting != null && text != null && !text.isEmpty()) {
         String value = this.focusedInputSetting.getValue();
         if (value == null) {
            value = "";
         }

         int caret = Math.max(0, Math.min(this.focusedInputCaret, value.length()));
         String updated = value.substring(0, caret) + text + value.substring(caret);
         this.focusedInputSetting.setValue(updated);
         this.focusedInputCaret = Math.min(this.focusedInputSetting.getValue().length(), caret + text.length());
      }
   }

   private boolean isCtrlDown() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.getWindow() != null) {
         long handle = mc.getWindow().handle();
         return GLFW.glfwGetKey(handle, 341) == 1 || GLFW.glfwGetKey(handle, 345) == 1;
      } else {
         return false;
      }
   }

   private String getClipboard() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.keyboardHandler != null) {
         String value = mc.keyboardHandler.getClipboard();
         return value == null ? "" : value;
      } else {
         return "";
      }
   }

   private float snapElementToQuarterGuides(float start, float size, int screenSize, float threshold) {
      float[] guides = this.quarterGuides(screenSize);
      float best = start;
      float bestDist = Float.MAX_VALUE;
      float left = start;
      float center = start + size * 0.5F;
      float right = start + size;

      for (float guide : guides) {
         float distLeft = Math.abs(left - guide);
         if (distLeft < bestDist) {
            bestDist = distLeft;
            best = guide;
         }

         float distCenter = Math.abs(center - guide);
         if (distCenter < bestDist) {
            bestDist = distCenter;
            best = guide - size * 0.5F;
         }

         float distRight = Math.abs(right - guide);
         if (distRight < bestDist) {
            bestDist = distRight;
            best = guide - size;
         }
      }

      return bestDist <= threshold ? best : start;
   }

   private void drawSnapGrid(GuiGraphicsExtractor context, int screenW, int screenH) {
      float[] xGuides = this.quarterGuides(screenW);
      float[] yGuides = this.quarterGuides(screenH);
      int quarterColor = ColorUtil.rgba(255, 255, 255, 95);
      int centerColor = ColorUtil.rgba(255, 255, 255, 135);

      for (float x : xGuides) {
         int color = Math.abs(x - screenW * 0.5F) < 0.5F ? centerColor : quarterColor;
         RenderUtil.drawRoundRect(context, x, 0.0F, 1.0F, screenH, 0.0F, color);
      }

      for (float y : yGuides) {
         int color = Math.abs(y - screenH * 0.5F) < 0.5F ? centerColor : quarterColor;
         RenderUtil.drawRoundRect(context, 0.0F, y, screenW, 1.0F, 0.0F, color);
      }
   }

   private float[] quarterGuides(int size) {
      float q = size * 0.25F;
      return new float[]{0.0F, q, q * 2.0F, q * 3.0F, size};
   }

   private float clamp(float value, float min, float max) {
      return !Float.isFinite(value) ? min : Math.max(min, Math.min(max, value));
   }

   private boolean isHovered(float mouseX, float mouseY, float x, float y, float w, float h) {
      return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
   }

   private record PopupLayout(float x, float y, float width, float height, List<HudElementManager.PopupSettingLayout> settings) {
   }

   private record PopupSettingLayout(Setting setting, String key, float x, float y, float width, float height, float expandProgress) {
   }
}
