// Exploration-only custody for browser context receipts.
//
// This store does not own Conversation, prompt composition, Vex Home, Memory, or model state.
// Canonical Conversation remains owned by the current VexLife Companion service.
//
// [VXG RealForever]
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';

function writeJsonAtomic(filePath, value) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  const temporary = `${filePath}.partial-${process.pid}-${crypto.randomBytes(4).toString('hex')}`;
  fs.writeFileSync(temporary, `${JSON.stringify(value, null, 2)}\n`);
  fs.renameSync(temporary, filePath);
}

export function createContextStore(dataDir) {
  const contextDir = path.join(dataDir, 'contexts');
  return Object.freeze({
    dataDir,
    saveContext(record) {
      writeJsonAtomic(path.join(contextDir, `${record.contextRef}.json`), record);
    },
    loadContext(contextRef) {
      if (!/^context-[0-9a-f]{16}$/u.test(contextRef)) return null;
      const filePath = path.join(contextDir, `${contextRef}.json`);
      return fs.existsSync(filePath) ? JSON.parse(fs.readFileSync(filePath, 'utf8')) : null;
    }
  });
}
