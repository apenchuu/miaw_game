const gameService = require('../services/gameService');

async function getProgress(req, res) {
  try {
    const { playerId } = req.params;
    const progress = await gameService.getProgress(playerId);
    if (!progress) return res.status(404).json({ error: 'Progress not found' });
    return res.json(progress);
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: 'Internal error' });
  }
}

async function saveProgress(req, res) {
  try {
    const { playerId } = req.params;
    const progress = await gameService.upsertProgress(playerId, req.body);
    return res.json(progress);
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: 'Internal error' });
  }
}

async function getInventory(req, res) {
  try {
    const { playerId } = req.params;
    const inventory = await gameService.getInventory(playerId);
    return res.json(inventory);
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: 'Internal error' });
  }
}

async function saveInventory(req, res) {
  try {
    const { playerId } = req.params;
    const { items = [] } = req.body;
    const inventory = await gameService.replaceInventory(playerId, items);
    return res.json(inventory);
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: 'Internal error' });
  }
}

async function saveInventoryItem(req, res) {
  try {
    const { playerId } = req.params;
    const { item_type, quantity = 0 } = req.body;
    if (!item_type) return res.status(400).json({ error: 'item_type is required' });
    const item = await gameService.upsertInventoryItem(playerId, item_type, Number(quantity));
    return res.json(item);
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: 'Internal error' });
  }
}

module.exports = {
  getProgress,
  saveProgress,
  getInventory,
  saveInventory,
  saveInventoryItem,
};
