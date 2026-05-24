const userService = require('../services/userService');
const gameService = require('../services/gameService');
const bcrypt = require('bcrypt');

async function register(req, res) {
  try {
    const { username, password } = req.body;
    if (!username || !password) return res.status(400).json({ error: 'Missing fields' });
    const existing = await userService.getUserByUsername(username);
    if (existing) return res.status(409).json({ error: 'Username taken' });
    const password_hash = await bcrypt.hash(password, 10);
    const user = await userService.createUser(username, password_hash);
    
    // Load initial player progress and inventory
    const progress = await gameService.getProgress(user.id);
    const inventory = await gameService.getInventory(user.id);
    
    return res.json({
      id: user.id,
      username: user.username,
      progress,
      inventory,
    });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: 'Internal error' });
  }
}

async function login(req, res) {
  try {
    const { username, password } = req.body;
    if (!username || !password) return res.status(400).json({ error: 'Missing fields' });
    const user = await userService.getUserByUsername(username);
    if (!user) return res.status(401).json({ error: 'Invalid credentials' });
    const match = await bcrypt.compare(password, user.password_hash);
    if (!match) return res.status(401).json({ error: 'Invalid credentials' });
    
    // Load player progress and inventory
    const progress = await gameService.getProgress(user.id);
    const inventory = await gameService.getInventory(user.id);
    
    return res.json({
      id: user.id,
      username: user.username,
      progress,
      inventory,
    });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: 'Internal error' });
  }
}

module.exports = { register, login };
