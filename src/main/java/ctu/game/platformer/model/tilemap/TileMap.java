package ctu.game.platformer.model.tilemap;

import ctu.game.platformer.model.common.GameObject;
import jakarta.annotation.PostConstruct;
import org.lwjgl.opengl.GL11;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

@Component
public class TileMap {
    // Constants
    private static final int TILE_SIZE = 32;
    private static final float PARALLAX_X = 0.2f;
    private static final float PARALLAX_Y = 0.05f;

    // Map properties
    private int mapWidth;
    private int mapHeight;
    private int[][][] mapLayers;
    private int layerCount;
    private boolean[] layerVisible;

    // Current state
    private int currentLayer = 0;
    private int previousLayer = 0;
    private float cameraX;
    private float cameraY;
    private boolean showCollision = true;

    // Background
    private String currentBackground = null;
    private int backgroundTextureId = -1;
    private Map<String, Integer> backgroundCache = new HashMap<>();

    // Textures
    private Map<Integer, Integer> tileTextures = new HashMap<>();
    private Map<String, Integer> objectTextures = new HashMap<>();
    private boolean texturesLoaded = false;
    private int defaultTextureId = -1;

    // Objects
    private Map<Integer, List<MapObject>> layerObjects = new HashMap<>();
    private List<MapObject> objects = new ArrayList<>();

    @PostConstruct
    public void init() {
        // Spring initialization hook
    }

