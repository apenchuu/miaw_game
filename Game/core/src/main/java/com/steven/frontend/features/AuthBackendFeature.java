package com.steven.frontend.features;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonReader;
import com.steven.frontend.Main;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import com.badlogic.gdx.Preferences;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class AuthBackendFeature {
    private AuthBackendFeature() {}

    public static String backendDeviceId = "";

    public static void initBackendConfig(Main main) {
        if (Gdx.app == null) return;
        Preferences prefs = Gdx.app.getPreferences("caslab-backend-sync");
        main.backendBaseUrl = prefs.getString("main.backendBaseUrl", Main.DEFAULT_BACKEND_URL);
        backendDeviceId = prefs.getString("deviceId", "");
        if (backendDeviceId == null || backendDeviceId.isEmpty()) {
            backendDeviceId = UUID.randomUUID().toString();
            prefs.putString("deviceId", backendDeviceId);
            prefs.flush();
        }
        main.backendPlayerId = "";
        main.backendUsername = "";
        main.backendPassword = "";
        prefs.remove("playerId");
        prefs.remove("username");
        prefs.remove("password");
        prefs.flush();
        main.authGateActive = true;
        main.authStatusText = "Tekan L untuk login atau R untuk register";
    }

    private static com.badlogic.gdx.scenes.scene2d.utils.Drawable createColorDrawable(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    private static com.badlogic.gdx.scenes.scene2d.utils.Drawable createRoundedDrawable(Color fillColor, Color borderColor) {
        final int width = 72;
        final int height = 72;
        final int radius = 16;
        final int border = 2;

        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);

        // Border
        pixmap.setColor(borderColor);
        pixmap.fillCircle(radius, radius, radius);
        pixmap.fillCircle(width - radius - 1, radius, radius);
        pixmap.fillCircle(radius, height - radius - 1, radius);
        pixmap.fillCircle(width - radius - 1, height - radius - 1, radius);
        pixmap.fillRectangle(radius, 0, width - (radius * 2), height);
        pixmap.fillRectangle(0, radius, width, height - (radius * 2));

        // Inner fill
        if (fillColor.a > 0f) {
            pixmap.setColor(fillColor);
            pixmap.fillCircle(radius, radius, radius - border);
            pixmap.fillCircle(width - radius - 1, radius, radius - border);
            pixmap.fillCircle(radius, height - radius - 1, radius - border);
            pixmap.fillCircle(width - radius - 1, height - radius - 1, radius - border);
            pixmap.fillRectangle(radius, border, width - (radius * 2), height - (border * 2));
            pixmap.fillRectangle(border, radius, width - (border * 2), height - (radius * 2));
        }

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        NinePatch patch = new NinePatch(texture, radius, radius, radius, radius);
        return new NinePatchDrawable(patch);
    }

    public static void persistBackendConfig(Main main) {
        if (Gdx.app == null) return;
        Preferences prefs = Gdx.app.getPreferences("caslab-backend-sync");
        prefs.putString("main.backendBaseUrl", main.backendBaseUrl == null ? Main.DEFAULT_BACKEND_URL : main.backendBaseUrl);
        prefs.putString("deviceId", backendDeviceId == null ? "" : backendDeviceId);
        prefs.flush();
    }

    public static void updateAuthModeIndicator(Main main) {
        // Handled in Main render()
    }

    public static void initializeLoginUI(Main main) {
        if (main.authStage != null) return;

        main.authStage = new Stage(new ScreenViewport());
        
        // Create skin with styles for textfields and buttons
        if (main.authSkin == null) {
            main.authSkin = new Skin();
            BitmapFont font = new BitmapFont();
            BitmapFont smallFont = new BitmapFont();
            smallFont.getData().setScale(0.9f);
            main.authSkin.add("default-font", font);
            main.authSkin.add("small-font", smallFont);

            // Label styles
            Label.LabelStyle labelStyle = new Label.LabelStyle();
            labelStyle.font = font;
            labelStyle.fontColor = Color.WHITE;
            main.authSkin.add("default", labelStyle);

            Label.LabelStyle titleStyle = new Label.LabelStyle();
            titleStyle.font = font;
            titleStyle.fontColor = new Color(1f, 1f, 0.8f, 1f); // Light yellow
            main.authSkin.add("title", titleStyle);
            
            // TextField style
            TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
            textFieldStyle.font = smallFont;
            textFieldStyle.fontColor = Color.BLACK;
            textFieldStyle.focusedFontColor = Color.BLACK;
            textFieldStyle.background = createRoundedDrawable(new Color(0f, 0f, 0f, 0f), new Color(0.96f, 0.52f, 0.82f, 0.95f));
            textFieldStyle.focusedBackground = createRoundedDrawable(new Color(0f, 0f, 0f, 0.03f), new Color(1f, 0.72f, 0.92f, 1f));
            textFieldStyle.cursor = createColorDrawable(new Color(1f, 0.82f, 0.95f, 1f));
            textFieldStyle.selection = createColorDrawable(new Color(0.86f, 0.48f, 0.78f, 0.45f));
            main.authSkin.add("default", textFieldStyle, TextField.TextFieldStyle.class);
            
            // TextButton style
            TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
            buttonStyle.font = smallFont;
            buttonStyle.fontColor = Color.WHITE;
            buttonStyle.up = createRoundedDrawable(new Color(0.76f, 0.26f, 0.62f, 1f), new Color(1f, 0.86f, 0.96f, 0.9f));
            buttonStyle.over = createRoundedDrawable(new Color(0.86f, 0.34f, 0.70f, 1f), new Color(1f, 0.9f, 0.98f, 1f));
            buttonStyle.down = createRoundedDrawable(new Color(0.63f, 0.18f, 0.54f, 1f), new Color(0.96f, 0.72f, 0.92f, 0.95f));
            main.authSkin.add("default", buttonStyle, TextButton.TextButtonStyle.class);
        }

        // Main form container with form_login background
        Stack formStack = new Stack();
        
        // Background image
        try {
            if (main.loginFormTexture == null) {
                main.loginFormTexture = new Texture(Gdx.files.internal("login/form_login.png"));
            }
            formStack.setSize(main.loginFormTexture.getWidth(), main.loginFormTexture.getHeight());
            com.badlogic.gdx.scenes.scene2d.utils.Drawable bgDrawable = new SpriteDrawable(
                new Sprite(main.loginFormTexture)
            );
            formStack.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(bgDrawable) {
                { setFillParent(true); }
            });
        } catch (Exception e) {
            // Fallback to colored background
            com.badlogic.gdx.scenes.scene2d.utils.Drawable bgDrawable = createColorDrawable(
                new Color(0.15f, 0.1f, 0.25f, 0.95f)
            );
            formStack.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(bgDrawable) {
                { setFillParent(true); }
            });
        }
        
        // Form content table
        Table formTable = new Table(main.authSkin);
        formTable.setFillParent(true);
        formTable.padTop(42f);
        formTable.padLeft(44f);
        formTable.padRight(44f);
        formTable.padBottom(34f);
        formTable.defaults().pad(5f).fillX();
        
        // Title
        Label titleLabel = main.uiFactory.createLabel(main.authSkin, "LOGIN / REGISTER", "title");
        titleLabel.setFontScale(1.15f);
        titleLabel.setAlignment(Align.center);
        formTable.add(titleLabel).row();
        
        // Subtitle
        Label subtitleLabel = main.uiFactory.createLabel(main.authSkin, "Masukkan username dan password");
        subtitleLabel.setFontScale(0.78f);
        subtitleLabel.setAlignment(Align.center);
        subtitleLabel.setWrap(true);
        formTable.add(subtitleLabel).width(300f).row();
        
        // Spacing
        formTable.add(new Label("", main.authSkin)).row();
        
        // Username label and field
        Label usernameLabel = main.uiFactory.createLabel(main.authSkin, "Username:");
        usernameLabel.setFontScale(0.82f);
        formTable.add(usernameLabel).row();
        
        main.txtUsername = new TextField("", main.authSkin);
        main.txtUsername.setMessageText("Ketik username...");
        main.txtUsername.setMaxLength(32);
        main.txtUsername.setAlignment(Align.left);
        formTable.add(main.txtUsername).height(34f).width(300f).row();
        
        // Spacing
        formTable.add(new Label("", main.authSkin)).row();
        
        // Password label and field
        Label passwordLabel = main.uiFactory.createLabel(main.authSkin, "Password:");
        passwordLabel.setFontScale(0.82f);
        formTable.add(passwordLabel).row();
        
        main.txtPassword = new TextField("", main.authSkin);
        main.txtPassword.setMessageText("Ketik password...");
        main.txtPassword.setPasswordMode(true);
        main.txtPassword.setPasswordCharacter('*');
        main.txtPassword.setMaxLength(32);
        main.txtPassword.setAlignment(Align.left);
        formTable.add(main.txtPassword).height(34f).width(300f).row();
        
        // Spacing
        formTable.add(new Label("", main.authSkin)).row();
        
        // Buttons table
        Table buttonTable = new Table(main.authSkin);
        buttonTable.defaults().pad(4f).expandX().fillX();
        
        main.btnLogin = new TextButton("LOGIN", main.authSkin);
        main.btnLogin.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                beginAuth(main, Main.AuthMode.LOGIN);
                main.authInputUsername = main.txtUsername != null ? main.txtUsername.getText().trim() : "";
                main.authInputPassword = main.txtPassword != null ? main.txtPassword.getText() : "";
                submitAuth(main);
            }
        });
        buttonTable.add(main.btnLogin).height(34f).width(144f);
        
        main.btnRegister = new TextButton("REGISTER", main.authSkin);
        main.btnRegister.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                beginAuth(main, Main.AuthMode.REGISTER);
                main.authInputUsername = main.txtUsername != null ? main.txtUsername.getText().trim() : "";
                main.authInputPassword = main.txtPassword != null ? main.txtPassword.getText() : "";
                submitAuth(main);
            }
        });
        buttonTable.add(main.btnRegister).height(34f).width(144f);
        
        formTable.add(buttonTable).fillX().row();
        
        // Spacing
        formTable.add(new Label("", main.authSkin)).row();
        
        // Info label
        Label infoLabel = main.uiFactory.createLabel(main.authSkin, "Tekan L untuk login atau R untuk register");
        infoLabel.setFontScale(0.68f);
        infoLabel.setAlignment(Align.center);
        infoLabel.setWrap(true);
        formTable.add(infoLabel).width(300f).row();
        
        formStack.add(formTable);
        
        // Center the form on stage
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.center();
        mainTable.add(formStack);
        
        main.authStage.addActor(mainTable);
    }

    public static void beginAuth(Main main, Main.AuthMode mode) {
        main.pendingAuthMode = mode;
        main.authField = Main.AuthField.USERNAME;
        main.authInputUsername = "";
        main.authInputPassword = "";
        main.authStatusText = "Masukkan username...";
    }

    public static void submitAuth(Main main) {
        String username = main.txtUsername != null ? main.txtUsername.getText().trim() : main.authInputUsername;
        String password = main.txtPassword != null ? main.txtPassword.getText() : main.authInputPassword;
        main.backendUsername = username == null ? "" : username;
        main.backendPassword = password == null ? "" : password;
        main.authInputUsername = main.backendUsername;
        main.authInputPassword = main.backendPassword;
        if (main.backendUsername.isEmpty() || main.backendPassword.isEmpty()) {
            main.authStatusText = "Username/Password tidak boleh kosong!";
            return;
        }
        main.authStatusText = "Menghubungi server...";
        main.backendLoading = true;
        authenticateAndLoadBackendAsync(main);
    }

    public static void authenticateAndLoadBackendAsync(Main main) {
        new Thread(() -> {
            boolean success = false;
            String pid = "";
            java.util.List<String> candidates = buildBackendBaseUrlCandidates(main);
            if (candidates.isEmpty()) {
                if (Gdx.app != null) {
                    Gdx.app.postRunnable(() -> {
                        main.authStatusText = "Error: Backend URL tidak dikonfigurasi.";
                        main.backendLoading = false;
                    });
                } else {
                    main.authStatusText = "Error: Backend URL tidak dikonfigurasi.";
                    main.backendLoading = false;
                }
                return;
            }

            for (String url : candidates) {
                try {
                    pid = loginOrRegister(main, url, main.backendUsername, main.backendPassword);
                    if (pid != null && !pid.isEmpty()) {
                        success = true;
                        main.backendBaseUrl = url;
                        persistBackendConfig(main);
                        break;
                    }
                } catch (Exception e) {
                    System.out.println("Gagal via " + url + ": " + e.getMessage());
                }
            }

            if (!success) {
                if (Gdx.app != null) {
                    Gdx.app.postRunnable(() -> {
                        main.authStatusText = "Login/Register gagal: Cek koneksi backend.";
                        main.backendLoading = false;
                    });
                } else {
                    main.authStatusText = "Login/Register gagal: Cek koneksi backend.";
                    main.backendLoading = false;
                }
                return;
            }

            System.out.println("[auth] Success, playerId=" + pid);
            final String playerId = pid;

            try {
                String pRes = httpRequest(main, "GET", main.backendBaseUrl + "/game/progress/" + playerId, null);
                String iRes = httpRequest(main, "GET", main.backendBaseUrl + "/game/inventory/" + playerId, null);
                if (Gdx.app != null) {
                    Gdx.app.postRunnable(() -> {
                        main.backendPlayerId = playerId;
                        applyBackendState(main, pRes, iRes);
                        main.authGateActive = false;
                        main.authStatusText = "";
                        main.backendLoading = false;
                        System.out.println("[client] Remote state loaded.");
                    });
                } else {
                    main.backendPlayerId = playerId;
                    applyBackendState(main, pRes, iRes);
                    main.authGateActive = false;
                    main.authStatusText = "";
                    main.backendLoading = false;
                }
            } catch (Exception e) {
                System.out.println("[client] No prior state found (or load failed), starting fresh. " + e.getMessage());
                if (Gdx.app != null) {
                    Gdx.app.postRunnable(() -> {
                        main.backendPlayerId = playerId;
                        main.authGateActive = false;
                        main.authStatusText = "";
                        markProgressDirty(main);
                        markInventoryDirty(main);
                        main.backendLoading = false;
                    });
                } else {
                    main.backendPlayerId = playerId;
                    main.authGateActive = false;
                    main.authStatusText = "";
                    markProgressDirty(main);
                    markInventoryDirty(main);
                    main.backendLoading = false;
                }
            }
        }).start();
    }

    public static void applyBackendState(Main main, String progressResponse, String inventoryResponse) {
        try {
            JsonValue root = new JsonReader().parse(progressResponse);
            String mapPath = null;
            if (root.has("coins")) main.inventory.coins = root.getInt("coins");
            if (root.has("player_fatigue")) main.playerFatigue = root.getFloat("player_fatigue");
            if (root.has("current_day")) main.currentDay = root.getInt("current_day");
            if (root.has("game_time")) main.gameTime = root.getFloat("game_time");
            if (root.has("current_map")) mapPath = root.getString("current_map");
            if (root.has("player_x")) main.x = root.getFloat("player_x");
            if (root.has("player_y")) main.y = root.getFloat("player_y");
            if (root.has("total_coins_earned")) main.totalCoinsEarned = root.getInt("total_coins_earned");
            if (root.has("consumed_count")) main.consumedCount = root.getInt("consumed_count");
            if (root.has("sold_count")) main.soldCount = root.getInt("sold_count");
            if (root.has("world_coin_collected")) main.worldCoinCollected = root.getBoolean("world_coin_collected");
            if (root.has("world_bridge_repaired")) main.worldBridgeRepaired = root.getBoolean("world_bridge_repaired");
            if (root.has("world_pusat_bridge1_repaired")) main.worldPusatBridge1Repaired = root.getBoolean("world_pusat_bridge1_repaired");
            if (root.has("world_pusat_bridge2_repaired")) main.worldPusatBridge2Repaired = root.getBoolean("world_pusat_bridge2_repaired");
            if (root.has("house_final_repaired")) main.houseFinalRepaired = root.getBoolean("house_final_repaired");
            if (root.has("world_chest_claimed")) ChestFeature.worldChestClaimed = root.getBoolean("world_chest_claimed");
            if (root.has("world_pusat_chest1_claimed")) ChestFeature.worldPusatChest1Claimed = root.getBoolean("world_pusat_chest1_claimed");
            if (root.has("world_pusat_chest2_claimed")) ChestFeature.worldPusatChest2Claimed = root.getBoolean("world_pusat_chest2_claimed");

            if (mapPath != null && !mapPath.isEmpty()) {
                if (mapPath.endsWith("world_pusat.tmx")) {
                    main.currentMapPath = "world/world_pusat.tmx";
                } else if (mapPath.endsWith("world.tmx")) {
                    main.currentMapPath = "world/world.tmx";
                } else {
                    main.currentMapPath = mapPath;
                }
            }
            
            if (main.currentMapPath != null && !main.currentMapPath.isEmpty()) {
                main.switchMap(main.currentMapPath, main.x, main.y);
            }
        } catch (Exception e) {
            System.err.println("apply progress error: " + e.getMessage());
        }

        try {
            JsonValue root = new JsonReader().parse(inventoryResponse);
            if (root != null) {
                if (root.isArray()) {
                    for (JsonValue item : root) {
                        String type = item.getString("item_type");
                        int qty = item.getInt("quantity");
                        if ("seed_bit".equals(type)) main.inventory.bitSeeds = qty;
                        if ("seed_wheat".equals(type)) main.inventory.wheatSeeds = qty;
                        if ("harvest_bit".equals(type)) main.inventory.harvestedBit = qty;
                        if ("harvest_wheat".equals(type)) main.inventory.harvestedWheat = qty;
                        if ("key".equals(type)) main.inventory.keys = qty;
                    }
                } else {
                    JsonValue items = root.get("items");
                    if (items != null) {
                        for (JsonValue item : items) {
                            String type = item.getString("item_type");
                            int qty = item.getInt("quantity");
                            if ("seed_bit".equals(type)) main.inventory.bitSeeds = qty;
                            if ("seed_wheat".equals(type)) main.inventory.wheatSeeds = qty;
                            if ("harvest_bit".equals(type)) main.inventory.harvestedBit = qty;
                            if ("harvest_wheat".equals(type)) main.inventory.harvestedWheat = qty;
                            if ("key".equals(type)) main.inventory.keys = qty;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("apply inventory error: " + e.getMessage());
        }
        
        main.rebuildHotbarMapping();
        // After applying backend state, ensure completion is checked so saved-complete accounts
        // will trigger the game-completed UI if all conditions already met. Also lock completion
        // so it cannot be reverted or overwritten.
        try {
            com.steven.frontend.features.CompletionFeature.checkForGameCompletion(main);
        } catch (Exception e) {
            System.err.println("checkForGameCompletion error: " + e.getMessage());
        }

        try {
            boolean all = main.worldBridgeRepaired && main.worldPusatBridge1Repaired && main.worldPusatBridge2Repaired
                && main.houseFinalRepaired && com.steven.frontend.features.ChestFeature.worldChestClaimed
                && com.steven.frontend.features.ChestFeature.worldPusatChest1Claimed && com.steven.frontend.features.ChestFeature.worldPusatChest2Claimed
                && main.worldCoinCollected;
            if (all) {
                main.gameCompletedLocked = true;
                if (!main.gameCompletedShown) {
                    main.gameCompletedShown = true;
                    try { main.gameEventManager.notifyGameCompleted(); } catch (Exception ignore) {}
                }
            }
        } catch (Exception e) {
            System.err.println("completion lock error: " + e.getMessage());
        }
    }

    public static void processBackendAutosave(Main main, float dt) {
        // If the game is completed and locked, skip autosave to avoid overwriting completed state.
        if (main.gameCompletedLocked) return;
        if (main.backendBaseUrl == null || main.backendBaseUrl.isEmpty() || main.backendPlayerId == null || main.backendPlayerId.isEmpty()) return;
        if (main.backendLoading || main.backendSaveInFlight) return;
        
        if (main.progressDirty || main.inventoryDirty) {
            main.backendAutosaveTimer += dt;
            if (main.backendAutosaveTimer >= Main.BACKEND_AUTOSAVE_INTERVAL) {
                final String pid = main.backendPlayerId;
                final String bUrl = main.backendBaseUrl;
                final boolean pDirty = main.progressDirty;
                final boolean iDirty = main.inventoryDirty;
                final long pSeq = main.progressChangeSeq;
                final long iSeq = main.inventoryChangeSeq;
                final String pPayload = pDirty ? buildProgressPayload(main) : null;
                final String iPayload = iDirty ? buildInventoryPayload(main) : null;
                
                main.backendSaveInFlight = true;
                
                new Thread(() -> {
                    try {
                        if (pDirty) {
                            System.out.println("[client] sending progress payload: " + pPayload);
                            httpRequest(main, "PUT", bUrl + "/game/progress/" + pid, pPayload);
                        }
                        if (iDirty) {
                            System.out.println("[client] sending inventory payload: " + iPayload);
                            httpRequest(main, "PUT", bUrl + "/game/inventory/" + pid, iPayload);
                        }
                        
                        Gdx.app.postRunnable(() -> {
                            if (pDirty && main.progressChangeSeq == pSeq) {
                                main.progressDirty = false;
                                main.progressSavedSeq = pSeq;
                            }
                            if (iDirty && main.inventoryChangeSeq == iSeq) {
                                main.inventoryDirty = false;
                                main.inventorySavedSeq = iSeq;
                            }
                            main.backendAutosaveTimer = 0f;
                            main.backendSaveInFlight = false;
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        Gdx.app.postRunnable(() -> main.backendSaveInFlight = false);
                    }
                }).start();
            }
        }
    }

    public static void markProgressDirty(Main main) {
        if (main.backendLoading) return;
        main.progressDirty = true;
        main.progressChangeSeq++;
        if (!main.inventoryDirty) main.backendAutosaveTimer = 0f;
    }

    public static void markInventoryDirty(Main main) {
        if (main.backendLoading) return;
        main.inventoryDirty = true;
        main.inventoryChangeSeq++;
        if (!main.progressDirty) main.backendAutosaveTimer = 0f;
    }

    public static String escapeJson(Main main, String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    public static String buildProgressPayload(Main main) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"coins\":").append(main.inventory.coins).append(",");
        sb.append("\"player_fatigue\":").append(main.playerFatigue).append(",");
        sb.append("\"current_day\":").append(main.currentDay).append(",");
        sb.append("\"game_time\":").append(main.gameTime).append(",");
        sb.append("\"current_map\":\"").append(main.currentMapPath).append("\",");
        sb.append("\"player_x\":").append(main.x).append(",");
        sb.append("\"player_y\":").append(main.y).append(",");
        sb.append("\"total_coins_earned\":").append(main.totalCoinsEarned).append(",");
        sb.append("\"consumed_count\":").append(main.consumedCount).append(",");
        sb.append("\"sold_count\":").append(main.soldCount).append(",");
        sb.append("\"world_coin_collected\":").append(main.worldCoinCollected).append(",");
        sb.append("\"world_bridge_repaired\":").append(main.worldBridgeRepaired).append(",");
        sb.append("\"world_pusat_bridge1_repaired\":").append(main.worldPusatBridge1Repaired).append(",");
        sb.append("\"world_pusat_bridge2_repaired\":").append(main.worldPusatBridge2Repaired).append(",");
        sb.append("\"house_final_repaired\":").append(main.houseFinalRepaired).append(",");
        sb.append("\"world_chest_claimed\":").append(ChestFeature.worldChestClaimed).append(",");
        sb.append("\"world_pusat_chest1_claimed\":").append(ChestFeature.worldPusatChest1Claimed).append(",");
        sb.append("\"world_pusat_chest2_claimed\":").append(ChestFeature.worldPusatChest2Claimed);
        sb.append("}");
        return sb.toString();
    }

    public static String buildInventoryPayload(Main main) {
        return "{" +
            "\"items\":[" +
            "{\"item_type\":\"seed_bit\",\"quantity\":" + main.inventory.bitSeeds + "}," +
            "{\"item_type\":\"seed_wheat\",\"quantity\":" + main.inventory.wheatSeeds + "}," +
            "{\"item_type\":\"harvest_bit\",\"quantity\":" + main.inventory.harvestedBit + "}," +
            "{\"item_type\":\"harvest_wheat\",\"quantity\":" + main.inventory.harvestedWheat + "}," +
            "{\"item_type\":\"key\",\"quantity\":" + main.inventory.keys + "}" +
            "]}";
    }

    public static String httpRequest(Main main, String method, String urlStr, String body) throws Exception {
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

    public static String normalizeBaseUrl(Main main, String value) {
        String candidate = value == null ? "" : value.trim();
        if (candidate.isEmpty()) return "";
        while (candidate.endsWith("/")) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        return candidate;
    }

    public static java.util.List<String> buildBackendBaseUrlCandidates(Main main) {
        java.util.LinkedHashSet<String> ordered = new java.util.LinkedHashSet<>();
        String current = normalizeBaseUrl(main, main.backendBaseUrl);
        if (!current.isEmpty()) ordered.add(current);
        for (String fallback : main.BACKEND_BASE_URL_CANDIDATES) {
            String normalized = normalizeBaseUrl(main, fallback);
            if (!normalized.isEmpty()) ordered.add(normalized);
        }
        return new java.util.ArrayList<>(ordered);
    }

    public static String loginOrRegister(Main main, String baseUrl, String username, String password) throws Exception {
        String authBody = "{\"username\":\"" + escapeJson(main, username) + "\",\"password\":\"" + escapeJson(main, password) + "\"}";
        try {
            String loginResponse = httpRequest(main, "POST", baseUrl + "/auth/login", authBody);
            JsonValue loginJson = new JsonReader().parse(loginResponse);
            return loginJson.getString("id", "");
        } catch (Exception loginError) {
            String registerResponse = httpRequest(main, "POST", baseUrl + "/auth/register", authBody);
            JsonValue regJson = new JsonReader().parse(registerResponse);
            return regJson.getString("id", "");
        }
    }
}
