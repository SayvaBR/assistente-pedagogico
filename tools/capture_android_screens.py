#!/usr/bin/env python3
"""Capture genuine Android emulator UI with synthetic records only.

A capture is evidence of what the running app rendered, NEVER a generated mockup.
Fail when navigation cannot be proven and retain diagnostic images for debugging.
"""
from pathlib import Path
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

OUT = Path('visual-evidence')
OUT.mkdir(exist_ok=True)


def adb(*args, text=True):
    return subprocess.run(['adb', *args], check=True, capture_output=True, text=text).stdout


def screenshot(name):
    path = OUT / f'{name}.png'
    for attempt in range(3):
        try:
            image = adb('exec-out', 'screencap', '-p', text=False)
            if not image.startswith(b'\x89PNG\r\n\x1a\n') or len(image) < 1000:
                raise RuntimeError('Android returned an invalid PNG screenshot')
            path.write_bytes(image)
            print(f'CAPTURED {path} ({len(image)} bytes)', flush=True)
            return
        except Exception:
            if attempt == 2:
                raise
            time.sleep(2)


def hierarchy():
    adb('shell', 'uiautomator', 'dump', '/sdcard/window.xml')
    raw = adb('exec-out', 'cat', '/sdcard/window.xml')
    root = ET.fromstring(raw)
    parents = {child: parent for parent in root.iter() for child in parent}
    return root, parents


def bounds(node):
    parts = re.findall(r'\d+', node.get('bounds', ''))
    if len(parts) != 4:
        raise ValueError('Android UI element has invalid bounds')
    return tuple(map(int, parts))


def tap(node):
    x1, y1, x2, y2 = bounds(node)
    if x2 <= x1 or y2 <= y1:
        raise ValueError('Android UI element has empty bounds')
    adb('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))
    time.sleep(1.6)


def tap_text(target, exact=False):
    root, _ = hierarchy()
    for node in root.iter('node'):
        texts = (node.get('text', ''), node.get('content-desc', ''))
        found = any(t.casefold() == target.casefold() for t in texts) if exact else any(target.casefold() in t.casefold() for t in texts)
        if found:
            tap(node)
            return
    raise RuntimeError(f'Cannot find action {target!r}; visible labels: {[n.get("text") for n in root.iter("node") if n.get("text")][:40]}')


def device_height():
    text = adb('shell', 'wm', 'size')
    matches = re.findall(r'(\d+)x(\d+)', text)
    if not matches:
        raise RuntimeError(f'Cannot obtain screen resolution: {text}')
    return int(matches[-1][1])


def tap_tab(label):
    """Select an exact label within the bottom bar and tap its clickable ancestor.

    Compose often marks a parent clickable, but its Text child is not clickable.
    Matching a word in the body (e.g. 'Ver planejamento') was a false positive.
    """
    root, parents = hierarchy()
    lower_limit = device_height() * .73
    candidates = []
    for node in root.iter('node'):
        if label.casefold() not in (node.get('text', '').casefold(), node.get('content-desc', '').casefold()):
            continue
        item = node
        while item is not None and item.get('clickable') != 'true':
            item = parents.get(item)
        if item is None:
            continue
        x1, y1, x2, y2 = bounds(item)
        if (y1 + y2) / 2 >= lower_limit:
            candidates.append((y1, item))
    if not candidates:
        raise RuntimeError(f'Bottom tab {label!r} not found in the bottom 27% of the display')
    tap(max(candidates, key=lambda pair: pair[0])[1])


def visible_anchor(root, anchor):
    """Screen anchors may contain suffix punctuation, e.g. 'Vamos criar sua turma?'.

    Always match a meaningful whole phrase, not the tiny tab substring that previously
    allowed duplicate/incorrect captures. A former tuple membership test accidentally
    required exact equality and rejected the correct destination with a question mark.
    """
    needle = anchor.casefold()
    return any(
        needle in candidate.casefold()
        for node in root.iter('node')
        for candidate in (node.get('text', ''), node.get('content-desc', ''))
    )


def wait_for_text(anchor, timeout=12):
    until = time.monotonic() + timeout
    while time.monotonic() < until:
        root, _ = hierarchy()
        if visible_anchor(root, anchor):
            return
        time.sleep(.7)
    root, _ = hierarchy()
    visible = [n.get('text', '') for n in root.iter('node') if n.get('text', '')]
    raise RuntimeError(f'Destination marker {anchor!r} never appeared; actual visible texts: {visible[:36]!r}')


def write_field(label, value):
    tap_text(label)
    adb('shell', 'input', 'text', value)
    adb('shell', 'input', 'keyevent', '4')
    time.sleep(1)


try:
    time.sleep(3)
    screenshot('00-boas-vindas')
    tap_text('Preparar meu espaço', exact=True)
    wait_for_text('Como podemos')
    screenshot('01-perfil')
    write_field('Seu nome', 'ProfessoraTeste')
    tap_text('Continuar', exact=True)
    wait_for_text('Em qual etapa')
    tap_text('Ensino Fundamental', exact=True)
    tap_text('Continuar', exact=True)
    wait_for_text('Sua primeira turma')
    screenshot('02-primeira-turma')
    write_field('Nome da turma', 'TurmaTeste')
    tap_text('Criar meu espaço', exact=True)
    wait_for_text('Sua turma foi salva')
    screenshot('03-turma-criada')
    tap_text('Entrar no meu espaço', exact=True)
    wait_for_text('Olá, ProfessoraTeste!')
    tap_tab('Turmas')
    wait_for_text('Suas turmas')
    screenshot('04-turmas-lista')
    tap_text('TurmaTeste', exact=True)
    wait_for_text('Fazer chamada de hoje')
    screenshot('05-detalhe-turma-vazia')
    tap_text('Adicionar aluno', exact=True)
    wait_for_text('Nome completo do aluno')
    screenshot('06-adicionar-aluno')
    write_field('Nome completo do aluno', 'EstudanteTeste')
    tap_text('Salvar aluno', exact=True)
    wait_for_text('EstudanteTeste')
    screenshot('07-turma-com-aluno')
    tap_text('Fazer chamada de hoje', exact=True)
    wait_for_text('Pendentes')
    screenshot('08-frequencia-da-turma')
    print('SUCCESS: 9 genuine captures from the running app, including the first-access and Turmas journeys.', flush=True)
except Exception as error:
    print(f'ANDROID SCREENSHOT FLOW FAILED: {error}', file=sys.stderr, flush=True)
    try:
        screenshot('error-current-screen')
    except Exception as capture_error:
        print(f'Could not capture failure state: {capture_error}', file=sys.stderr, flush=True)
    raise
