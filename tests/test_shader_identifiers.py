"""Source-level regressions for shader identifiers; no Minecraft runtime needed."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java"
RESOURCES = ROOT / "src/main/resources/assets"


class ShaderIdentifierTests(unittest.TestCase):
    def test_pipeline_string_overloads_do_not_receive_namespaces(self):
        # Minecraft's String overload uses withDefaultNamespace, not parse.
        pattern = re.compile(r'\.with(?:VertexShader|FragmentShader|Location)\(\s*"([^"\n]*:[^"\n]*)"')
        for path in JAVA.rglob("*.java"):
            with self.subTest(path=str(path.relative_to(ROOT))):
                self.assertEqual(pattern.findall(path.read_text()), [])

    def test_target_esp_only_keeps_requested_modes(self):
        source = (JAVA / "havoc/module/impl/visuals/TargetESP.java").read_text()
        modes = dict(re.findall(r'private static final String (MODE_\w+) = "([^"]+)";', source))
        self.assertEqual(modes, {
            "MODE_MARKER": "Маркер", "MODE_RING": "Кольцо",
            "MODE_GHOSTS": "Призраки", "MODE_BRACKET": "Скан-брекет",
        })
        self.assertIn("List.of(MODE_MARKER, MODE_RING, MODE_GHOSTS, MODE_BRACKET)", source)
        for method in ("renderMarker", "renderRing", "renderGhosts", "renderBracketOverlay"):
            self.assertIn(f"private void {method}(", source)
        pipelines = (JAVA / "havoc/render/TargetEspPipelines.java").read_text()
        self.assertNotIn("ESP_FIELD", pipelines)
        self.assertNotIn("ESP_PARTICLE", pipelines)
        self.assertEqual(list(RESOURCES.rglob("target_esp_field.*")), [])
        self.assertEqual(list(RESOURCES.rglob("target_esp_particle.*")), [])


if __name__ == "__main__":
    unittest.main()
