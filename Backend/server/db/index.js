const { Pool } = require('pg');
const fs = require('fs');
const path = require('path');
const { randomUUID } = require('crypto');
const dns = require('dns').promises;

let pool = null;
let online = false;
let store = null;

let resolvedHost = null;
let resolvedPort = 5432;
let resolvedDatabase = 'postgres';
let resolvedUser = null;
let resolvedPassword = null;
let resolvedServerName = null;

const storePath = path.join(__dirname, 'store.json');

function defaultStore() {
  return {
    users: [],
    player_progress: [],
    player_inventory: [],
    opened_chests: [],
  };
}

async function loadStore() {
  if (!fs.existsSync(storePath)) {
    store = defaultStore();
    await saveStore();
    return;
  }
  const raw = await fs.promises.readFile(storePath, 'utf8');
  store = raw ? JSON.parse(raw) : defaultStore();
}

async function saveStore() {
  await fs.promises.writeFile(storePath, JSON.stringify(store, null, 2), 'utf8');
}

function buildConnectionFromEnv() {
  const connectionString = process.env.PSQL_CONNECTION_STRING || process.env.DATABASE_URL || process.env.PSQL || process.env.DATABASE;
  if (!connectionString) return null;

  const url = new URL(connectionString);
  resolvedServerName = url.hostname;
  resolvedHost = url.hostname;
  resolvedPort = url.port ? Number(url.port) : 5432;
  resolvedDatabase = (url.pathname || '/postgres').replace(/^\//, '') || 'postgres';
  resolvedUser = decodeURIComponent(url.username || '');
  resolvedPassword = decodeURIComponent(url.password || '');
  return connectionString;
}

async function resolvePreferredIpv4(hostname) {
  try {
    const addresses = await dns.resolve4(hostname);
    if (addresses && addresses.length > 0) {
      return addresses[0];
    }
  } catch (err) {
    // ignore and let the caller use hostname directly
  }
  return hostname;
}

async function resolveAllIpv4(hostname) {
  try {
    const addresses = await dns.resolve4(hostname);
    if (addresses && addresses.length > 0) {
      return addresses;
    }
  } catch (err) {
    // ignore and fallback to hostname
  }
  return [hostname];
}

function createPoolForHost(host) {
  return new Pool({
    host,
    port: resolvedPort,
    user: resolvedUser,
    password: resolvedPassword,
    database: resolvedDatabase,
    ssl: { rejectUnauthorized: false, servername: resolvedServerName },
    family: 4,
    keepAlive: true,
    connectionTimeoutMillis: 15000,
    idleTimeoutMillis: 30000,
    max: 10,
  });
}

async function init() {
  const connectionString = buildConnectionFromEnv();
  if (!connectionString) {
    online = false;
    await loadStore();
    return;
  }

  const preferredHost = await resolvePreferredIpv4(resolvedHost);
  const allHosts = await resolveAllIpv4(resolvedHost);
  const candidateHosts = [preferredHost, ...allHosts.filter((ip) => ip !== preferredHost)];

  let lastError = null;
  for (const host of candidateHosts) {
    const candidatePool = createPoolForHost(host);
    try {
      await candidatePool.query('SELECT 1');
      pool = candidatePool;
      online = true;
      try {
        await ensureProgressSchema();
      } catch (schemaErr) {
        console.warn(`Progress schema migration skipped: ${schemaErr.message}`);
      }
      console.log(`Neon connected via ${host}:${resolvedPort}`);
      return;
    } catch (err) {
      lastError = err;
      try {
        await candidatePool.end();
      } catch (_) {
        // ignore close errors
      }
    }
  }

  online = false;
  console.warn('Neon tidak terjangkau di semua host IPv4 hasil DNS, memakai storage lokal sementara.');
  if (lastError) {
    console.warn(`Last Neon error: ${lastError.code || lastError.message}`);
  }
  await loadStore();
}

function isOnline() {
  return online;
}

function makeDefaultProgress(playerId) {
  return {
    player_id: playerId,
    coins: 10000,
    player_fatigue: 0.0,
    current_day: 1,
    game_time: 6.0,
    current_map: 'world/world.tmx',
    // use -1.0 as sentinel to indicate "no saved position" so client
    // will use map spawn logic instead of forcing (0,0) which may be invalid
    player_x: -1.0,
    player_y: -1.0,
    total_coins_earned: 0,
    world_bridge_repaired: false,
    world_pusat_bridge1_repaired: false,
    world_pusat_bridge2_repaired: false,
    house_final_repaired: false,
    world_chest_claimed: false,
    world_pusat_chest1_claimed: false,
    world_pusat_chest2_claimed: false,
    consumed_count: 0,
    sold_count: 0,
    world_coin_collected: false,
    updated_at: new Date().toISOString(),
  };
}

async function ensureProgressSchema() {
  if (!online || !pool) return;
  await pool.query('ALTER TABLE player_progress ADD COLUMN IF NOT EXISTS world_bridge_repaired BOOLEAN DEFAULT FALSE');
  await pool.query('ALTER TABLE player_progress ADD COLUMN IF NOT EXISTS world_pusat_bridge1_repaired BOOLEAN DEFAULT FALSE');
  await pool.query('ALTER TABLE player_progress ADD COLUMN IF NOT EXISTS world_pusat_bridge2_repaired BOOLEAN DEFAULT FALSE');
  await pool.query('ALTER TABLE player_progress ADD COLUMN IF NOT EXISTS house_final_repaired BOOLEAN DEFAULT FALSE');
  await pool.query('ALTER TABLE player_progress ADD COLUMN IF NOT EXISTS world_chest_claimed BOOLEAN DEFAULT FALSE');
  await pool.query('ALTER TABLE player_progress ADD COLUMN IF NOT EXISTS world_pusat_chest1_claimed BOOLEAN DEFAULT FALSE');
  await pool.query('ALTER TABLE player_progress ADD COLUMN IF NOT EXISTS world_pusat_chest2_claimed BOOLEAN DEFAULT FALSE');
  await pool.query('ALTER TABLE player_progress ADD COLUMN IF NOT EXISTS consumed_count INT DEFAULT 0');
  await pool.query('ALTER TABLE player_progress ADD COLUMN IF NOT EXISTS sold_count INT DEFAULT 0');
  await pool.query('ALTER TABLE player_progress ADD COLUMN IF NOT EXISTS world_coin_collected BOOLEAN DEFAULT FALSE');
}

function makeDefaultInventory(playerId) {
  return [
    { id: randomUUID(), player_id: playerId, item_type: 'seed_bit', quantity: 2 },
    { id: randomUUID(), player_id: playerId, item_type: 'seed_wheat', quantity: 2 },
    { id: randomUUID(), player_id: playerId, item_type: 'harvest_bit', quantity: 0 },
    { id: randomUUID(), player_id: playerId, item_type: 'harvest_wheat', quantity: 0 },
    { id: randomUUID(), player_id: playerId, item_type: 'key', quantity: 0 },
  ];
}

async function ensureDefaultInventoryForUser(userId) {
  if (online) {
    const res = await pool.query('SELECT COUNT(*)::int AS count FROM player_inventory WHERE player_id = $1', [userId]);
    const count = res.rows[0]?.count || 0;
    if (count === 0) {
      const defaults = makeDefaultInventory(userId);
      for (const item of defaults) {
        await pool.query(
          'INSERT INTO player_inventory (player_id, item_type, quantity) VALUES ($1, $2, $3)',
          [item.player_id, item.item_type, item.quantity]
        );
      }
    }
    return;
  }

  const hasAny = store.player_inventory.some((item) => item.player_id === userId);
  if (!hasAny) {
    store.player_inventory.push(...makeDefaultInventory(userId));
    await saveStore();
  }
}

async function createUser(username, password_hash) {
  if (online) {
    const res = await pool.query(
      'INSERT INTO users (username, password_hash) VALUES ($1, $2) RETURNING *',
      [username, password_hash]
    );
    const user = res.rows[0];
    // Insert initial progress with unset position sentinel (-1.0)
    await pool.query(
      `INSERT INTO player_progress 
       (player_id, coins, player_fatigue, current_day, game_time, current_map, player_x, player_y, total_coins_earned,
        consumed_count, sold_count, world_coin_collected, world_bridge_repaired, world_pusat_bridge1_repaired, world_pusat_bridge2_repaired, house_final_repaired,
        world_chest_claimed, world_pusat_chest1_claimed, world_pusat_chest2_claimed)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19)`,
      [
        user.id,
        10000,
        0.0,
        1,
        6.0,
        'world/world.tmx',
        -1.0,
        -1.0,
        0,
        0,
        0,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
      ]
    );
    await ensureDefaultInventoryForUser(user.id);
    return user;
  }

  const exists = store.users.find((u) => u.username === username);
  if (exists) throw new Error('Username taken');
  const user = {
    id: randomUUID(),
    username,
    password_hash,
    created_at: new Date().toISOString(),
  };
  store.users.push(user);
  store.player_progress.push(makeDefaultProgress(user.id));
  store.player_inventory.push(...makeDefaultInventory(user.id));
  await saveStore();
  return user;
}

async function getUserByUsername(username) {
  if (online) {
    const res = await pool.query('SELECT * FROM users WHERE username = $1', [username]);
    return res.rows[0];
  }
  return store.users.find((u) => u.username === username) || null;
}

async function getProgress(playerId) {
  if (online) {
    const res = await pool.query('SELECT * FROM player_progress WHERE player_id = $1', [playerId]);
    return res.rows[0] || null;
  }
  return store.player_progress.find((p) => p.player_id === playerId) || null;
}

async function upsertProgress(playerId, progress) {
  console.log(`[db] upsertProgress called for ${playerId} with payload:`, progress);
  const current = (await getProgress(playerId)) || makeDefaultProgress(playerId);
  const next = {
    ...current,
    player_id: playerId,
    coins: Number(progress?.coins ?? current.coins ?? 10000),
    player_fatigue: Number(progress?.player_fatigue ?? current.player_fatigue ?? 0.0),
    current_day: Number(progress?.current_day ?? current.current_day ?? 1),
    game_time: Number(progress?.game_time ?? current.game_time ?? 6.0),
    current_map: String(progress?.current_map ?? current.current_map ?? 'world/world.tmx'),
    player_x: Number(progress?.player_x ?? current.player_x ?? 0.0),
    player_y: Number(progress?.player_y ?? current.player_y ?? 0.0),
    total_coins_earned: Number(progress?.total_coins_earned ?? current.total_coins_earned ?? 0),
    consumed_count: Number(progress?.consumed_count ?? current.consumed_count ?? 0),
    sold_count: Number(progress?.sold_count ?? current.sold_count ?? 0),
    world_coin_collected: Boolean(progress?.world_coin_collected ?? current.world_coin_collected ?? false),
    world_bridge_repaired: Boolean(progress?.world_bridge_repaired ?? current.world_bridge_repaired ?? false),
    world_pusat_bridge1_repaired: Boolean(progress?.world_pusat_bridge1_repaired ?? current.world_pusat_bridge1_repaired ?? false),
    world_pusat_bridge2_repaired: Boolean(progress?.world_pusat_bridge2_repaired ?? current.world_pusat_bridge2_repaired ?? false),
    house_final_repaired: Boolean(progress?.house_final_repaired ?? current.house_final_repaired ?? false),
    world_chest_claimed: Boolean(progress?.world_chest_claimed ?? current.world_chest_claimed ?? false),
    world_pusat_chest1_claimed: Boolean(progress?.world_pusat_chest1_claimed ?? current.world_pusat_chest1_claimed ?? false),
    world_pusat_chest2_claimed: Boolean(progress?.world_pusat_chest2_claimed ?? current.world_pusat_chest2_claimed ?? false),
    updated_at: new Date().toISOString(),
  };

  if (online) {
    const res = await pool.query(
      `INSERT INTO player_progress
        (player_id, coins, player_fatigue, current_day, game_time, current_map, player_x, player_y, total_coins_earned,
           consumed_count, sold_count, world_coin_collected, world_bridge_repaired, world_pusat_bridge1_repaired, world_pusat_bridge2_repaired, house_final_repaired,
           world_chest_claimed, world_pusat_chest1_claimed, world_pusat_chest2_claimed, updated_at)
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19, CURRENT_TIMESTAMP)
       ON CONFLICT (player_id)
       DO UPDATE SET
           consumed_count = EXCLUDED.consumed_count,
           sold_count = EXCLUDED.sold_count,
           world_coin_collected = EXCLUDED.world_coin_collected,
         coins = EXCLUDED.coins,
         player_fatigue = EXCLUDED.player_fatigue,
         current_day = EXCLUDED.current_day,
         game_time = EXCLUDED.game_time,
         current_map = EXCLUDED.current_map,
         player_x = EXCLUDED.player_x,
         player_y = EXCLUDED.player_y,
         total_coins_earned = EXCLUDED.total_coins_earned,
         world_bridge_repaired = EXCLUDED.world_bridge_repaired,
         world_pusat_bridge1_repaired = EXCLUDED.world_pusat_bridge1_repaired,
         world_pusat_bridge2_repaired = EXCLUDED.world_pusat_bridge2_repaired,
         house_final_repaired = EXCLUDED.house_final_repaired,
         world_chest_claimed = EXCLUDED.world_chest_claimed,
         world_pusat_chest1_claimed = EXCLUDED.world_pusat_chest1_claimed,
         world_pusat_chest2_claimed = EXCLUDED.world_pusat_chest2_claimed,
         updated_at = CURRENT_TIMESTAMP
       RETURNING *`,
      [
        next.player_id,
        next.coins,
        next.player_fatigue,
        next.current_day,
        next.game_time,
        next.current_map,
        next.player_x,
        next.player_y,
        next.total_coins_earned,
        next.consumed_count,
        next.sold_count,
        next.world_coin_collected,
        next.world_bridge_repaired,
        next.world_pusat_bridge1_repaired,
        next.world_pusat_bridge2_repaired,
        next.house_final_repaired,
        next.world_chest_claimed,
        next.world_pusat_chest1_claimed,
        next.world_pusat_chest2_claimed,
      ]
    );
    console.log(`[db] upsertProgress stored (online) for ${playerId}:`, res.rows[0]);
    return res.rows[0];
  }

  const idx = store.player_progress.findIndex((p) => p.player_id === playerId);
  if (idx >= 0) store.player_progress[idx] = next;
  else store.player_progress.push(next);
  await saveStore();
  console.log(`[db] upsertProgress stored (local) for ${playerId}:`, next);
  return next;
}

async function getInventory(playerId) {
  if (online) {
    const res = await pool.query(
      'SELECT id, player_id, item_type, quantity FROM player_inventory WHERE player_id = $1 ORDER BY item_type ASC',
      [playerId]
    );
    if (!res.rows || res.rows.length === 0) {
      await ensureDefaultInventoryForUser(playerId);
      const retry = await pool.query(
        'SELECT id, player_id, item_type, quantity FROM player_inventory WHERE player_id = $1 ORDER BY item_type ASC',
        [playerId]
      );
      return retry.rows;
    }
    return res.rows;
  }
  const rows = store.player_inventory.filter((i) => i.player_id === playerId);
  if (rows.length === 0) {
    await ensureDefaultInventoryForUser(playerId);
    return store.player_inventory.filter((i) => i.player_id === playerId);
  }
  return rows;
}

async function replaceInventory(playerId, items) {
  if (online) {
    const client = await pool.connect();
    try {
      await client.query('BEGIN');
      await client.query('DELETE FROM player_inventory WHERE player_id = $1', [playerId]);
      const saved = [];
      for (const item of items || []) {
        if (!item || !item.item_type) continue;
        const quantity = Number.isFinite(Number(item.quantity)) ? Number(item.quantity) : 0;
        const res = await client.query(
          `INSERT INTO player_inventory (player_id, item_type, quantity)
           VALUES ($1, $2, $3)
           RETURNING id, player_id, item_type, quantity`,
          [playerId, item.item_type, quantity]
        );
        saved.push(res.rows[0]);
      }
      await client.query('COMMIT');
      return saved;
    } catch (err) {
      await client.query('ROLLBACK');
      throw err;
    } finally {
      client.release();
    }
  }

  store.player_inventory = store.player_inventory.filter((i) => i.player_id !== playerId);
  const saved = [];
  for (const item of items || []) {
    if (!item || !item.item_type) continue;
    const quantity = Number.isFinite(Number(item.quantity)) ? Number(item.quantity) : 0;
    const row = { id: randomUUID(), player_id: playerId, item_type: item.item_type, quantity };
    store.player_inventory.push(row);
    saved.push(row);
  }
  await saveStore();
  return saved;
}

async function upsertInventoryItem(playerId, itemType, quantity) {
  if (online) {
    const res = await pool.query(
      `INSERT INTO player_inventory (player_id, item_type, quantity)
       VALUES ($1, $2, $3)
       ON CONFLICT (player_id, item_type)
       DO UPDATE SET quantity = EXCLUDED.quantity
       RETURNING id, player_id, item_type, quantity`,
      [playerId, itemType, quantity]
    );
    return res.rows[0];
  }

  const idx = store.player_inventory.findIndex((i) => i.player_id === playerId && i.item_type === itemType);
  const row = idx >= 0
    ? { ...store.player_inventory[idx], quantity }
    : { id: randomUUID(), player_id: playerId, item_type: itemType, quantity };
  if (idx >= 0) store.player_inventory[idx] = row;
  else store.player_inventory.push(row);
  await saveStore();
  return row;
}

async function query() {
  if (!online || !pool) {
    throw new Error('Raw query is not available in fallback mode.');
  }
  return pool.query(...arguments);
}

async function getClient() {
  if (!online) throw new Error('Client connection is not available in fallback mode.');
  return pool.connect();
}

module.exports = {
  init,
  isOnline,
  query,
  getClient,
  createUser,
  getUserByUsername,
  getProgress,
  upsertProgress,
  getInventory,
  replaceInventory,
  upsertInventoryItem,
};
