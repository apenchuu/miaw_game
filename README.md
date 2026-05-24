# CASLAB – Cozy 2D Farming Adventure

## Deskripsi
CASLAB adalah game 2D cozy bertema farming dan eksplorasi. Tujuan utama pemain adalah menanam seed yang diberikan di awal permainan, menyiram tanaman tepat waktu, lalu memanen dan menjual hasil panen untuk mendapatkan coin.

Tantangan utama ada pada manajemen waktu dan progres:
- Jika telat menyiram, tanaman akan layu dan hilang.
- Coin dibutuhkan untuk memperbaiki infrastruktur seperti jembatan dan rumah rusak.
- Di world tersedia chest tersembunyi untuk mempercepat progres (coin + key rahasia).

Ending game akan muncul ketika pemain berhasil menyelesaikan seluruh objektif infrastruktur dan progres utama.

## Fitur Utama
- Menanam seed awal permainan.
- Menyiram dan harvest tanaman.
- Consume hasil panen.
- Trading item (buy/sell).
- Membuka chest untuk reward.
- Memperbaiki jembatan rusak.
- Memperbaiki rumah rusak (`house_final` -> `house_final_fix`).
- Sistem door interaktif (`F`) termasuk di world pusat/house final.
- Sleep system (ganti hari + reset fatigue).
- Sistem kelelahan (fatigue) yang memengaruhi aktivitas.
- Integrasi backend login/register + sinkronisasi progres.

## Alur Gameplay Singkat
1. Login/Register akun.
2. Mulai farming dari seed awal.
3. Siram tanaman sebelum layu.
4. Panen dan jual hasil untuk menambah coin.
5. Buka chest untuk bonus coin/key.
6. Gunakan coin untuk repair bridge/house.
7. Selesaikan seluruh objektif untuk memunculkan GUI game completed.

## Kontrol (Cara Main)
### Movement
- `W A S D` / Arrow keys: gerak.

### Aksi utama
- Klik kanan mouse: interaksi kontekstual (plant / water / harvest / chest).
- `E`: ganti seed aktif.
- `1..9`: pilih slot hotbar.
- `C`: 
	- claim reward chest (saat popup reward terbuka), atau
	- consume item panen (saat popup chest tidak aktif).
- `F`: buka/tutup door (dekat pintu).
- `G`: repair bridge atau house final (jika dekat dan coin cukup).
- `X`: sleep (hanya saat dekat bed, waktu >= 21:00 atau fatigue penuh).

### Trading & UI
- `H`: buka/tutup menu buy (dekat selling place).
- `J`: buka/tutup menu sell (dekat selling place).
- `ESC`: tutup menu aktif.

### Login/Auth
- `L`: mode login.
- `R`: mode register.
- `TAB`: pindah field username/password.
- `ENTER`: submit auth.

## Kondisi Completion
GUI Completed akan muncul jika semua kondisi true:
- `world_bridge_repaired`
- `world_pusat_bridge1_repaired`
- `world_pusat_bridge2_repaired`
- `house_final_repaired`
- `world_chest_claimed`
- `world_pusat_chest1_claimed`
- `world_pusat_chest2_claimed`
- `world_coin_collected`

Saat completion tercapai, game menampilkan statistik seperti:
- lama bermain/day progress,
- total coin yang didapat,
- total harvest,
- consumed/sold item.

## Struktur Project
```text
CASLAB/
├── Backend/
│   └── server/
│       ├── controllers/
│       ├── db/
│       ├── routes/
│       └── index.js
├── Game/
│   ├── core/
│   ├── lwjgl3/
│   └── assets/
└── README.md
```

## Cara Menjalankan

### 1) Jalankan Backend
Prerequisite:
- Node.js 18+ (disarankan)
- NPM

Masuk ke folder backend:
```bash
cd Backend/server
npm install
```

Run mode development:
```bash
npm run dev
```

Atau production:
```bash
npm start
```

Secara default backend listen di port `4000`.

Catatan DB:
- Jika environment PostgreSQL/Neon tersedia, backend akan pakai PostgreSQL.
- Jika tidak tersedia, backend fallback ke storage lokal `db/store.json`.

### 2) Jalankan Game (LibGDX)
Prerequisite:
- Java 17

Masuk ke folder game:
```bash
cd Game
./gradlew lwjgl3:run
```

Build:
```bash
./gradlew build
```

## Backend Routes
Base URL default: `http://localhost:4000`

### Auth
- `POST /auth/register`
- `POST /auth/login`

Body (register/login):
```json
{
	"username": "player1",
	"password": "secret123"
}
```

### Game Progress
- `GET /game/progress/:playerId` -> ambil progres
- `PUT /game/progress/:playerId` -> simpan progres

Contoh body `PUT /game/progress/:playerId`:
```json
{
	"coins": 12000,
	"player_fatigue": 10,
	"current_day": 3,
	"game_time": 14.5,
	"current_map": "world/world_pusat.tmx",
	"player_x": 12.5,
	"player_y": 7.5,
	"total_coins_earned": 3000,
	"consumed_count": 2,
	"sold_count": 8,
	"world_coin_collected": true,
	"world_bridge_repaired": true,
	"world_pusat_bridge1_repaired": true,
	"world_pusat_bridge2_repaired": true,
	"house_final_repaired": true,
	"world_chest_claimed": true,
	"world_pusat_chest1_claimed": true,
	"world_pusat_chest2_claimed": true
}
```

### Game Inventory
- `GET /game/inventory/:playerId` -> ambil inventory
- `PUT /game/inventory/:playerId` -> replace seluruh inventory
- `POST /game/inventory/:playerId/item` -> upsert 1 item

Contoh body `PUT /game/inventory/:playerId`:
```json
{
	"items": [
		{ "item_type": "seed_bit", "quantity": 5 },
		{ "item_type": "seed_wheat", "quantity": 2 },
		{ "item_type": "harvest_bit", "quantity": 10 },
		{ "item_type": "harvest_wheat", "quantity": 4 },
		{ "item_type": "key", "quantity": 1 }
	]
}
```

Contoh body `POST /game/inventory/:playerId/item`:
```json
{
	"item_type": "seed_bit",
	"quantity": 7
}
```