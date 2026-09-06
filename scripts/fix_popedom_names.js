const fs = require('fs');
const path = require('path');

function walk(dir, acc = []) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, acc);
    else if (p.endsWith('.java')) acc.push(p);
  }
  return acc;
}

const root =
  'e:\\ProgramData\\Document All\\xwechat_files\\wxid_eoj9x9r127ci22_bd90\\msg\\file\\2026-09\\miyf\\server';
const kitchen =
  '@PopedomGroup(value = "11030000", name = "\u7ba1\u7406\u5458", product = "kitchen", sort = 10)';
const iam =
  '@PopedomGroup(value = "10030000", name = "\u7ba1\u7406\u5458", product = "iam", sort = 5)';

let n = 0;
for (const f of walk(root)) {
  let t = fs.readFileSync(f, 'utf8');
  const o = t;
  t = t.replace(
    /@PopedomGroup\(value = "11030000", name = "[^"]*", product = "kitchen", sort = 10\)/g,
    kitchen,
  );
  t = t.replace(
    /@PopedomGroup\(value = "10030000", name = "[^"]*", product = "iam", sort = 5\)/g,
    iam,
  );
  if (t !== o) {
    fs.writeFileSync(f, t, 'utf8');
    n += 1;
    console.log('fixed', f);
  }
}
console.log('count', n);
