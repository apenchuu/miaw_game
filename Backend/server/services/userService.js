async function createUser(username, password_hash) {
  const db = require('../db');
  return db.createUser(username, password_hash);
}

async function getUserByUsername(username) {
  const db = require('../db');
  return db.getUserByUsername(username);
}

module.exports = { createUser, getUserByUsername };
