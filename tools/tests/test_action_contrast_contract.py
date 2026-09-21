from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[2]
TOKENS = ROOT / "app/src/main/java/com/sayvabr/assistentepedagogico/ui/ApDesignTokens.kt"


def _luminance(hex_rgb: str) -> float:
    channels = [int(hex_rgb[index:index + 2], 16) / 255 for index in (0, 2, 4)]
    linear = [value / 12.92 if value <= 0.04045 else ((value + 0.055) / 1.055) ** 2.4 for value in channels]
    return sum(channel * weight for channel, weight in zip(linear, (0.2126, 0.7152, 0.0722)))


class ActionContrastTest(unittest.TestCase):
    def test_primary_action_gradient_preserves_wcag_aa_contrast_for_white_labels(self):
        source = TOKENS.read_text(encoding="utf-8")
        colors = {}
        for token in ("ActionGradientTop", "Action"):
            match = re.search(rf"val {token}\s*=\s*Color\(0xFF([0-9A-Fa-f]{{6}})\)", source)
            self.assertIsNotNone(match, f"Missing approved action color token {token}")
            colors[token] = match.group(1)
            contrast = 1.05 / (_luminance(match.group(1)) + 0.05)
            self.assertGreaterEqual(contrast, 4.5, f"White button label contrast is only {contrast:.2f}:1 for {token}")

        first = [int(colors["ActionGradientTop"][i:i + 2], 16) for i in (0, 2, 4)]
        last = [int(colors["Action"][i:i + 2], 16) for i in (0, 2, 4)]
        for step in range(101):
            progress = step / 100
            mixed = [round(a + (b - a) * progress) for a, b in zip(first, last)]
            hex_rgb = "".join(f"{channel:02X}" for channel in mixed)
            contrast = 1.05 / (_luminance(hex_rgb) + 0.05)
            self.assertGreaterEqual(contrast, 4.5, f"Gradient midpoint {hex_rgb} drops to {contrast:.2f}:1")
