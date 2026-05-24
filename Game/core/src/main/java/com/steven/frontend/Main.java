package com.steven.frontend;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import com.steven.frontend.patterns.builder.ProgressPayloadBuilder;
import com.steven.frontend.patterns.command.Invoker;
import com.steven.frontend.patterns.command.RepairBridgeCommand;
import com.steven.frontend.patterns.command.RepairPusatBridge1Command;
import com.steven.frontend.patterns.command.RepairPusatBridge2Command;
import com.steven.frontend.patterns.facade.BackendFacade;
import com.steven.frontend.patterns.factory.UIFactory;
import com.steven.frontend.patterns.observer.GameEventManager;
import com.steven.frontend.patterns.singleton.GameConfig;
import com.steven.frontend.patterns.strategy.DefaultMovementStrategy;
import com.steven.frontend.patterns.strategy.MovementStrategy;
import com.steven.frontend.features.PlantFeature;
import com.steven.frontend.features.PlantFeature.Plant;
import com.steven.frontend.features.PlantFeature.PlantSprite;
import com.steven.frontend.features.ChestFeature;
import com.steven.frontend.features.ChestFeature.ChestKind;
import com.steven.frontend.features.ChestFeature.ChestSpot;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class Main extends ApplicationAdapter {
    public enum Facing { DOWN, LEFT, RIGHT, UP }
    public enum AuthField { USERNAME, PASSWORD }

    // Untuk menyusun ulang pemetaan hotbar berdasarkan isi inventory saat ini.
    public void rebuildHotbarMapping() {
        for (int i = 0; i < hotbarMapping.length; i++) hotbarMapping[i] = null;
        hotbarMapping[0] = "tool_water";

        java.util.List<String> items = new java.util.ArrayList<String>();
        if (inventory.bitSeeds > 0) items.add("seed_bit");
        if (inventory.wheatSeeds > 0) items.add("seed_wheat");
        if (inventory.harvestedBit > 0) items.add("harvest_bit");
        if (inventory.harvestedWheat > 0) items.add("harvest_wheat");
        if (inventory.keys > 0) items.add("key");

        int slot = 0;
        for (String it : items) {
            // skip reserved slot 0 if encountered
            while (slot == 0) slot++;
            if (slot >= hotbarMapping.length) break;
            hotbarMapping[slot++] = it;
        }
    }
        
    public static class Inventory {
        public int bitSeeds = 2;
        public int wheatSeeds = 2;
        public int harvestedBit = 0;
        public int harvestedWheat = 0;
        public int keys = 0;
        public int coins = 10000;
        
        public boolean plantBit() {
            if (bitSeeds > 0) {
                bitSeeds--;
                return true;
            }
            return false;
        }
        
        public boolean plantWheat() {
            if (wheatSeeds > 0) {
                wheatSeeds--;
                return true;
            }
            return false;
        }
        
        public void addBit() { bitSeeds++; }
        public void addWheat() { wheatSeeds++; }
        
        public void collectBit() { harvestedBit++; }
        public void collectWheat() { harvestedWheat++; }
        public void addKey() { keys++; }

        public int sellBitItems() {
            int sold = harvestedBit;
            coins += sold * 30;
            harvestedBit = 0;
            return sold;
        }

        public int sellWheatItems() {
            int sold = harvestedWheat;
            coins += sold * 40;
            harvestedWheat = 0;
            return sold;
        }

        public boolean hasSellableItems() {
            return harvestedBit > 0 || harvestedWheat > 0;
        }

        public void sellHarvested() {
            coins += harvestedBit * 30;
            coins += harvestedWheat * 40;
            harvestedBit = 0;
            harvestedWheat = 0;
        }
    }

    public OrthographicCamera camera;
    public SpriteBatch batch;
    public TiledMap map;
    public OrthogonalTiledMapRenderer mapRenderer;
    
    public TiledMapTileLayer grassLayer, waterLayer, spawnLayer, houseFloorLayer, doorsCloseLayer, doorsHalfLayer, doorsOpenLayer;
    public TiledMapTileLayer bedLayer, houseObjectLayer, kesetLayer, houseLineLayer, treeLayer, rockLayer;
    public TiledMapTileLayer sellingPlaceLayer, bridgeLayer, chestLayer, toKotaLayer, toHomeLayer, spawnKotaLayer, spawnHomeLayer, coinLayer;
    public TiledMapTileLayer houseFinalLayer, houseFinalFixLayer, houseFinalFurnitureLayer, houseFinalBedLayer;
    public TiledMapTileLayer houseFinalDoorCloseLayer, houseFinalDoorHalfLayer, houseFinalDoorOpenLayer;
    public com.badlogic.gdx.maps.tiled.TiledMapTileLayer[] plantAreaLayers = com.steven.frontend.features.PlantFeature.plantAreaLayers;
    
    // Backup of original plant_area layer cells (delegated to PlantFeature)
    public com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell[][][] plantAreaCellsBackup = com.steven.frontend.features.PlantFeature.plantAreaCellsBackup;
    
    // Global plant stage layers (bit1-4, bitdone, gandum1-4, gandumdone)
    public com.badlogic.gdx.maps.tiled.TiledMapTileLayer[] bitStageLayers = com.steven.frontend.features.PlantFeature.bitStageLayers;
    public com.badlogic.gdx.maps.tiled.TiledMapTileLayer[] wheatStageLayers = com.steven.frontend.features.PlantFeature.wheatStageLayers;
    public int[] plantAreaMinX = com.steven.frontend.features.PlantFeature.plantAreaMinX;
    public int[] plantAreaMaxX = com.steven.frontend.features.PlantFeature.plantAreaMaxX;
    public int[] plantAreaMinY = com.steven.frontend.features.PlantFeature.plantAreaMinY;
    public int[] plantAreaMaxY = com.steven.frontend.features.PlantFeature.plantAreaMaxY;
    public int[] plantAreaWidthTiles = com.steven.frontend.features.PlantFeature.plantAreaWidthTiles;
    public int[] plantAreaHeightTiles = com.steven.frontend.features.PlantFeature.plantAreaHeightTiles;
    public com.badlogic.gdx.graphics.g2d.BitmapFont font;
    public Texture uiPanelTexture;
    public java.util.List<PlantSprite> plantSpritesToRender = com.steven.frontend.features.PlantFeature.plantSpritesToRender;
    public Texture overlayTexture;

    public Texture sheetTexture;
    public Texture seedTexture;
    public Texture toolsTexture;
    public Texture harvestedBitTexture;
    public Texture harvestedWheatTexture;
    public Texture coinTexture;
    public Texture frontChestTexture;
    public Texture rightChestTexture;
    public Texture keyTexture;
    public Texture waterTexture;
    public Texture buyLayoutTexture;
    public Texture sellLayoutTexture;
    public Texture chestRewardLayoutTexture;
    public Texture chestRewardLayoutWp1Texture;
    public Texture chestRewardLayoutWp2Texture;
    public Music backsoundMusic;
    
    // Login form textures
    public Texture loginFormTexture;
    public Texture usernameFormTexture;
    public Texture passwordFormTexture;
    public Texture loginButtonTexture;
    public Texture registerButtonTexture;
    
    public TextureRegion seedWheatRegion, seedBitRegion;
    public TextureRegion toolBox3Region;
    public TextureRegion harvestedWheatRegion, harvestedBitRegion;
    public TextureRegion keyRegion;
    public Animation<TextureRegion> coinAnimation;
    public Animation<TextureRegion> chestAnimationFront;
    public Animation<TextureRegion> chestAnimationRight;
    public float coinAnimTime = 0f;
    public float chestAnimTime = 0f;
    public int mapWidth, mapHeight;
    public Animation<TextureRegion> animDown, animLeft, animRight, animUp;
    public Animation<TextureRegion> waterAnimDown, waterAnimLeft, waterAnimRight, waterAnimUp;
    public Animation<TextureRegion> currentAnim;
    public Facing lastFacing = Facing.DOWN;
    public Facing wateringFacing = Facing.DOWN;
    public boolean wateringActive = false;
    public float wateringAnimTime = 0f;
    public float stateTime;

    public float x, y;
    public final float speed = 3f;
    
    // Bridge repair layers and states
    public TiledMapTileLayer bridgeBenerLayer, bridgeRusak1Layer, bridgeRusak2Layer, bridgeBener1Layer, bridgeBener2Layer;
    public boolean worldBridgeRepaired = false;
    public boolean worldPusatBridge1Repaired = false;
    public boolean worldPusatBridge2Repaired = false;
    public boolean houseFinalRepaired = false;
    public boolean worldCoinCollected = false;
    public int consumedCount = 0;
    public int soldCount = 0;
   
    // Game completed UI
    public Stage gameCompleteStage = null;
    public Skin gameCompleteSkin = null;
    public boolean gameCompletedShown = false;
    // When true, the completed state is locked and progress shouldn't be modified or autosaved.
    public boolean gameCompletedLocked = false;
    
    public Inventory inventory = new Inventory();
    public java.util.List<Plant> plants = com.steven.frontend.features.PlantFeature.plants;
    public int selectedSeed = 0;
    public int selectedSellItem = 0; 
    public int hotbarSelected = -1; 
    public OrthographicCamera uiCamera;
    
    
    public String[] hotbarMapping = new String[9];

    public float sellingPromptTimer = 0f;
    public int sellingPromptTileX = -1, sellingPromptTileY = -1;
    public boolean sellingWasNear = false;

    public float plantPromptTimer = 0f;
    public int plantPromptTileX = -1, plantPromptTileY = -1;
    public boolean plantWasNear = false;

    public float bridgePromptTimer = 0f;
    public int bridgePromptTileX = -1, bridgePromptTileY = -1;
    public boolean bridgeWasNear = false;

    public float houseFinalPromptTimer = 0f;
    public int houseFinalPromptTileX = -1, houseFinalPromptTileY = -1;
    public boolean houseFinalWasNear = false;

    public float chestPromptTimer = 0f;
    public int chestPromptTileX = -1, chestPromptTileY = -1;
    public boolean chestWasNear = false;
    public static final int SEED_BIT_PRICE = 100;
    public static final int SEED_WHEAT_PRICE = 150;
    
    // Day/night cycle
    public float gameTime = 6f; 
    public boolean isSleeping = false;
    public float sleepTimer = 0f; 
    public int currentDay = 1;
    public static final float TIME_SPEED = 0.1f; 
    
    // Fatigue system
    public float playerFatigue = 0f; 
    public static final float MAX_FATIGUE = 100f;
    public static final float MOVE_FATIGUE_RATE = 0.2f; 
    public String currentMapPath = "world/world.tmx";
    public static final float MAP_TRANSITION_COOLDOWN = 0.4f;
    public float mapTransitionCooldown = 0f;

    public static final String DEFAULT_BACKEND_URL = "http://localhost:3000";
    public static final float BACKEND_AUTOSAVE_INTERVAL = 0.75f;
    public final Object backendSyncLock = new Object();
    public String backendBaseUrl = DEFAULT_BACKEND_URL;
    public String backendUsername = "";
    public String backendPassword = "";
    public String backendPlayerId = "";
    public String backendDeviceId = "";
    public boolean progressDirty = false;
    public boolean inventoryDirty = false;
    public boolean backendSaveInFlight = false;
    public boolean backendLoading = false;
    public float backendAutosaveTimer = 0f;
    public long progressChangeSeq = 0L;
    public long inventoryChangeSeq = 0L;
    public long progressSavedSeq = 0L;
    public long inventorySavedSeq = 0L;
    public int totalCoinsEarned = 0;
    public boolean authGateActive = false;
    public String authStatusText = "";
    public final GameEventManager gameEventManager = new GameEventManager();
    public final UIFactory uiFactory = new UIFactory();
    public BackendFacade backendFacade;
    public Invoker invoker;
    public MovementStrategy movementStrategy;

    public enum AuthMode { NONE, LOGIN, REGISTER }
    public AuthMode pendingAuthMode = AuthMode.NONE;
    public AuthField authField = AuthField.USERNAME;
    public String authInputUsername = "";
    public String authInputPassword = "";
    
    // Scene2D UI Components for Login
    public Stage authStage;
    public Skin authSkin;
    public TextField txtUsername;
    public TextField txtPassword;
    public TextButton btnLogin;
    public TextButton btnRegister;
    public Label authModeLabel;
    
    // Login form UI bounds (legacy, kept for compatibility)
    public float authFormX = 100f;
    public float authFormY = 60f;
    public float usernameFieldX = 220f;
    public float usernameFieldY = 300f;
    public float passwordFieldX = 220f;
    public float passwordFieldY = 230f;
    public float loginButtonX = 140f;
    public float loginButtonY = 110f;
    public float registerButtonX = 280f;
    public float registerButtonY = 50f;
    public com.badlogic.gdx.math.Rectangle loginButtonBounds;
    public com.badlogic.gdx.math.Rectangle registerButtonBounds;

    public static final String[] BACKEND_BASE_URL_CANDIDATES = new String[] {
        "http://localhost:3000",
        "http://127.0.0.1:3000",
        "http://localhost:4000",
        "http://127.0.0.1:4000"
    };

    @Override
    public void create() {
        batch = new SpriteBatch();
        backendFacade = new BackendFacade(this::httpRequest, this::loginOrRegister);
        invoker = new Invoker();
        movementStrategy = new DefaultMovementStrategy(this);
        gameEventManager.addListener(() -> Gdx.app.postRunnable(() -> {
            com.steven.frontend.features.UIFeature.initializeGameCompletedUI(this);
            Gdx.input.setInputProcessor(gameCompleteStage);
        }));
        
        com.steven.frontend.features.AuthBackendFeature.initializeLoginUI(this);
        Gdx.input.setInputProcessor(new com.badlogic.gdx.InputMultiplexer(authStage, new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (!authGateActive) return false;

                if (keycode == Input.Keys.ESCAPE) {
                    authStatusText = "Login/register dibatalkan";
                    pendingAuthMode = AuthMode.NONE;
                    authField = AuthField.USERNAME;
                    authInputUsername = "";
                    authInputPassword = "";
                    if (txtUsername != null) txtUsername.setText("");
                    if (txtPassword != null) txtPassword.setText("");
                    com.steven.frontend.features.AuthBackendFeature.updateAuthModeIndicator(Main.this);
                    return true;
                }

                if (keycode == Input.Keys.L) {
                    com.steven.frontend.features.AuthBackendFeature.beginAuth(Main.this, Main.AuthMode.LOGIN);
                    return true;
                }
                if (keycode == Input.Keys.R) {
                    com.steven.frontend.features.AuthBackendFeature.beginAuth(Main.this, Main.AuthMode.REGISTER);
                    return true;
                }
                if (pendingAuthMode == AuthMode.NONE) return true;

                if (keycode == Input.Keys.TAB) {
                    authField = (authField == AuthField.USERNAME) ? AuthField.PASSWORD : AuthField.USERNAME;
                    return true;
                }
                if (keycode == Input.Keys.BACKSPACE) {
                    if (authField == AuthField.USERNAME && authInputUsername.length() > 0) {
                        authInputUsername = authInputUsername.substring(0, authInputUsername.length() - 1);
                    } else if (authField == AuthField.PASSWORD && authInputPassword.length() > 0) {
                        authInputPassword = authInputPassword.substring(0, authInputPassword.length() - 1);
                    }
                    return true;
                }
                if (keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER) {
                    com.steven.frontend.features.AuthBackendFeature.submitAuth(Main.this);
                    return true;
                }
                return true;
            }

            @Override
            public boolean keyTyped(char character) {
                if (!authGateActive || pendingAuthMode == AuthMode.NONE) return false;
                if (character == '\b' || character == '\r' || character == '\n' || character == '\t') return true;
                if (character < 32) return true;

                if (authField == AuthField.USERNAME) {
                    authInputUsername += character;
                } else {
                    authInputPassword += character;
                }
                return true;
            }
            
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (!authGateActive || pendingAuthMode == AuthMode.NONE) return false;
                
                // Convert screen coordinates to UI coordinates
                float uiY = Gdx.graphics.getHeight() - screenY;
                float uiX = screenX;
                
                // Check if clicked on login button
                if (loginButtonBounds != null && loginButtonBounds.contains(uiX, uiY)) {
                    // If not in login mode, switch to login
                    if (pendingAuthMode != AuthMode.LOGIN) {
                        com.steven.frontend.features.AuthBackendFeature.beginAuth(Main.this, Main.AuthMode.LOGIN);
                    } else {
                        // Already in login mode, submit
                        com.steven.frontend.features.AuthBackendFeature.submitAuth(Main.this);
                    }
                    return true;
                }
                
                // Check if clicked on register button
                if (registerButtonBounds != null && registerButtonBounds.contains(uiX, uiY)) {
                    // If not in register mode, switch to register
                    if (pendingAuthMode != AuthMode.REGISTER) {
                        com.steven.frontend.features.AuthBackendFeature.beginAuth(Main.this, Main.AuthMode.REGISTER);
                    } else {
                        // Already in register mode, submit
                        com.steven.frontend.features.AuthBackendFeature.submitAuth(Main.this);
                    }
                    return true;
                }
                
                // Check if clicked on username field
                if (usernameFormTexture != null) {
                    float usernameFieldWidth = usernameFormTexture.getWidth();
                    float usernameFieldHeight = usernameFormTexture.getHeight();
                    if (uiX >= usernameFieldX && uiX < usernameFieldX + usernameFieldWidth &&
                        uiY >= usernameFieldY && uiY < usernameFieldY + usernameFieldHeight) {
                        authField = AuthField.USERNAME;
                        return true;
                    }
                }
                
                // Check if clicked on password field
                if (passwordFormTexture != null) {
                    float passwordFieldWidth = passwordFormTexture.getWidth();
                    float passwordFieldHeight = passwordFormTexture.getHeight();
                    if (uiX >= passwordFieldX && uiX < passwordFieldX + passwordFieldWidth &&
                        uiY >= passwordFieldY && uiY < passwordFieldY + passwordFieldHeight) {
                        authField = AuthField.PASSWORD;
                        return true;
                    }
                }
                
                return false;
            }
        }));
        // Load tiled map
        map = new TmxMapLoader().load("world/world.tmx");
        updateMapDimensionsAndViewport();

        // Determine spawn
        // Load layers
        grassLayer = (TiledMapTileLayer) map.getLayers().get("grass");
        waterLayer = (TiledMapTileLayer) map.getLayers().get("water");
        spawnLayer = (TiledMapTileLayer) map.getLayers().get("spawn");
        houseFloorLayer = (TiledMapTileLayer) map.getLayers().get("house_floor");
        doorsCloseLayer = (TiledMapTileLayer) map.getLayers().get("doors_close");
        doorsHalfLayer = (TiledMapTileLayer) map.getLayers().get("doors_half");
        doorsOpenLayer = (TiledMapTileLayer) map.getLayers().get("doors_open");
        bedLayer = (TiledMapTileLayer) map.getLayers().get("bed");
        houseObjectLayer = (TiledMapTileLayer) map.getLayers().get("house_object");
        kesetLayer = (TiledMapTileLayer) map.getLayers().get("keset");
        houseLineLayer = (TiledMapTileLayer) map.getLayers().get("house_line");
        treeLayer = (TiledMapTileLayer) map.getLayers().get("tree");
        rockLayer = (TiledMapTileLayer) map.getLayers().get("rock");
        houseFinalLayer = (TiledMapTileLayer) map.getLayers().get("house_final");
        houseFinalFixLayer = (TiledMapTileLayer) map.getLayers().get("house_final_fix");
        houseFinalFurnitureLayer = (TiledMapTileLayer) map.getLayers().get("house_final_furniture");
        if (houseFinalFurnitureLayer == null) houseFinalFurnitureLayer = (TiledMapTileLayer) map.getLayers().get("final_house_furniture");
        houseFinalBedLayer = (TiledMapTileLayer) map.getLayers().get("house_final_bed");
        houseFinalDoorCloseLayer = (TiledMapTileLayer) map.getLayers().get("house_final_door_close");
        if (houseFinalDoorCloseLayer == null) houseFinalDoorCloseLayer = (TiledMapTileLayer) map.getLayers().get("house_final_door");
        houseFinalDoorHalfLayer = (TiledMapTileLayer) map.getLayers().get("house_final_door_half");
        houseFinalDoorOpenLayer = (TiledMapTileLayer) map.getLayers().get("house_final_door_open");
        sellingPlaceLayer = (TiledMapTileLayer) map.getLayers().get("selling_place");
        chestLayer = (TiledMapTileLayer) map.getLayers().get("chest");
        bridgeLayer = (TiledMapTileLayer) map.getLayers().get("bridge");
        bridgeBenerLayer = (TiledMapTileLayer) map.getLayers().get("bridge_bener");
        bridgeRusak1Layer = (TiledMapTileLayer) map.getLayers().get("bridge_rusak1");
        bridgeRusak2Layer = (TiledMapTileLayer) map.getLayers().get("bridge_rusak2");
        bridgeBener1Layer = (TiledMapTileLayer) map.getLayers().get("bridge_bener1");
        bridgeBener2Layer = (TiledMapTileLayer) map.getLayers().get("bridge_bener2");
        toKotaLayer = (TiledMapTileLayer) map.getLayers().get("to_kota");
        toHomeLayer = (TiledMapTileLayer) map.getLayers().get("to_home");
        spawnKotaLayer = (TiledMapTileLayer) map.getLayers().get("spawn_kota");
        spawnHomeLayer = (TiledMapTileLayer) map.getLayers().get("spawn_home");
        coinLayer = (TiledMapTileLayer) map.getLayers().get("coin");
        
        
        for (int i = 0; i < 6; i++) {
            plantAreaLayers[i] = (TiledMapTileLayer) map.getLayers().get("plant_area" + (i + 1));
            if (plantAreaLayers[i] != null) {
                // Backup original cells
                int width = plantAreaLayers[i].getWidth();
                int height = plantAreaLayers[i].getHeight();
                plantAreaCellsBackup[i] = new TiledMapTileLayer.Cell[width][height];
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        plantAreaCellsBackup[i][x][y] = plantAreaLayers[i].getCell(x, y);
                    }
                }
                
                // Find the bounds of actual tiles in this area
                int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
                int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
                boolean hasAnyTile = false;
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        if (plantAreaCellsBackup[i][x][y] != null) {
                            minX = Math.min(minX, x);
                            maxX = Math.max(maxX, x);
                            minY = Math.min(minY, y);
                            maxY = Math.max(maxY, y);
                            hasAnyTile = true;
                        }
                    }
                }
                if (hasAnyTile) {
                    plantAreaMinX[i] = minX;
                    plantAreaMaxX[i] = maxX;
                    plantAreaMinY[i] = minY;
                    plantAreaMaxY[i] = maxY;
                    plantAreaWidthTiles[i] = maxX - minX + 1;
                    plantAreaHeightTiles[i] = maxY - minY + 1;
                }
            }
        }
        
        // Load global plant stage layers
        String[] bitNames = {"bit1", "bit2", "bit3", "bit4", "bitdone"};
        String[] wheatNames = {"gandum1", "gandum2", "gandum3", "gandum4", "gandumdone"};
        for (int i = 0; i < 5; i++) {
            bitStageLayers[i] = (TiledMapTileLayer) map.getLayers().get(bitNames[i]);
            wheatStageLayers[i] = (TiledMapTileLayer) map.getLayers().get(wheatNames[i]);
        }
        
        // UI camera for HUD (screen-space, not world-space)
        uiCamera = new OrthographicCamera(640f, 480f);
        uiCamera.position.set(320f, 240f, 0);
        uiCamera.update();
        
        // Font for inventory display
        font = new com.badlogic.gdx.graphics.g2d.BitmapFont();
        try { font.getData().setScale(1.05f); } catch (Exception e) {}

        Pixmap overlayPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        overlayPixmap.setColor(0f, 0f, 0f, 1f);
        overlayPixmap.fill();
        overlayTexture = new Texture(overlayPixmap);
        overlayPixmap.dispose();

        // UI panel texture for auth/login card (1x1 solid, tinted)
        try {
            Pixmap panelPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            panelPixmap.setColor(0.18f, 0.15f, 0.25f, 0.95f);
            panelPixmap.fill();
            uiPanelTexture = new Texture(panelPixmap);
            panelPixmap.dispose();
        } catch (Exception e) {
            uiPanelTexture = null;
        }

        // Set initial door visibility
        com.steven.frontend.features.DoorFeature.setDoorLayersVisible(this, true, false, false);
        com.steven.frontend.features.DoorFeature.doorState = com.steven.frontend.features.DoorFeature.DoorState.CLOSED;
        

        int cx = mapWidth / 2;
        int cy = mapHeight / 2;

        // Spawn from layer "spawn" if available
        spawnLayer = (TiledMapTileLayer) map.getLayers().get("spawn");
        if (spawnLayer != null) {
            boolean foundSpawn = false;
            for (int ty = mapHeight - 1; ty >= 0 && !foundSpawn; ty--) {
                for (int tx = 0; tx < mapWidth && !foundSpawn; tx++) {
                    if (spawnLayer.getCell(tx, ty) != null) {
                        cx = tx;
                        cy = ty;
                        foundSpawn = true;
                    }
                }
            }
        }

        if (grassLayer != null && grassLayer.getCell(cx, cy) == null) {
            boolean found = false;
            int radius = 1;
            while (!found && radius < Math.max(mapWidth, mapHeight)) {
                for (int dx = -radius; dx <= radius && !found; dx++) {
                    for (int dy = -radius; dy <= radius && !found; dy++) {
                        int tx = cx + dx, ty = cy + dy;
                        if (tx < 0 || ty < 0 || tx >= mapWidth || ty >= mapHeight) continue;
                        if (grassLayer.getCell(tx, ty) != null) {
                            cx = tx; cy = ty; found = true; break;
                        }
                    }
                }
                radius++;
            }
        }
        x = cx + 0.5f;
        y = cy + 0.5f;

        int frameW = 16;
        int frameH = 16;
       
        sheetTexture = new Texture(Gdx.files.internal("character/character.png"));
        TextureRegion[][] tiles = TextureRegion.split(sheetTexture, frameW, frameH);
        int[] frameCols = new int[]{1, 4, 7, 10};

        // Mapping row order: down, left, right, up
        animDown = createAnimationFromSheet(tiles, 1, frameCols);
        animLeft = createAnimationFromSheet(tiles, 4, frameCols);
        animRight = createAnimationFromSheet(tiles, 7, frameCols);
        animUp = createAnimationFromSheet(tiles, 10, frameCols);

        Animation<TextureRegion> fallbackAnim = animDown;
        if (animLeft == null) animLeft = fallbackAnim;
        if (animRight == null) animRight = fallbackAnim;
        if (animUp == null) animUp = fallbackAnim;

        // Watering animation uses the A pose only for all directions to avoid
        // direction-specific bugs. Source row is 11 (0-based row 10).
        try {
            waterTexture = new Texture(Gdx.files.internal("character/watering.png"));
            TextureRegion[][] waterTiles = TextureRegion.split(waterTexture, frameW, frameH);
            waterAnimLeft = createAnimationFromSheet(waterTiles, 10, frameCols); // A
            waterAnimRight = waterAnimLeft;
            waterAnimDown = waterAnimLeft;
            waterAnimUp = waterAnimLeft;
        } catch (Exception e) {
            waterTexture = null;
            waterAnimDown = null;
            waterAnimLeft = null;
            waterAnimRight = null;
            waterAnimUp = null;
        }

        // default animation
        currentAnim = animDown != null ? animDown : (animLeft != null ? animLeft : (animRight != null ? animRight : animUp));
        stateTime = 0f;

        try {
            seedTexture = new Texture(Gdx.files.internal("tools/seed.png"));
            TextureRegion[][] seedTiles = TextureRegion.split(seedTexture, 16, 16);
           
            if (seedTiles.length > 0 && seedTiles[0].length > 0) {
                seedWheatRegion = seedTiles[0][0];
            }
            if (seedTiles.length > 1 && seedTiles[1].length > 0) {
                seedBitRegion = seedTiles[1][0];
            }
            if (seedBitRegion == null && seedTiles.length > 0 && seedTiles[0].length > 0) seedBitRegion = seedTiles[0][0];
            if (seedWheatRegion == null && seedTiles.length > 0 && seedTiles[0].length > 0) seedWheatRegion = seedTiles[0][0];
        } catch (Exception e) {
            seedTexture = null;
            seedWheatRegion = null;
            seedBitRegion = null;
        }

        // Load tools icon (tools/tools.png) for hotbar box 3 from first row
        try {
            toolsTexture = new Texture(Gdx.files.internal("tools/tools.png"));
            TextureRegion[][] toolTiles = TextureRegion.split(toolsTexture, 16, 16);
            if (toolTiles.length > 0 && toolTiles[0].length > 0) {
                toolBox3Region = toolTiles[0][0];
            }
        } catch (Exception e) {
            toolsTexture = null;
            toolBox3Region = null;
        }

        // Load harvested item icons from dedicated assets
        try {
            harvestedBitTexture = new Texture(Gdx.files.internal("tools/bit.png"));
            harvestedBitRegion = new TextureRegion(harvestedBitTexture);
        } catch (Exception e) {
            harvestedBitTexture = null;
            harvestedBitRegion = null;
        }

        try {
            harvestedWheatTexture = new Texture(Gdx.files.internal("tools/wheat.png"));
            harvestedWheatRegion = new TextureRegion(harvestedWheatTexture);
        } catch (Exception e) {
            harvestedWheatTexture = null;
            harvestedWheatRegion = null;
        }

        // Load coin animation (tools/coin.png)
        try {
            coinTexture = new Texture(Gdx.files.internal("tools/coin.png"));
            TextureRegion[][] coinTiles = TextureRegion.split(coinTexture, 16, 16);
            TextureRegion[] coinFrames = new TextureRegion[coinTiles.length * coinTiles[0].length];
            int idx = 0;
            for (int r = 0; r < coinTiles.length; r++) {
                for (int c = 0; c < coinTiles[r].length; c++) {
                    coinFrames[idx++] = coinTiles[r][c];
                }
            }
            if (idx > 0) {
                TextureRegion[] trimmedFrames = new TextureRegion[idx];
                System.arraycopy(coinFrames, 0, trimmedFrames, 0, idx);
                coinAnimation = new Animation<>(0.1f, trimmedFrames);
                coinAnimation.setPlayMode(Animation.PlayMode.LOOP);
            }
        } catch (Exception e) {
            coinTexture = null;
            coinAnimation = null;
        }

        // Load buy layout image (layout/buy_layout.png)
        try {
            buyLayoutTexture = new Texture(Gdx.files.internal("layout/buy_layout.png"));
        } catch (Exception e) {
            buyLayoutTexture = null;
        }

        // Load sell layout image (layout/sell_layout.png)
        try {
            sellLayoutTexture = new Texture(Gdx.files.internal("layout/sell_layout.png"));
        } catch (Exception e) {
            sellLayoutTexture = null;
        }

        // Load chest reward layout image
        try {
            chestRewardLayoutTexture = new Texture(Gdx.files.internal("layout/chest_reward.png"));
        } catch (Exception e) {
            chestRewardLayoutTexture = null;
        }

        try {
            chestRewardLayoutWp1Texture = new Texture(Gdx.files.internal("layout/chest_reward_wp1.png"));
        } catch (Exception e) {
            chestRewardLayoutWp1Texture = null;
        }

        try {
            chestRewardLayoutWp2Texture = new Texture(Gdx.files.internal("layout/chest_reward_wp2.png"));
        } catch (Exception e) {
            chestRewardLayoutWp2Texture = null;
        }
        
        // Load login form assets
        try {
            loginFormTexture = new Texture(Gdx.files.internal("login/form_login.png"));
        } catch (Exception e) {
            loginFormTexture = null;
        }
        
        try {
            usernameFormTexture = new Texture(Gdx.files.internal("login/username_form.png"));
        } catch (Exception e) {
            usernameFormTexture = null;
        }
        
        try {
            passwordFormTexture = new Texture(Gdx.files.internal("login/password_form.png"));
        } catch (Exception e) {
            passwordFormTexture = null;
        }
        
        try {
            loginButtonTexture = new Texture(Gdx.files.internal("login/login_button.png"));
        } catch (Exception e) {
            loginButtonTexture = null;
        }
        
        try {
            registerButtonTexture = new Texture(Gdx.files.internal("login/register_button.png"));
        } catch (Exception e) {
            registerButtonTexture = null;
        }
        
        // Initialize button bounds
        loginButtonBounds = new com.badlogic.gdx.math.Rectangle(loginButtonX, loginButtonY, 
            loginButtonTexture != null ? loginButtonTexture.getWidth() : 120, 
            loginButtonTexture != null ? loginButtonTexture.getHeight() : 40);
        registerButtonBounds = new com.badlogic.gdx.math.Rectangle(registerButtonX, registerButtonY, 
            registerButtonTexture != null ? registerButtonTexture.getWidth() : 120, 
            registerButtonTexture != null ? registerButtonTexture.getHeight() : 40);

        // Load chest animation sprite sheets and key icon
        try {
            frontChestTexture = new Texture(Gdx.files.internal("tools/front_chest.png"));
            TextureRegion[][] frontChestTiles = TextureRegion.split(frontChestTexture, 16, 16);
            chestAnimationFront = createAnimationFromSheet(frontChestTiles, 0, new int[]{0, 1, 2, 3});
            if (chestAnimationFront != null) chestAnimationFront.setPlayMode(Animation.PlayMode.NORMAL);

            rightChestTexture = new Texture(Gdx.files.internal("tools/right_chest.png"));
            TextureRegion[][] rightChestTiles = TextureRegion.split(rightChestTexture, 16, 16);
            chestAnimationRight = createAnimationFromSheet(rightChestTiles, 0, new int[]{0, 1, 2, 3});
            if (chestAnimationRight != null) chestAnimationRight.setPlayMode(Animation.PlayMode.NORMAL);
        } catch (Exception e) {
            frontChestTexture = null;
            rightChestTexture = null;
            chestAnimationFront = null;
            chestAnimationRight = null;
        }

        try {
            Pixmap sourcePixmap = new Pixmap(Gdx.files.internal("tools/key.png"));
            Pixmap resizedPixmap = new Pixmap(16, 16, sourcePixmap.getFormat());
            resizedPixmap.setBlending(Pixmap.Blending.None);
            resizedPixmap.drawPixmap(
                sourcePixmap,
                0, 0, sourcePixmap.getWidth(), sourcePixmap.getHeight(),
                0, 0, 16, 16
            );
            keyTexture = new Texture(resizedPixmap);
            keyRegion = new TextureRegion(keyTexture);
            sourcePixmap.dispose();
            resizedPixmap.dispose();
        } catch (Exception e) {
            keyTexture = null;
            keyRegion = null;
        }

        // Load and play backsound music
        try {
            backsoundMusic = Gdx.audio.newMusic(Gdx.files.internal("sound/backsound.mp3"));
            backsoundMusic.setLooping(true);
            backsoundMusic.play();
        } catch (Exception e) {
            backsoundMusic = null;
        }

        com.steven.frontend.features.AuthBackendFeature.initBackendConfig(this);
        com.steven.frontend.features.AuthBackendFeature.initializeLoginUI(this);
        if (!authGateActive) {
            com.steven.frontend.features.AuthBackendFeature.authenticateAndLoadBackendAsync(this);
        }

        applyPersistentLayerVisibility();
        // Initialize plant feature state and backups
        com.steven.frontend.features.PlantFeature.init(this);

        // Camera follow (x,y are in tile units) but clamp to map bounds
        float halfW = camera.viewportWidth * 0.5f;
        float halfH = camera.viewportHeight * 0.5f;
        float camX = MathUtils.clamp(x, halfW, mapWidth - halfW);
        float camY = MathUtils.clamp(y, halfH, mapHeight - halfH);
        camera.position.set(camX, camY, 0);
        camera.update();
    }

    public Animation<TextureRegion> createAnimationFromSheet(TextureRegion[][] tiles, int rowIndex, int[] colIndexes) {
        if (tiles == null || tiles.length == 0) return null;
        if (rowIndex < 0 || rowIndex >= tiles.length) return null;

        TextureRegion[] frames = new TextureRegion[colIndexes.length];
        int valid = 0;
        for (int i = 0; i < colIndexes.length; i++) {
            int col = colIndexes[i];
            if (col >= 0 && col < tiles[rowIndex].length) {
                frames[valid++] = tiles[rowIndex][col];
            }
        }
        if (valid == 0) return null;

        TextureRegion[] trimmed = new TextureRegion[valid];
        System.arraycopy(frames, 0, trimmed, 0, valid);
        return new Animation<>(0.12f, trimmed);
    }

    public Animation<TextureRegion> getWateringAnimationForFacing(Facing facing) {
        return waterAnimLeft != null ? waterAnimLeft : (waterAnimRight != null ? waterAnimRight : waterAnimDown);
    }

    // Untuk memulai animasi penyiraman tanaman dan mengatur state terkait.
    public void startWateringAnimation() {
        wateringAnimTime = 0f;
        wateringFacing = lastFacing;
        wateringActive = getWateringAnimationForFacing(wateringFacing) != null;
    }

    // Untuk menandai progress sebagai berubah sehingga autosave akan mengirimkannya.
    public void markProgressDirty() {
        if (backendLoading) return;
        if (gameCompletedLocked) return;
        progressDirty = true;
        progressChangeSeq++;
        if (!inventoryDirty) backendAutosaveTimer = 0f;
    }

    // Untuk menandai inventory sebagai berubah sehingga autosave akan mengirimkannya.
    public void markInventoryDirty() {
        if (backendLoading) return;
        if (gameCompletedLocked) return;
        inventoryDirty = true;
        inventoryChangeSeq++;
        if (!progressDirty) backendAutosaveTimer = 0f;
    }

    public String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    // Untuk menyusun payload progress pemain sebelum dikirim ke backend.
    public String buildProgressPayload() {
        ProgressPayloadBuilder builder = new ProgressPayloadBuilder().start();
        builder.field("coins", inventory.coins);
        builder.field("player_fatigue", playerFatigue);
        builder.field("current_day", currentDay);
        builder.field("game_time", gameTime);
        builder.fieldString("current_map", currentMapPath);
        builder.field("player_x", x);
        builder.field("player_y", y);
        builder.field("total_coins_earned", totalCoinsEarned);
        builder.field("consumed_count", consumedCount);
        builder.field("sold_count", soldCount);
        builder.field("world_coin_collected", worldCoinCollected);
        builder.field("world_bridge_repaired", worldBridgeRepaired);
        builder.field("world_pusat_bridge1_repaired", worldPusatBridge1Repaired);
        builder.field("world_pusat_bridge2_repaired", worldPusatBridge2Repaired);
        builder.field("house_final_repaired", houseFinalRepaired);
        builder.field("world_chest_claimed", ChestFeature.worldChestClaimed);
        builder.field("world_pusat_chest1_claimed", ChestFeature.worldPusatChest1Claimed);
        builder.field("world_pusat_chest2_claimed", ChestFeature.worldPusatChest2Claimed);
        return builder.build();
    }

    // Untuk menyusun payload inventory pemain sebelum dikirim ke backend.
    public String buildInventoryPayload() {
        return "{" +
            "\"items\":[" +
            "{\"item_type\":\"seed_bit\",\"quantity\":" + inventory.bitSeeds + "}," +
            "{\"item_type\":\"seed_wheat\",\"quantity\":" + inventory.wheatSeeds + "}," +
            "{\"item_type\":\"harvest_bit\",\"quantity\":" + inventory.harvestedBit + "}," +
            "{\"item_type\":\"harvest_wheat\",\"quantity\":" + inventory.harvestedWheat + "}," +
            "{\"item_type\":\"key\",\"quantity\":" + inventory.keys + "}" +
            "]}";
    }

    // Untuk menambah posisi pemain ketika strategi movement sudah mengizinkan langkah baru.
    public void moveBy(float dx, float dy) {
        x += dx;
        y += dy;
    }

    // Untuk membuat permintaan HTTP sinkron sederhana dan mengembalikan respon teks.
    public String httpRequest(String method, String urlStr, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoInput(true);
        if (body != null) {
            conn.setDoOutput(true);
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
            }
        }
        int code = conn.getResponseCode();
        InputStream stream = code >= 200 && code < 400 ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        if (stream != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
        }
        String response = sb.toString();
        if (code < 200 || code >= 300) {
            throw new RuntimeException(response == null || response.isEmpty() ? ("HTTP " + code) : response);
        }
        return response;
    }

    public String normalizeBaseUrl(String value) {
        String candidate = value == null ? "" : value.trim();
        if (candidate.isEmpty()) return "";
        while (candidate.endsWith("/")) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        return candidate;
    }

    public java.util.List<String> buildBackendBaseUrlCandidates() {
        java.util.LinkedHashSet<String> ordered = new java.util.LinkedHashSet<String>();
        String current = normalizeBaseUrl(backendBaseUrl);
        if (!current.isEmpty()) ordered.add(current);
        for (String fallback : BACKEND_BASE_URL_CANDIDATES) {
            String normalized = normalizeBaseUrl(fallback);
            if (!normalized.isEmpty()) ordered.add(normalized);
        }
        return new java.util.ArrayList<String>(ordered);
    }

    // Untuk mengirim request login atau register ke backend dan mengembalikan player id.
    public String loginOrRegister(String baseUrl, String username, String password) throws Exception {
        String authBody = "{\"username\":\"" + escapeJson(username) + "\",\"password\":\"" + escapeJson(password) + "\"}";
        try {
            String loginResponse = httpRequest("POST", baseUrl + "/auth/login", authBody);
            JsonValue loginJson = new JsonReader().parse(loginResponse);
            return loginJson.getString("id", "");
        } catch (Exception loginError) {
            String registerResponse = httpRequest("POST", baseUrl + "/auth/register", authBody);
            JsonValue regJson = new JsonReader().parse(registerResponse);
            return regJson.getString("id", "");
        }
    }

    @Override
    // Untuk menjalankan loop render utama game, update logic, input, animasi, dan menggambar frame.
    public void render() {
        float dt = com.badlogic.gdx.Gdx.graphics.getDeltaTime();

        if (authGateActive) {
            com.badlogic.gdx.Gdx.gl.glClearColor(0.08f, 0.08f, 0.12f, 1f);
            com.badlogic.gdx.Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT);
            if (authStage != null) {
                authStage.act(dt);
                authStage.draw();
            }
            return;
        }

        com.steven.frontend.features.InputFeature.handleInput(this, dt);
        com.steven.frontend.features.LogicFeature.update(this, dt);
        com.steven.frontend.features.WorldRenderFeature.renderWorld(this, dt);
        com.steven.frontend.features.UIFeature.renderHUD(this, dt);

        com.steven.frontend.features.AuthBackendFeature.processBackendAutosave(this, dt);
        if (gameCompletedShown && gameCompleteStage != null) {
            gameCompleteStage.act(dt);
            gameCompleteStage.draw();
        }
    }

    @Override
    public void dispose() {
        // Untuk membersihkan semua sumber daya (textures, sounds, stages) dan melakukan final save ke backend jika diperlukan.
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
        if (authStage != null) authStage.dispose();
        if (authSkin != null) authSkin.dispose();
        if (sheetTexture != null) sheetTexture.dispose();
        if (seedTexture != null) seedTexture.dispose();
        if (toolsTexture != null) toolsTexture.dispose();
        if (harvestedBitTexture != null) harvestedBitTexture.dispose();
        if (harvestedWheatTexture != null) harvestedWheatTexture.dispose();
        if (coinTexture != null) coinTexture.dispose();
        if (frontChestTexture != null) frontChestTexture.dispose();
        if (rightChestTexture != null) rightChestTexture.dispose();
        if (keyTexture != null) keyTexture.dispose();
        if (waterTexture != null) waterTexture.dispose();
        if (buyLayoutTexture != null) buyLayoutTexture.dispose();
        if (sellLayoutTexture != null) sellLayoutTexture.dispose();
        if (chestRewardLayoutTexture != null) chestRewardLayoutTexture.dispose();
        if (chestRewardLayoutWp1Texture != null) chestRewardLayoutWp1Texture.dispose();
        if (chestRewardLayoutWp2Texture != null) chestRewardLayoutWp2Texture.dispose();
        if (loginFormTexture != null) loginFormTexture.dispose();
        if (usernameFormTexture != null) usernameFormTexture.dispose();
        if (passwordFormTexture != null) passwordFormTexture.dispose();
        if (loginButtonTexture != null) loginButtonTexture.dispose();
        if (registerButtonTexture != null) registerButtonTexture.dispose();
        if (backsoundMusic != null) backsoundMusic.dispose();
        if (mapRenderer != null) mapRenderer.dispose();
        if (map != null) map.dispose();
        // Attempt a final synchronous save when closing if connected to backend
        if (backendPlayerId != null && !backendPlayerId.isEmpty() && (progressDirty || inventoryDirty)) {
            try {
                String progressPayload = progressDirty ? buildProgressPayload() : null;
                String inventoryPayload = inventoryDirty ? buildInventoryPayload() : null;
                System.out.println("[client] dispose: final save progressPayload=" + progressPayload + " inventoryPayload=" + inventoryPayload);
                if (progressPayload != null) backendFacade.sendProgress(backendBaseUrl + "/game/progress/" + backendPlayerId, progressPayload);
                if (inventoryPayload != null) backendFacade.sendInventory(backendBaseUrl + "/game/inventory/" + backendPlayerId, inventoryPayload);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public boolean layerHasAnyTile(TiledMapTileLayer layer) {
        if (layer == null) return false;
        int w = layer.getWidth();
        int h = layer.getHeight();
        for (int tx = 0; tx < w; tx++) {
            for (int ty = 0; ty < h; ty++) {
                if (layer.getCell(tx, ty) != null) return true;
            }
        }
        return false;
    }

    public TiledMapTileLayer getActiveDoorCloseLayer() {
        if ("world/world_pusat.tmx".equals(currentMapPath)) return houseFinalDoorCloseLayer;
        return doorsCloseLayer;
    }

    public TiledMapTileLayer getActiveDoorHalfLayer() {
        if ("world/world_pusat.tmx".equals(currentMapPath)) return houseFinalDoorHalfLayer;
        return doorsHalfLayer;
    }

    public TiledMapTileLayer getActiveDoorOpenLayer() {
        if ("world/world_pusat.tmx".equals(currentMapPath)) return houseFinalDoorOpenLayer;
        return doorsOpenLayer;
    }

    public void applyPersistentLayerVisibility() {
    // Untuk mengatur ulang visibilitas layer penting berdasarkan progress yang sudah tersimpan.
        ChestFeature.rebuildChestSpotsForCurrentMap(this);
        ChestFeature.updateChestLayerVisibility(this);

        if ("world/world.tmx".equals(currentMapPath)) {
            if (bridgeLayer != null) bridgeLayer.setVisible(!worldBridgeRepaired);
            if (bridgeBenerLayer != null) bridgeBenerLayer.setVisible(worldBridgeRepaired);
            if (doorsCloseLayer != null) doorsCloseLayer.setVisible(true);
            if (doorsHalfLayer != null) doorsHalfLayer.setVisible(false);
            if (doorsOpenLayer != null) doorsOpenLayer.setVisible(false);
            if (coinLayer != null && worldCoinCollected) {
                for (int cx = 0; cx < coinLayer.getWidth(); cx++) {
                    for (int cy = 0; cy < coinLayer.getHeight(); cy++) {
                        if (coinLayer.getCell(cx, cy) != null) coinLayer.setCell(cx, cy, null);
                    }
                }
            }
        } else if ("world/world_pusat.tmx".equals(currentMapPath)) {
            if (bridgeRusak1Layer != null) bridgeRusak1Layer.setVisible(!worldPusatBridge1Repaired);
            if (bridgeBener1Layer != null) bridgeBener1Layer.setVisible(worldPusatBridge1Repaired);
            if (bridgeRusak2Layer != null) bridgeRusak2Layer.setVisible(!worldPusatBridge2Repaired);
            if (bridgeBener2Layer != null) bridgeBener2Layer.setVisible(worldPusatBridge2Repaired);
            if (houseFinalLayer != null) houseFinalLayer.setVisible(!houseFinalRepaired);
            if (houseFinalFixLayer != null) houseFinalFixLayer.setVisible(houseFinalRepaired);
            if (houseFinalFurnitureLayer != null) houseFinalFurnitureLayer.setVisible(houseFinalRepaired);
            if (houseFinalBedLayer != null) houseFinalBedLayer.setVisible(houseFinalRepaired);
            // Initialize house_final door state: closed when first repaired
            if (houseFinalRepaired) {
                com.steven.frontend.features.DoorFeature.setDoorLayersVisible(this, true, false, false);
            }
        }
    }

    public boolean canMoveTo(float nx, float ny) {
        int tx = (int)Math.floor(nx);
        int ty = (int)Math.floor(ny);
        if (tx < 0 || ty < 0 || tx >= mapWidth || ty >= mapHeight) return false;
        boolean onWater = hasTile(waterLayer, tx, ty);
        boolean onGrass = hasTile(grassLayer, tx, ty);
        boolean onHouseFloor = hasTile(houseFloorLayer, tx, ty);
        boolean onSpawn = hasTile(spawnLayer, tx, ty);
        boolean onKeset = hasTile(kesetLayer, tx, ty);
        boolean onBridge = hasTile(bridgeLayer, tx, ty);
        boolean onBridgeBener = hasTile(bridgeBenerLayer, tx, ty);
        boolean onBridgeRusak1 = hasTile(bridgeRusak1Layer, tx, ty);
        boolean onBridgeRusak2 = hasTile(bridgeRusak2Layer, tx, ty);
        boolean onBridgeBener1 = hasTile(bridgeBener1Layer, tx, ty);
        boolean onBridgeBener2 = hasTile(bridgeBener2Layer, tx, ty);
        boolean onToKota = hasTile(toKotaLayer, tx, ty);
        boolean onToHome = hasTile(toHomeLayer, tx, ty);
        boolean onSpawnKota = hasTile(spawnKotaLayer, tx, ty);
        boolean onSpawnHome = hasTile(spawnHomeLayer, tx, ty);
        boolean onDoorOpen = hasTile(getActiveDoorOpenLayer(), tx, ty);
        boolean onDoorHalf = hasTile(getActiveDoorHalfLayer(), tx, ty);
        boolean onDoorClose = hasTile(getActiveDoorCloseLayer(), tx, ty);
        boolean onHouseFinal = hasTile(houseFinalLayer, tx, ty);
        boolean onHouseFinalFix = hasTile(houseFinalFixLayer, tx, ty);
        boolean onHouseFinalFurniture = hasTile(houseFinalFurnitureLayer, tx, ty);
        boolean onHouseLine = hasTile(houseLineLayer, tx, ty);
        boolean onTree = hasTile(treeLayer, tx, ty);
        boolean onRock = hasTile(rockLayer, tx, ty);

        boolean collidesHouseObject = hasTile(houseObjectLayer, tx, ty);
        boolean collidesSellingPlace = hasTile(sellingPlaceLayer, tx, ty);
        boolean collidesChest = chestLayer != null && hasTile(chestLayer, tx, ty);
        boolean collidesChest1 = hasTile((TiledMapTileLayer) (map != null ? map.getLayers().get("chest1") : null), tx, ty);
        boolean collidesChest2 = hasTile((TiledMapTileLayer) (map != null ? map.getLayers().get("chest2") : null), tx, ty);

        // Explicit collision: if position is on an unrepaired broken bridge, block movement
        if (bridgeRusak1Layer != null && bridgeRusak1Layer.getCell(tx, ty) != null && !worldPusatBridge1Repaired) return false;
        if (bridgeRusak2Layer != null && bridgeRusak2Layer.getCell(tx, ty) != null && !worldPusatBridge2Repaired) return false;
        if (bridgeLayer != null && bridgeLayer.getCell(tx, ty) != null && !worldBridgeRepaired) return false;
        if (bridgeBenerLayer != null && bridgeBenerLayer.getCell(tx, ty) != null && !worldBridgeRepaired) return false;
        if (bridgeBener1Layer != null && bridgeBener1Layer.getCell(tx, ty) != null && !worldPusatBridge1Repaired) return false;
        if (bridgeBener2Layer != null && bridgeBener2Layer.getCell(tx, ty) != null && !worldPusatBridge2Repaired) return false;
        if ("world/world_pusat.tmx".equals(currentMapPath)) {
            if (onHouseFinal || onHouseFinalFix || onHouseFinalFurniture) return false;
        }

        boolean walkableBase = onGrass || onHouseFloor || onSpawn || onKeset
            || (onBridge && worldBridgeRepaired) || (onBridgeBener && worldBridgeRepaired)
            || (onBridgeBener1 && worldPusatBridge1Repaired) || (onBridgeBener2 && worldPusatBridge2Repaired)
            || onDoorOpen || onDoorHalf || onDoorClose
            || onToKota || onToHome || onSpawnKota || onSpawnHome;
        if (!walkableBase) return false;
        if ((onWater && !((onBridge && worldBridgeRepaired) || (onBridgeBener && worldBridgeRepaired) || (onBridgeBener1 && worldPusatBridge1Repaired) || (onBridgeBener2 && worldPusatBridge2Repaired))) || onHouseLine || onTree || onRock) return false;
        if (collidesHouseObject || collidesSellingPlace || collidesChest || collidesChest1 || collidesChest2) return false;

        // Only block door entry if house is not repaired in world_pusat
        if ("world/world_pusat.tmx".equals(currentMapPath) && !houseFinalRepaired) {
            if ((onDoorClose || onDoorHalf || onDoorOpen)) return false;
        }

        if ((onDoorClose || onDoorHalf) && com.steven.frontend.features.DoorFeature.doorState != com.steven.frontend.features.DoorFeature.DoorState.OPEN) return false;

        return true;
    }

    public boolean hasTile(TiledMapTileLayer layer, int tx, int ty) {
        return layer != null && layer.getCell(tx, ty) != null;
    }
    
    public float getDistanceToBridge() {
        // Check distance to any unrepaired bridge
        int px = (int)Math.floor(x);
        int py = (int)Math.floor(y);
        float minDist = Float.MAX_VALUE;
        
        // Check distance to world bridge
        if (!worldBridgeRepaired) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    if (hasTile(bridgeLayer, px + dx, py + dy)) {
                        float dist = (float)Math.sqrt(dx * dx + dy * dy);
                        minDist = Math.min(minDist, dist);
                    }
                }
            }
        }
        
        // Check distance to world_pusat bridge rusak1
        if (!worldPusatBridge1Repaired) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    if (hasTile(bridgeRusak1Layer, px + dx, py + dy)) {
                        float dist = (float)Math.sqrt(dx * dx + dy * dy);
                        minDist = Math.min(minDist, dist);
                    }
                }
            }
        }
        
        // Check distance to world_pusat bridge rusak2
        if (!worldPusatBridge2Repaired) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    if (hasTile(bridgeRusak2Layer, px + dx, py + dy)) {
                        float dist = (float)Math.sqrt(dx * dx + dy * dy);
                        minDist = Math.min(minDist, dist);
                    }
                }
            }
        }
        
        return minDist;
    }

    // Untuk mengupdate ukuran peta dan viewport kamera setelah memuat peta.
    public void updateMapDimensionsAndViewport() {
        MapProperties props = map.getProperties();
        mapWidth = props.get("width", Integer.class);
        mapHeight = props.get("height", Integer.class);

        Integer tileWidthObj = props.get("tilewidth", Integer.class);
        Integer tileHeightObj = props.get("tileheight", Integer.class);
        int tileWidth = (tileWidthObj != null && tileWidthObj > 0) ? tileWidthObj : 16;
        int tileHeight = (tileHeightObj != null && tileHeightObj > 0) ? tileHeightObj : 16;

        mapRenderer = new OrthogonalTiledMapRenderer(map, 1f / (float)tileWidth);

        float viewportWidth = Math.min(640f / (float)tileWidth, mapWidth);
        float viewportHeight = Math.min(480f / (float)tileHeight, mapHeight);
        if (camera == null) {
            camera = new OrthographicCamera(viewportWidth, viewportHeight);
        } else {
            camera.viewportWidth = viewportWidth;
            camera.viewportHeight = viewportHeight;
        }
        camera.update();
    }

    // Untuk memeriksa apakah pemain men-trigger pergantian map (teleport antar area).
    public void checkMapTransition() {
        int px = (int)Math.floor(x);
        int py = (int)Math.floor(y);

        boolean isWorld = "world/world.tmx".equals(currentMapPath) || "world.tmx".equals(currentMapPath);
        boolean isWorldPusat = "world/world_pusat.tmx".equals(currentMapPath) || "world_pusat.tmx".equals(currentMapPath);

        if (isWorld && hasTile(toKotaLayer, px, py)) {
            switchMap("world/world_pusat.tmx", "spawn_home");
            return;
        }

        if (isWorldPusat && hasTile(toHomeLayer, px, py)) {
            switchMap("world/world.tmx", "spawn_kota");
        }
    }

    // Untuk mengganti peta saat transisi, memuat layer dan backup yang diperlukan.
    public void switchMap(String mapAssetPath, String spawnLayerName) {
        if (mapRenderer != null) mapRenderer.dispose();
        if (map != null) map.dispose();

        map = new TmxMapLoader().load(mapAssetPath);
        currentMapPath = mapAssetPath;
        updateMapDimensionsAndViewport();

        grassLayer = (TiledMapTileLayer) map.getLayers().get("grass");
        waterLayer = (TiledMapTileLayer) map.getLayers().get("water");
        spawnLayer = (TiledMapTileLayer) map.getLayers().get("spawn");
        houseFloorLayer = (TiledMapTileLayer) map.getLayers().get("house_floor");
        doorsCloseLayer = (TiledMapTileLayer) map.getLayers().get("doors_close");
        doorsHalfLayer = (TiledMapTileLayer) map.getLayers().get("doors_half");
        doorsOpenLayer = (TiledMapTileLayer) map.getLayers().get("doors_open");
        bedLayer = (TiledMapTileLayer) map.getLayers().get("bed");
        houseObjectLayer = (TiledMapTileLayer) map.getLayers().get("house_object");
        kesetLayer = (TiledMapTileLayer) map.getLayers().get("keset");
        houseLineLayer = (TiledMapTileLayer) map.getLayers().get("house_line");
        treeLayer = (TiledMapTileLayer) map.getLayers().get("tree");
        rockLayer = (TiledMapTileLayer) map.getLayers().get("rock");
        houseFinalLayer = (TiledMapTileLayer) map.getLayers().get("house_final");
        houseFinalFixLayer = (TiledMapTileLayer) map.getLayers().get("house_final_fix");
        houseFinalFurnitureLayer = (TiledMapTileLayer) map.getLayers().get("house_final_furniture");
        if (houseFinalFurnitureLayer == null) houseFinalFurnitureLayer = (TiledMapTileLayer) map.getLayers().get("final_house_furniture");
        houseFinalBedLayer = (TiledMapTileLayer) map.getLayers().get("house_final_bed");
        houseFinalDoorCloseLayer = (TiledMapTileLayer) map.getLayers().get("house_final_door_close");
        if (houseFinalDoorCloseLayer == null) houseFinalDoorCloseLayer = (TiledMapTileLayer) map.getLayers().get("house_final_door");
        houseFinalDoorHalfLayer = (TiledMapTileLayer) map.getLayers().get("house_final_door_half");
        houseFinalDoorOpenLayer = (TiledMapTileLayer) map.getLayers().get("house_final_door_open");
        sellingPlaceLayer = (TiledMapTileLayer) map.getLayers().get("selling_place");
        chestLayer = (TiledMapTileLayer) map.getLayers().get("chest");
        bridgeLayer = (TiledMapTileLayer) map.getLayers().get("bridge");
        bridgeBenerLayer = (TiledMapTileLayer) map.getLayers().get("bridge_bener");
        bridgeRusak1Layer = (TiledMapTileLayer) map.getLayers().get("bridge_rusak1");
        bridgeRusak2Layer = (TiledMapTileLayer) map.getLayers().get("bridge_rusak2");
        bridgeBener1Layer = (TiledMapTileLayer) map.getLayers().get("bridge_bener1");
        bridgeBener2Layer = (TiledMapTileLayer) map.getLayers().get("bridge_bener2");
        toKotaLayer = (TiledMapTileLayer) map.getLayers().get("to_kota");
        toHomeLayer = (TiledMapTileLayer) map.getLayers().get("to_home");

        for (int i = 0; i < 6; i++) {
            plantAreaLayers[i] = null;
            plantAreaCellsBackup[i] = null;
            plantAreaMinX[i] = 0;
            plantAreaMaxX[i] = 0;
            plantAreaMinY[i] = 0;
            plantAreaMaxY[i] = 0;
            plantAreaWidthTiles[i] = 0;
            plantAreaHeightTiles[i] = 0;

            plantAreaLayers[i] = (TiledMapTileLayer) map.getLayers().get("plant_area" + (i + 1));
            if (plantAreaLayers[i] != null) {
                int width = plantAreaLayers[i].getWidth();
                int height = plantAreaLayers[i].getHeight();
                plantAreaCellsBackup[i] = new TiledMapTileLayer.Cell[width][height];
                for (int tx = 0; tx < width; tx++) {
                    for (int ty = 0; ty < height; ty++) {
                        plantAreaCellsBackup[i][tx][ty] = plantAreaLayers[i].getCell(tx, ty);
                    }
                }

                int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
                int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
                boolean hasAnyTile = false;
                for (int tx = 0; tx < width; tx++) {
                    for (int ty = 0; ty < height; ty++) {
                        if (plantAreaCellsBackup[i][tx][ty] != null) {
                            minX = Math.min(minX, tx);
                            maxX = Math.max(maxX, tx);
                            minY = Math.min(minY, ty);
                            maxY = Math.max(maxY, ty);
                            hasAnyTile = true;
                        }
                    }
                }

                if (hasAnyTile) {
                    plantAreaMinX[i] = minX;
                    plantAreaMaxX[i] = maxX;
                    plantAreaMinY[i] = minY;
                    plantAreaMaxY[i] = maxY;
                    plantAreaWidthTiles[i] = maxX - minX + 1;
                    plantAreaHeightTiles[i] = maxY - minY + 1;
                }
            }
        }

        String[] bitNames = {"bit1", "bit2", "bit3", "bit4", "bitdone"};
        String[] wheatNames = {"gandum1", "gandum2", "gandum3", "gandum4", "gandumdone"};
        for (int i = 0; i < 5; i++) {
            bitStageLayers[i] = (TiledMapTileLayer) map.getLayers().get(bitNames[i]);
            wheatStageLayers[i] = (TiledMapTileLayer) map.getLayers().get(wheatNames[i]);
        }

        int cx = mapWidth / 2;
        int cy = mapHeight / 2;
        TiledMapTileLayer desiredSpawn = (TiledMapTileLayer) map.getLayers().get(spawnLayerName);
        if (desiredSpawn != null) {
            boolean foundSpawn = false;
            for (int ty = mapHeight - 1; ty >= 0 && !foundSpawn; ty--) {
                for (int tx = 0; tx < mapWidth && !foundSpawn; tx++) {
                    if (desiredSpawn.getCell(tx, ty) != null) {
                        cx = tx;
                        cy = ty;
                        foundSpawn = true;
                    }
                }
            }
        }

        if (grassLayer != null && grassLayer.getCell(cx, cy) == null) {
            boolean found = false;
            int radius = 1;
            while (!found && radius < Math.max(mapWidth, mapHeight)) {
                for (int dx = -radius; dx <= radius && !found; dx++) {
                    for (int dy = -radius; dy <= radius && !found; dy++) {
                        int tx = cx + dx, ty = cy + dy;
                        if (tx < 0 || ty < 0 || tx >= mapWidth || ty >= mapHeight) continue;
                        if (grassLayer.getCell(tx, ty) != null) {
                            cx = tx;
                            cy = ty;
                            found = true;
                        }
                    }
                }
                radius++;
            }
        }

        x = cx + 0.5f;
        y = cy + 0.5f;
        markProgressDirty();

        com.steven.frontend.features.DoorFeature.doorState = com.steven.frontend.features.DoorFeature.DoorState.CLOSED;
        
        com.steven.frontend.features.DoorFeature.setDoorLayersVisible(this, true, false, false);
        applyPersistentLayerVisibility();

        mapTransitionCooldown = MAP_TRANSITION_COOLDOWN;
    }

    public void switchMap(String mapAssetPath, float spawnX, float spawnY) {
        String fallbackSpawn = "spawn";
        if ("world/world.tmx".equals(mapAssetPath)) fallbackSpawn = "spawn_kota";
        else if ("world/world_pusat.tmx".equals(mapAssetPath)) fallbackSpawn = "spawn_home";
        switchMap(mapAssetPath, fallbackSpawn);
        try {
            if (spawnX >= 0f && spawnY >= 0f) {
                x = spawnX;
                y = spawnY;
            }
            float halfW = camera.viewportWidth * 0.5f;
            float halfH = camera.viewportHeight * 0.5f;
            float camX = MathUtils.clamp(x, halfW, mapWidth - halfW);
            float camY = MathUtils.clamp(y, halfH, mapHeight - halfH);
            camera.position.set(camX, camY, 0);
            camera.update();
            markProgressDirty();
        } catch (Exception e) {
        }
    }
}
