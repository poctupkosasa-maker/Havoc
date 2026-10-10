"""Static integration checks for console UI assets and mixin registration."""
import json
from pathlib import Path
import re
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


if __name__ == "__main__":
    unittest.main()
