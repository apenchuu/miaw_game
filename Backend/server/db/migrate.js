const fs = require('fs');
const path = require('path');
const db = require('./index');

async function runMigrations() {
  if (!db.isOnline()) return;
  const p = path.join(__dirname, '..', 'migrations', 'init.sql');
  if (!fs.existsSync(p)) return;
  const sql = fs.readFileSync(p, 'utf8');
  await db.query(sql);
}

module.exports = { runMigrations };
