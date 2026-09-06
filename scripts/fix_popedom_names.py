# -*- coding: utf-8 -*-
import pathlib
import re

root = pathlib.Path(r"e:\ProgramData\Document All\xwechat_files\wxid_eoj9x9r127ci22_bd90\msg\file\2026-09\miyf\server")
kitchen = '@PopedomGroup(value = "11030000", name = "管理员", product = "kitchen", sort = 10)'
iam = '@PopedomGroup(value = "10030000", name = "管理员", product = "iam", sort = 5)'

for p in root.rglob("*.java"):
    text = p.read_text(encoding="utf-8", errors="replace")
    orig = text
    text = re.sub(
        r'@PopedomGroup\(value = "11030000", name = "[^"]*", product = "kitchen", sort = 10\)',
        kitchen,
        text,
    )
    text = re.sub(
        r'@PopedomGroup\(value = "10030000", name = "[^"]*", product = "iam", sort = 5\)',
        iam,
        text,
    )
    if text != orig:
        p.write_text(text, encoding="utf-8")
        print("fixed", p)
