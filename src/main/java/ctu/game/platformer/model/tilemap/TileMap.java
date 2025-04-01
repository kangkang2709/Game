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


    private int defaultTextureId = -1;

    @PostConstruct
    public void init() {

    }

    public void loadMap(String filename) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("maps/" + filename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            // Đọc kích thước bản đồ và số layer
            String[] dimensions = reader.readLine().split(",");
            mapWidth = Integer.parseInt(dimensions[0]);
            mapHeight = Integer.parseInt(dimensions[1]);
            layerCount = (dimensions.length > 2) ? Integer.parseInt(dimensions[2]) : 1;

            // Khởi tạo các mảng bản đồ
            mapLayers = new int[layerCount][mapHeight][mapWidth];
            mapData = new int[mapHeight][mapWidth]; // Tương thích với code cũ
            layerVisible = new boolean[layerCount];
            Arrays.fill(layerVisible, true); // Mặc định tất cả layer hiển thị

            // Đọc dữ liệu từng layer
            for (int layer = 0; layer < layerCount; layer++) {
                for (int y = 0; y < mapHeight; y++) {
                    String[] tiles = reader.readLine().split(",");
                    for (int x = 0; x < Math.min(mapWidth, tiles.length); x++) {
                        int tileValue = Integer.parseInt(tiles[x]);
                        mapLayers[layer][y][x] = tileValue;

                        // Tương thích với mapData, chỉ ghi đè nếu cần
                        if (layer == 0 || (isTileSolid(tileValue) && mapData[y][x] == 0)) {
                            mapData[y][x] = tileValue;
                        }
                    }
                }
                // Bỏ qua dòng trống giữa các layer (nếu có)
                if (layer < layerCount - 1) {
                    reader.readLine();
                }
            }

            // Xóa danh sách object cũ trước khi nạp mới
            objects.clear();

            // Đọc dữ liệu đối tượng (nếu có)
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    String[] parts = line.split(",");
                    if (parts.length >= 3) {
                        int type = Integer.parseInt(parts[0]);
                        float objX = Float.parseFloat(parts[1]);
                        float objY = Float.parseFloat(parts[2]);

                        MapObject obj = createObject(type, objX, objY);
                        if (obj != null) {
                            objects.add(obj);
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error loading map: " + e.getMessage());
            e.printStackTrace();
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

    private MapObject createObject(int type, float x, float y) {
        switch (type) {
            case 1: // item
                return new MapObject(x, y, 32, 32, "coin", "level2.csv"); // Set the map filename
            case 2: // Enemy
                return new MapObject(x, y, 32, 32, "enemy", null);
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
            defaultTextureId = TextureManager.loadTexture("maps/tiles/default.png", true);

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

            texturesLoaded = true;
            long endTime = System.currentTimeMillis();
            System.out.println("Textures loaded in " + (endTime - startTime) + "ms");
        } catch (Exception e) {
            System.err.println("Error initializing textures: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void render(float playerX, float playerY, int screenWidth, int screenHeight) {
        // Update camera position
        if (!texturesLoaded) {
            loadTextures();
        }


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

        // Render each layer
        for (int layer = 0; layer < layerCount; layer++) {
            for (int y = startTileY; y < endTileY; y++) {
                for (int x = startTileX; x < endTileX; x++) {
                    int tileType = mapLayers[layer][y][x];
                    if (tileType == 0) continue;

                    float drawX = x * tileSize;
                    float drawY = y * tileSize;

                    renderTile(tileType, drawX, drawY);

                    // Only show collision on the collision layer (typically first layer)
                    if (showCollision && layer == 0 && isTileSolid(tileType)) {
                        renderCollisionBox(drawX, drawY, tileSize, tileSize);
                    }
                }
            }
        }


        renderObjects();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    private void renderObjects() {
        for (MapObject obj : objects) {
            renderObject(obj, obj.getX(), obj.getY());
        }
    }

    private void checkPlayerPosition(float playerX, float playerY) {
        for (MapObject obj : objects) {
            // Check if player collides with the specific object type (e.g., "coin")
            if ("coin".equals(obj.getType()) &&
                    Math.abs(playerX - obj.getX()) < tileSize &&
                    Math.abs(playerY - obj.getY()) < tileSize) {
                // Load the corresponding map
                loadMap(obj.getMapFilename());
                break;
            }
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

        // Look for solid tiles in any layer
        for (int layer = 0; layer < layerCount; layer++) {
            int tileType = mapLayers[layer][tileY][tileX];
            if (isTileSolid(tileType)) {
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