"""Static integration checks for console UI assets and mixin registration."""
import json
from pathlib import Path
import re
import struct
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/havoc"
RESOURCES = ROOT / "src/main/resources"


class ConsoleUiTests(unittest.TestCase):
    def test_title_assets_exist(self):
        source = (JAVA / "screen/title/HavocTitleScreen.java").read_text()
        icons = re.search(r'String\[\] ICONS = \{([^}]+)\}', source).group(1)
        for icon in re.findall(r'"([^"]+)"', icons):
            self.assertTrue((RESOURCES / f"assets/havoc/textures/gui/icons/{icon}.png").is_file())
        self.assertTrue((RESOURCES / "assets/havoc/textures/gui/console_background.png").is_file())

    def test_button_and_slider_mixins_registered_once(self):
        config = json.loads((RESOURCES / "havoc.mixins.json").read_text())
        for name in ("ConsoleButtonMixin", "ConsoleSliderMixin"):
            self.assertEqual(config["client"].count(name), 1)
            self.assertTrue((JAVA / f"mixin/{name}.java").is_file())

    def test_title_sections_match(self):
        source = (JAVA / "screen/title/HavocTitleScreen.java").read_text()
        for field in ("TITLES", "LABELS", "DESCRIPTIONS", "ACTIONS", "ICONS"):
            values = re.search(rf'String\[\] {field} = \{{([^}}]+)\}}', source).group(1)
            self.assertEqual(len(re.findall(r'"([^"]+)"', values)), 5, field)

    def test_library_accessors_registered(self):
        config = json.loads((RESOURCES / "havoc.mixins.json").read_text())
        for name in ("ConsoleWorldScreenAccessor", "ConsoleWorldListAccessor", "ConsoleWorldEntryAccessor",
                     "ConsoleServerScreenAccessor", "ConsoleServerEntryAccessor"):
            self.assertEqual(config["client"].count(name), 1)
            self.assertTrue((JAVA / f"mixin/{name}.java").is_file())
        self.assertTrue((RESOURCES / "assets/havoc/textures/gui/console_servers.png").is_file())

    def test_library_swap_does_not_replace_custom_subclasses(self):
        source = (JAVA / "mixin/HavocTitleScreenSwapMixin.java").read_text()
        self.assertIn("screen.getClass() == SelectWorldScreen.class", source)
        self.assertIn("screen.getClass() == JoinMultiplayerScreen.class", source)

    def test_console_v2_covers_are_square(self):
        for name in ("console_world_cover", "console_server_cover"):
            data = (RESOURCES / f"assets/havoc/textures/gui/{name}.png").read_bytes()
            self.assertEqual(data[:8], b"\x89PNG\r\n\x1a\n")
            self.assertEqual(struct.unpack(">II", data[16:24]), (512, 512))

    def test_console_v2_removes_pink_tiles_and_oversized_slogan(self):
        title = (JAVA / "screen/title/HavocTitleScreen.java").read_text()
        self.assertNotIn("menu_background.png", title)
        self.assertNotIn("Твой мир. Твои правила.", title)
        self.assertIn("this.updateTileBounds();", title)
        self.assertIn("LibraryArtwork.cover(index == 1)", title)
        self.assertIn("this::toggleQuickActions", title)
        library = (JAVA / "screen/library/ConsoleLibraryView.java").read_text()
        self.assertIn("tile.setPosition(tile.motion.x(), tile.motion.y())", library)
        self.assertNotIn("card.title(), getX() + 4", library)


if __name__ == "__main__":
    unittest.main()
