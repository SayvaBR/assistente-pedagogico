from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
COMPONENTS = ROOT / "app/src/main/java/com/sayvabr/assistentepedagogico/ui/ApVisualComponents.kt"
APP = ROOT / "app/src/main/java/com/sayvabr/assistentepedagogico/ui/TeacherApp.kt"


def test_hero_eyebrow_defaults_to_white_on_primary():
    source = COMPONENTS.read_text(encoding="utf-8")
    assert re.search(r"fun ApEyebrow\([^)]*onPrimary:Boolean=true", source)
    assert "if(onPrimary)ApColors.White else ApColors.Pressed" in source


def test_home_hero_uses_shared_eyebrow_primitive():
    source = APP.read_text(encoding="utf-8")
    hero = source[source.index('Surface(shape = RoundedCornerShape(ApShapeToken.Hero)'):]
    hero = hero[:hero.index('Spacer(Modifier.height(20.dp))')]
    assert 'ApEyebrow("Aula em foco"' in hero
