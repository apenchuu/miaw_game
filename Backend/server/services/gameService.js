async function getProgress(playerId) {
  const db = require('../db');
  return db.getProgress(playerId);
}

async function upsertProgress(playerId, progress) {
  const {
    coins = 10000,
    player_fatigue = 0.0,
    current_day = 1,
    game_time = 6.0,
    current_map = 'world/world.tmx',
    player_x = 0.0,
    player_y = 0.0,
    total_coins_earned = 0,
    consumed_count = 0,
    sold_count = 0,
    world_bridge_repaired = false,
    world_pusat_bridge1_repaired = false,
    world_pusat_bridge2_repaired = false,
    house_final_repaired = false,
    world_chest_claimed = false,
    world_pusat_chest1_claimed = false,
    world_pusat_chest2_claimed = false,
    world_coin_collected = false,
  } = progress || {};

  const db = require('../db');
  return db.upsertProgress(playerId, {
    coins,
    player_fatigue,
    current_day,
    game_time,
    current_map,
    player_x,
    player_y,
    total_coins_earned,
    consumed_count,
    sold_count,
    world_bridge_repaired,
    world_pusat_bridge1_repaired,
    world_pusat_bridge2_repaired,
    house_final_repaired,
    world_chest_claimed,
    world_pusat_chest1_claimed,
    world_pusat_chest2_claimed,
    world_coin_collected,
  });
}

async function getInventory(playerId) {
  const db = require('../db');
  return db.getInventory(playerId);
}

async function replaceInventory(playerId, items) {
  const db = require('../db');
  return db.replaceInventory(playerId, items);
}

async function upsertInventoryItem(playerId, itemType, quantity) {
  const db = require('../db');
  return db.upsertInventoryItem(playerId, itemType, quantity);
}

module.exports = {
  getProgress,
  upsertProgress,
  getInventory,
  replaceInventory,
  upsertInventoryItem,
};
