const express = require('express');
const router = express.Router();
const gameController = require('../controllers/gameController');

router.get('/progress/:playerId', gameController.getProgress);
router.put('/progress/:playerId', gameController.saveProgress);
router.get('/inventory/:playerId', gameController.getInventory);
router.put('/inventory/:playerId', gameController.saveInventory);
router.post('/inventory/:playerId/item', gameController.saveInventoryItem);

module.exports = router;
