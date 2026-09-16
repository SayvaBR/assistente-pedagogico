#!/usr/bin/env python3
"""Capture REAL Android screens using ADB; no mocked/rendered UI.

Run after installing/launching the debug APK on an Android emulator. Only
synthetic test values are entered. Captures are stored under visual-evidence/.
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
            result = subprocess.run(['adb', 'exec-out', 'screencap', '-p'], check=True, capture_output=True)
            if result.stdout.startswith(b'\x89PNG\r\n\x1a\n') and len(result.stdout) > 1000:
                path.write_bytes(result.stdout)
                print(f'CAPTURED: {path} ({len(result.stdout)} bytes)', flush=True)
                return
            raise RuntimeError(f'Invalid screenshot stream: {len(result.stdout)} bytes')
        except Exception:
            if attempt == 2:
                raise
            time.sleep(2)


def nodes():
    adb('shell', 'uiautomator', 'dump', '/sdcard/window.xml')
    raw = adb('exec-out', 'cat', '/sdcard/window.xml')
    return list(ET.fromstring(raw).iter('node'))


def tap_text(target):
    for item in nodes():
        value = ' '.join([item.get('text', ''), item.get('content-desc', '')])
        if target.casefold() in value.casefold():
            bounds = item.get('bounds', '')
            parts = re.findall(r'\d+', bounds)
            if len(parts) == 4:
                x1, y1, x2, y2 = map(int, parts)
                adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
                time.sleep(1.4)
                print(f'TAPPED: {target}', flush=True)
                return
    known = [' '.join([n.get('text', ''), n.get('content-desc', '')]) for n in nodes()]
    raise RuntimeError(f'Cannot find {target!r} in Android UI hierarchy. Visible: {known[:65]}')


def write_field(label, value):
    tap_text(label)
    adb('shell', 'input', 'text', value)
    adb('shell', 'input', 'keyevent', '4')  # hide keyboard
    time.sleep(1)


try:
    time.sleep(3)
    screenshot('01-onboarding')
    tap_text('Continuar')
    screenshot('02-onboarding')
    tap_text('Continuar')
    screenshot('03-onboarding')
    tap_text('Começar agora')
    screenshot('04-perfil-inicial')
    write_field('Seu nome', 'ProfessoraTeste')
    tap_text('Continuar')
    screenshot('05-criar-turma')
    write_field('Nome da turma', 'TurmaTeste')
    tap_text('Criar turma')
    screenshot('06-turmas')
    tap_text('Início')
    screenshot('07-home')
    tap_text('Planejamento')
    screenshot('08-planejamento')
    tap_text('Turmas')
    tap_text('TurmaTeste')
    screenshot('09-detalhe-turma')
    tap_text('Frequência')
    screenshot('10-frequencia')
    print('SUCCESS: ten screens captured from the running Android app.', flush=True)
except Exception as error:
    print(f'SCREENSHOT FLOW FAILED (previous captures retained): {error}', file=sys.stderr, flush=True)
    try:
        screenshot('error-current-screen')
    except Exception as capture_error:
        print(f'Also failed capturing diagnostic screenshot: {capture_error}', file=sys.stderr)
    raise
