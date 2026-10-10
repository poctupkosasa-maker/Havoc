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

    def test_target_esp_shaders_use_explicit_identifiers_and_exist(self):
        source = (JAVA / "havoc/render/TargetEspPipelines.java").read_text()
        for shader in ("target_esp_field", "target_esp_particle"):
            for stage, extension in (("Vertex", "vsh"), ("Fragment", "fsh")):
                with self.subTest(shader=shader, stage=stage):
                    self.assertIn(
                        f'.with{stage}Shader(Identifier.fromNamespaceAndPath("havoc", "core/{shader}"))',
                        source,
                    )
                    self.assertTrue((RESOURCES / f"havoc/shaders/core/{shader}.{extension}").is_file())


if __name__ == "__main__":
    unittest.main()
