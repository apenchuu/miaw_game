-- Initialize UUID extension and tables
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS player_progress (
    player_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    coins INT DEFAULT 10000,
    player_fatigue REAL DEFAULT 0.0,
    current_day INT DEFAULT 1,
    game_time REAL DEFAULT 6.0,
    current_map VARCHAR(100) DEFAULT 'world/world.tmx',
    player_x REAL DEFAULT 0.0,
    player_y REAL DEFAULT 0.0,
    total_coins_earned INT DEFAULT 0,
    world_bridge_repaired BOOLEAN DEFAULT FALSE,
    world_pusat_bridge1_repaired BOOLEAN DEFAULT FALSE,
    world_pusat_bridge2_repaired BOOLEAN DEFAULT FALSE,
    house_final_repaired BOOLEAN DEFAULT FALSE,
    world_chest_claimed BOOLEAN DEFAULT FALSE,
    world_pusat_chest1_claimed BOOLEAN DEFAULT FALSE,
    world_pusat_chest2_claimed BOOLEAN DEFAULT FALSE,
    consumed_count INT DEFAULT 0,
    sold_count INT DEFAULT 0,
    world_coin_collected BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS player_inventory (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    player_id UUID REFERENCES users(id) ON DELETE CASCADE,
    item_type VARCHAR(50) NOT NULL,
    quantity INT DEFAULT 0,
    CONSTRAINT unique_player_item UNIQUE (player_id, item_type)
);

CREATE TABLE IF NOT EXISTS opened_chests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    player_id UUID REFERENCES users(id) ON DELETE CASCADE,
    chest_kind VARCHAR(20) NOT NULL,
    chest_x INT NOT NULL,
    chest_y INT NOT NULL,
    opened_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_player_chest UNIQUE (player_id, chest_kind, chest_x, chest_y)
);