    public void loadMap(String filename) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("maps/" + filename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            // Parse map dimensions and layer information
            String[] dimensions = reader.readLine().split(",");
            mapWidth = Integer.parseInt(dimensions[0]);
            mapHeight = Integer.parseInt(dimensions[1]);
            layerCount = Integer.parseInt(dimensions[2]);

            // Initialize data structures
            initializeMapData();

            // Read layer tile data
            loadLayerData(reader);

            // Read object data
            loadObjectData(reader);

            System.out.println("Map loaded: " + mapWidth + "x" + mapHeight + " with " + layerCount +
                    " layers and " + objects.size() + " objects");
        } catch (Exception e) {
            System.err.println("Error loading map: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeMapData() {
        // Initialize map arrays
        mapLayers = new int[layerCount][mapHeight][mapWidth];
        layerVisible = new boolean[layerCount];
        Arrays.fill(layerVisible, true);

        // Clear object collections
        objects.clear();
        layerObjects.clear();

        for (int i = 0; i < layerCount; i++) {
            layerObjects.put(i, new ArrayList<>());
        }
    }

    private void loadLayerData(BufferedReader reader) throws Exception {
        for (int layer = 0; layer < layerCount; layer++) {
            for (int y = 0; y < mapHeight; y++) {
                String line = reader.readLine();
                if (line == null) break;

                String[] tiles = line.split(",");
                for (int x = 0; x < Math.min(mapWidth, tiles.length); x++) {
                    mapLayers[layer][y][x] = Integer.parseInt(tiles[x]);
                }
            }

            // Skip empty line between layers if not the last layer
            if (layer < layerCount - 1) {
                reader.readLine();
            }
        }
    }

    private void loadObjectData(BufferedReader reader) throws Exception {
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) {
                String[] parts = line.split(",");
                if (parts.length >= 4) { // Type, X, Y, Layer
                    int type = Integer.parseInt(parts[0]);
                    float objX = Float.parseFloat(parts[1]);
                    float objY = Float.parseFloat(parts[2]);
                    int objLayer = Integer.parseInt(parts[3]);

                    MapObject obj = createObject(type, objX, objY);
                    if (obj != null) {
                        objects.add(obj);

                        // Add to correct layer
                        int targetLayer = (objLayer >= 0 && objLayer < layerCount) ? objLayer : 0;
                        layerObjects.get(targetLayer).add(obj);
                    }
                }
            }
        }
    }

    public void render(float playerX, float playerY, int screenWidth, int screenHeight) {
        if (!texturesLoaded) {
            loadTextures();
        }

        // Update camera for culling
        cameraX = playerX;
        cameraY = playerY;

        // Draw background
        renderBackground(playerX, playerY, screenWidth, screenHeight);

        // Calculate visible area for culling
        int startTileX = Math.max(0, (int)((cameraX - screenWidth/2) / TILE_SIZE));
        int endTileX = Math.min(mapWidth, (int)((cameraX + screenWidth/2) / TILE_SIZE) + 2);
        int startTileY = Math.max(0, (int)((cameraY - screenHeight/2) / TILE_SIZE));
        int endTileY = Math.min(mapHeight, (int)((cameraY + screenHeight/2) / TILE_SIZE) + 2);

        // Enable texturing
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        // Only render visible layer
        if (layerVisible[currentLayer]) {
            renderTiles(startTileX, endTileX, startTileY, endTileY);
            renderLayerObjects(currentLayer);
        }

        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    private void renderTiles(int startX, int endX, int startY, int endY) {
        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                int tileType = mapLayers[currentLayer][y][x];
                if (tileType == 0) continue; // Skip empty tiles

                float drawX = x * TILE_SIZE;
                float drawY = y * TILE_SIZE;

                renderTile(tileType, drawX, drawY);

                // Show collision boxes only if enabled
                if (showCollision && isTileSolid(tileType)) {
                    renderCollisionBox(drawX, drawY, TILE_SIZE, TILE_SIZE);
                }
            }
        }
    }

    private void renderBackground(float playerX, float playerY, int screenWidth, int screenHeight) {
        if (currentBackground == null || currentBackground.isEmpty()) {
            return;
        }

        // Load texture if needed (with caching)
        if (backgroundTextureId == -1) {
            backgroundTextureId = backgroundCache.computeIfAbsent(currentBackground,
                    bg -> TextureManager.loadTexture("assets/images/" + bg, true));
        }

        if (backgroundTextureId != -1) {
            // Save current matrix
            GL11.glPushMatrix();
            GL11.glLoadIdentity();

            // Calculate parallax offset
            float offsetX = -playerX * PARALLAX_X;
            float offsetY = -playerY * PARALLAX_Y;

            // Draw background
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, backgroundTextureId);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0, 0); GL11.glVertex2f(0, 0);
            GL11.glTexCoord2f(1, 0); GL11.glVertex2f(screenWidth, 0);
            GL11.glTexCoord2f(1, 1); GL11.glVertex2f(screenWidth, screenHeight);
            GL11.glTexCoord2f(0, 1); GL11.glVertex2f(0, screenHeight);
            GL11.glEnd();
            GL11.glDisable(GL11.GL_TEXTURE_2D);

            // Restore matrix
            GL11.glPopMatrix();
        }
    }

    private void loadTextures() {
        if (texturesLoaded) return;

        System.out.println("Loading tile and object textures...");
        long startTime = System.currentTimeMillis();

        try {
            // Batch load tile textures
            Map<String, Integer> tileTextureFiles = Map.of(
                    "maps/tiles/wall.png", 1,
                    "maps/tiles/grass.png", 2,
                    "maps/tiles/dirt.png", 3,
                    "maps/tiles/water.png", 4
            );

            // Load all textures
            tileTextureFiles.forEach((path, id) -> {
                try {
                    int textureId = TextureManager.loadTexture(path, true);
                    tileTextures.put(id, textureId);
                } catch (Exception e) {
                    System.err.println("Failed to load tile texture " + path + ": " + e.getMessage());
                    if (defaultTextureId != -1) {
                        tileTextures.put(id, defaultTextureId);
                    }
                }
            });

            // Batch load object textures
            objectTextures.put("coin", TextureManager.loadTexture("textures/objects/coin.png", true));
            objectTextures.put("enemy", TextureManager.loadTexture("textures/objects/enemy.png", true));
            objectTextures.put("layerportal", TextureManager.loadTexture("textures/objects/layerportal.png", true));
            objectTextures.put("layerreturn", TextureManager.loadTexture("textures/objects/layerreturn.png", true));

            texturesLoaded = true;
            long endTime = System.currentTimeMillis();
            System.out.println("Textures loaded in " + (endTime - startTime) + "ms");
        } catch (Exception e) {
            System.err.println("Error initializing textures: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void renderTile(int tileType, float x, float y) {
        Integer textureId = tileTextures.get(tileType);

        if (textureId != null) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            drawQuad(x, y, TILE_SIZE, TILE_SIZE);
        } else {
            // Fallback to colored rectangles if texture not found
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            switch (tileType) {
                case 1: GL11.glColor3f(0.5f, 0.5f, 0.5f); break; // Gray
                case 2: GL11.glColor3f(0.0f, 0.8f, 0.0f); break; // Green
                case 3: GL11.glColor3f(0.6f, 0.3f, 0.0f); break; // Brown
                case 4: GL11.glColor3f(0.0f, 0.0f, 0.8f); break; // Blue
                default: GL11.glColor3f(1.0f, 1.0f, 1.0f); // White
            }

            drawQuad(x, y, TILE_SIZE, TILE_SIZE);

            // Restore state
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor3f(1.0f, 1.0f, 1.0f);
        }
    }

    private void drawQuad(float x, float y, float width, float height) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0); GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(1, 0); GL11.glVertex2f(x + width, y);
        GL11.glTexCoord2f(1, 1); GL11.glVertex2f(x + width, y + height);
        GL11.glTexCoord2f(0, 1); GL11.glVertex2f(x, y + height);
        GL11.glEnd();
    }

    private void renderLayerObjects(int layer) {
        List<MapObject> objectsToRender = layerObjects.getOrDefault(layer, Collections.emptyList());
        for (MapObject obj : objectsToRender) {
            // Simple culling - only render objects near the camera
            if (isObjectVisible(obj)) {
                renderObject(obj, obj.getX(), obj.getY());
            }
        }
    }

    private boolean isObjectVisible(MapObject obj) {
        float objCenterX = obj.getX() + obj.getWidth()/2;
        float objCenterY = obj.getY() + obj.getHeight()/2;

        // Only render objects near the camera view (with padding)
        float visibilityRadius = Math.max(1024, Math.max(mapWidth, mapHeight) * TILE_SIZE / 2);
        float dx = objCenterX - cameraX;
        float dy = objCenterY - cameraY;

        return (dx*dx + dy*dy) <= visibilityRadius*visibilityRadius;
    }

    private void renderObject(MapObject obj, float screenX, float screenY) {
        Integer textureId = objectTextures.get(obj.getType());

        if (textureId != null) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            drawQuad(screenX, screenY, obj.getWidth(), obj.getHeight());
        } else {
            // Fallback rendering without texture
            GL11.glDisable(GL11.GL_TEXTURE_2D);

            switch (obj.getType()) {
                case "coin": GL11.glColor3f(1.0f, 1.0f, 0.0f); break; // Yellow
                case "enemy": GL11.glColor3f(1.0f, 0.0f, 0.0f); break; // Red
                default: GL11.glColor3f(0.8f, 0.8f, 0.8f); break; // Gray
            }

            drawQuad(screenX, screenY, obj.getWidth(), obj.getHeight());

            // Restore state
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor3f(1.0f, 1.0f, 1.0f);
        }

        // Debug outline for collision visualization
        if (showCollision) {
            renderCollisionBox(screenX, screenY, obj.getWidth(), obj.getHeight());
        }
    }

    private void renderCollisionBox(float x, float y, float width, float height) {
        // Save current state
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        float[] currentColor = new float[4];
        GL11.glGetFloatv(GL11.GL_CURRENT_COLOR, currentColor);

        // Draw collision outline
        GL11.glColor4f(1.0f, 0.0f, 1.0f, 0.7f); // Magenta
        GL11.glLineWidth(2.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + width, y);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();

        // Restore state
        GL11.glColor4f(currentColor[0], currentColor[1], currentColor[2], currentColor[3]);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private MapObject createObject(int type, float x, float y) {
        return switch (type) {
            case 1 -> new MapObject(x, y, TILE_SIZE, TILE_SIZE, "coin", "level2.csv");
            case 2 -> new MapObject(x, y, TILE_SIZE, TILE_SIZE, "enemy", null);
            case 3 -> new MapObject(x, y, TILE_SIZE, TILE_SIZE, "layerportal", null, 1);
            case 4 -> new MapObject(x, y, TILE_SIZE, TILE_SIZE, "layerreturn", null);
            default -> null;
        };
    }

    public void checkPlayerPosition(float playerX, float playerY) {
        // Only check objects in the current layer
        List<MapObject> currentLayerObjects = layerObjects.getOrDefault(currentLayer, Collections.emptyList());

        for (MapObject obj : currentLayerObjects) {
            // Improved collision detection
            float objCenterX = obj.getX() + obj.getWidth() / 2;
            float objCenterY = obj.getY() + obj.getHeight() / 2;
            float distX = playerX - objCenterX;
            float distY = playerY - objCenterY;
            float distance = (float) Math.sqrt(distX * distX + distY * distY);
            float collisionRadius = (TILE_SIZE / 2 + obj.getWidth() / 2);

            if (distance < collisionRadius) {
                handleObjectInteraction(obj);
            }
        }
    }

    private void handleObjectInteraction(MapObject obj) {
        switch (obj.getType()) {
            case "layerportal":
                int targetLayer = obj.getTargetLayer();
                if (targetLayer >= 0 && targetLayer < layerCount) {
                    switchToLayer(targetLayer);
                }
                break;
            case "layerreturn":
                returnToPreviousLayer();
                break;
            case "coin":
                if (obj.getMapFilename() != null) {
                    loadMap(obj.getMapFilename());
                }
                break;
        }
    }

    public void switchToLayer(int newLayer) {
        if (newLayer >= 0 && newLayer < layerCount) {
            previousLayer = currentLayer;
            currentLayer = newLayer;
            System.out.println("Switched from layer " + previousLayer + " to layer " + currentLayer);
        }
    }

    public void returnToPreviousLayer() {
        int temp = currentLayer;
        currentLayer = previousLayer;
        previousLayer = temp;
        System.out.println("Returned to layer " + currentLayer);
    }

    public void setBackground(String backgroundPath) {
        if (!Objects.equals(this.currentBackground, backgroundPath)) {
            this.currentBackground = backgroundPath;
            this.backgroundTextureId = -1; // Force texture reload
        }
    }

    public boolean isSolid(float x, float y) {
        int tileX = (int)(x / TILE_SIZE);
        int tileY = (int)(y / TILE_SIZE);

        // Check bounds
        if (tileX < 0 || tileX >= mapWidth || tileY < 0 || tileY >= mapHeight) {
            return true; // Out of bounds is solid
        }

        // Check base layer (layer 0)
        if (isTileSolid(mapLayers[0][tileY][tileX])) {
            return true;
        }

        // Check current layer if different from base
        if (currentLayer > 0 && isTileSolid(mapLayers[currentLayer][tileY][tileX])) {
            return true;
        }

        return false;
    }

    private boolean isTileSolid(int tileType) {
        return tileType == 1 || tileType == 4;
    }

    public void toggleCollisionView() {
        showCollision = !showCollision;
    }

    // Getters
    public int getTileSize() {
        return TILE_SIZE;
    }

    public int getMapWidth() {
        return mapWidth;
    }

    public int getMapHeight() {
        return mapHeight;
    }

    public boolean isShowingCollision() {
        return showCollision;
    }
}