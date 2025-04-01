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
    private int[][] mapData;
    private int tileSize = 32;
    private int mapWidth;
    private int mapHeight;

    private List<MapObject> objects = new ArrayList<>();
    private boolean showCollision = true;
    // Track camera position for culling
    private float cameraX;
    private float cameraY;

    private Map<Integer, Integer> tileTextures = new HashMap<>();
    private Map<String, Integer> objectTextures = new HashMap<>();
    private boolean texturesLoaded = false;

    //layer
    private int[][][] mapLayers;
    private int layerCount;
    private boolean[] layerVisible;
    // Add these fields to your TileMap class

    private int currentLayer = 0;
    private int previousLayer = 0;
    private Map<Integer, List<MapObject>> layerObjects = new HashMap<>();

    private int defaultTextureId = -1;

    @PostConstruct
    public void init() {

    }

    public void loadMap(String filename) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("maps/" + filename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            // Read map dimensions and layer count
            String dimensionLine = reader.readLine();
            String[] dimensions = dimensionLine.split(",");
            mapWidth = Integer.parseInt(dimensions[0]);
            mapHeight = Integer.parseInt(dimensions[1]);
            layerCount = Integer.parseInt(dimensions[2]);

            // Initialize map data arrays
            mapLayers = new int[layerCount][mapHeight][mapWidth];
            layerVisible = new boolean[layerCount];
            for (int i = 0; i < layerCount; i++) {
                layerVisible[i] = true;
            }

            // Reset object collections
            objects.clear();
            layerObjects.clear();
            for (int i = 0; i < layerCount; i++) {
                layerObjects.put(i, new ArrayList<>());
            }

            // Read each layer's tile data
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

            // Read objects with layer information
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

                            // Add to the specific layer
                            if (objLayer >= 0 && objLayer < layerCount) {
                                layerObjects.get(objLayer).add(obj);
                            } else {
                                layerObjects.get(0).add(obj); // Default to first layer
                            }
                        }
                    }
                }
            }

            System.out.println("Map loaded: " + mapWidth + "x" + mapHeight + " with " + layerCount + " layers");
            System.out.println("Total object count: " + objects.size());

        } catch (Exception e) {
            System.err.println("Error loading map: " + e.getMessage());
            e.printStackTrace();
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


    public void checkPlayerPosition(float playerX, float playerY) {
        // Check objects only in the current layer
        List<MapObject> currentLayerObjects = layerObjects.getOrDefault(currentLayer, Collections.emptyList());

        for (MapObject obj : currentLayerObjects) {
            // Better collision detection with proper radius
            float distX = playerX - (obj.getX() + obj.getWidth() / 2);
            float distY = playerY - (obj.getY() + obj.getHeight() / 2);
            float distance = (float) Math.sqrt(distX * distX + distY * distY);

            // Collision detected if distance is less than combined radii
            if (distance < (tileSize / 2 + obj.getWidth() / 2)) {
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
        }
    }
    private boolean isTileSolid(int tileType) {
        return tileType == 1 || tileType == 4;
    }
    public void setLayerVisible(int layer, boolean visible) {
        if (layer >= 0 && layer < layerCount) {
            layerVisible[layer] = visible;
        }
    }
    private void renderLayerObjects(int layer) {
        List<MapObject> objectsToRender = layerObjects.getOrDefault(layer, Collections.emptyList());
        for (MapObject obj : objectsToRender) {
            renderObject(obj, obj.getX(), obj.getY());
        }
    }
    private MapObject createObject(int type, float x, float y) {
        switch (type) {
            case 1: // coin/map change item
                return new MapObject(x, y, 32, 32, "coin", "level2.csv");
            case 2: // Enemy
                return new MapObject(x, y, 32, 32, "enemy", null);
            case 3: // Layer portal (go to another layer)
                return new MapObject(x, y, 32, 32, "layerportal", null, 1); // Target layer 1
            case 4: // Layer return (go back to previous layer)
                return new MapObject(x, y, 32, 32, "layerreturn", null);
            default:
                return null;
        }
    }
    private void loadTextures() {
        if (texturesLoaded) return;

        System.out.println("Loading tile and object textures...");
        long startTime = System.currentTimeMillis();

        try {
            // Load default fallback texture once
//            defaultTextureId = TextureManager.loadTexture("maps/tiles/default.png", true);

            // Batch load all tile textures at once
            Map<String, Integer> tileTextureFiles = new HashMap<>();
            tileTextureFiles.put("maps/tiles/wall.png", 1);
            tileTextureFiles.put("maps/tiles/grass.png", 2);
            tileTextureFiles.put("maps/tiles/dirt.png", 3);
            tileTextureFiles.put("maps/tiles/water.png", 4);

            // Use parallel stream for batch loading if many textures
            tileTextureFiles.forEach((path, id) -> {
                try {
                    int textureId = TextureManager.loadTexture(path, true); // true = cache the texture
                    tileTextures.put(id, textureId);
                } catch (Exception e) {
                    System.err.println("Failed to load tile texture " + path + ": " + e.getMessage());
                    tileTextures.put(id, defaultTextureId); // Use fallback texture
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
    public void render(float playerX, float playerY, int screenWidth, int screenHeight) {
        if (!texturesLoaded) {
            loadTextures();
        }

        // Update camera position
        cameraX = playerX;
        cameraY = playerY;

        // Calculate visible area
        int startTileX = Math.max(0, (int)((cameraX - screenWidth/2) / tileSize));
        int endTileX = Math.min(mapWidth, (int)((cameraX + screenWidth/2) / tileSize) + 1);
        int startTileY = Math.max(0, (int)((cameraY - screenHeight/2) / tileSize));
        int endTileY = Math.min(mapHeight, (int)((cameraY + screenHeight/2) / tileSize) + 1);

        // Enable texturing
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        // Only render tiles from the current layer
        if (layerVisible[currentLayer]) {
            for (int y = startTileY; y < endTileY; y++) {
                for (int x = startTileX; x < endTileX; x++) {
                    int tileType = mapLayers[currentLayer][y][x];
                    if (tileType == 0) continue;

                    float drawX = x * tileSize;
                    float drawY = y * tileSize;

                    renderTile(tileType, drawX, drawY);

                    // Show collision boxes only if enabled
                    if (showCollision && isTileSolid(tileType)) {
                        renderCollisionBox(drawX, drawY, tileSize, tileSize);
                    }
                }
            }
        }

        // Only render objects from the current layer
        renderLayerObjects(currentLayer);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    private void renderObjects() {
        for (MapObject obj : objects) {
            renderObject(obj, obj.getX(), obj.getY());
        }
    }

    // Add this method to your TileMap class

    private Integer getObjectTexture(String objectType) {
        return objectTextures.get(objectType);
    }
    private void renderTile(int tileType, float x, float y) {
        // Bind the appropriate texture based on tile type
        Integer textureId = tileTextures.get(tileType);
        if (textureId != null) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
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
        }

        // Draw tile as a textured quad
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0); GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(1, 0); GL11.glVertex2f(x + tileSize, y);
        GL11.glTexCoord2f(1, 1); GL11.glVertex2f(x + tileSize, y + tileSize);
        GL11.glTexCoord2f(0, 1); GL11.glVertex2f(x, y + tileSize);
        GL11.glEnd();

        // Re-enable texturing if it was disabled
        if (textureId == null) {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor3f(1.0f, 1.0f, 1.0f); // Reset color
        }
    }

    private void renderCollisionBox(float x, float y, float width, float height) {
        // Save current color
        float[] currentColor = new float[4];
        GL11.glGetFloatv(GL11.GL_CURRENT_COLOR, currentColor);

        // Draw collision outline
        GL11.glColor4f(1.0f, 0.0f, 1.0f, 0.7f); // Magenta with transparency
        GL11.glLineWidth(2.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + width, y);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();

        // Restore previous color
        GL11.glColor4f(currentColor[0], currentColor[1], currentColor[2], currentColor[3]);
    }

    private void renderObject(MapObject obj, float screenX, float screenY) {
        // Bind the appropriate texture based on object type
        Integer textureId = getObjectTexture(obj.getType());
        if (textureId != null) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

            // Draw object as a textured quad
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0, 0); GL11.glVertex2f(screenX, screenY);
            GL11.glTexCoord2f(1, 0); GL11.glVertex2f(screenX + obj.getWidth(), screenY);
            GL11.glTexCoord2f(1, 1); GL11.glVertex2f(screenX + obj.getWidth(), screenY + obj.getHeight());
            GL11.glTexCoord2f(0, 1); GL11.glVertex2f(screenX, screenY + obj.getHeight());
            GL11.glEnd();
        } else {
            // Fallback to colored rectangles if texture not found
            GL11.glDisable(GL11.GL_TEXTURE_2D);

            if ("coin".equals(obj.getType())) {
                GL11.glColor3f(1.0f, 1.0f, 0.0f);  // Yellow for coins
            } else if ("enemy".equals(obj.getType())) {
                GL11.glColor3f(1.0f, 0.0f, 0.0f);  // Red for enemies
            } else {
                GL11.glColor3f(0.8f, 0.8f, 0.8f);  // Default gray
            }

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(screenX, screenY);
            GL11.glVertex2f(screenX + obj.getWidth(), screenY);
            GL11.glVertex2f(screenX + obj.getWidth(), screenY + obj.getHeight());
            GL11.glVertex2f(screenX, screenY + obj.getHeight());
            GL11.glEnd();

            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor3f(1.0f, 1.0f, 1.0f);  // Reset color
        }

        // Optionally render debug outline
        if (showCollision) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glColor3f(0.0f, 1.0f, 1.0f);  // Cyan
            GL11.glLineWidth(1.5f);
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glVertex2f(screenX, screenY);
            GL11.glVertex2f(screenX + obj.getWidth(), screenY);
            GL11.glVertex2f(screenX + obj.getWidth(), screenY + obj.getHeight());
            GL11.glVertex2f(screenX, screenY + obj.getHeight());
            GL11.glEnd();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor3f(1.0f, 1.0f, 1.0f);  // Reset color
        }
    }

    public boolean isSolid(float x, float y) {
        int tileX = (int)(x / tileSize);
        int tileY = (int)(y / tileSize);

        // Check bounds
        if (tileX < 0 || tileX >= mapWidth || tileY < 0 || tileY >= mapHeight) {
            return true; // Out of bounds is solid
        }

        // Check solid tiles in base layer (layer 0)
        int baseTileType = mapLayers[0][tileY][tileX];
        if (isTileSolid(baseTileType)) {
            return true;
        }

        // Check current layer if different from base
        if (currentLayer > 0) {
            int layerTileType = mapLayers[currentLayer][tileY][tileX];
            if (isTileSolid(layerTileType)) {
                return true;
            }
        }

        return false;
    }

    // Getters
    public int getTileSize() {
        return tileSize;
    }

    public int getMapWidth() {
        return mapWidth;
    }

    public int getMapHeight() {
        return mapHeight;
    }

    public void toggleCollisionView() {
        showCollision = !showCollision;
    }

    public boolean isShowingCollision() {
        return showCollision;
    }
}