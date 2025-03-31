package ctu.game.platformer.model.tilemap;

import ctu.game.platformer.model.common.GameObject;
import jakarta.annotation.PostConstruct;
import org.lwjgl.opengl.GL11;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @PostConstruct
    public void init() {

    }

    public void loadMap(String filename) {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("maps/" + filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            // Read dimensions
            String[] dimensions = reader.readLine().split(",");
            mapWidth = Integer.parseInt(dimensions[0]);
            mapHeight = Integer.parseInt(dimensions[1]);

            // Initialize map array
            mapData = new int[mapHeight][mapWidth];

            // Read tile data
            for (int y = 0; y < mapHeight; y++) {
                String line = reader.readLine();
                String[] tiles = line.split(",");
                for (int x = 0; x < mapWidth && x < tiles.length; x++) {
                    mapData[y][x] = Integer.parseInt(tiles[x]);
                }
            }

            // Read objects if available
            String objectLine;
            while ((objectLine = reader.readLine()) != null && !objectLine.trim().isEmpty()) {
                String[] objData = objectLine.split(",");
                int type = Integer.parseInt(objData[0]);
                float x = Float.parseFloat(objData[1]);
                float y = Float.parseFloat(objData[2]);

                MapObject object = createObject(type, x, y);
                if (object != null) {
                    objects.add(object);
                }
            }

            reader.close();
            System.out.println("Map loaded: " + filename + " (" + mapWidth + "x" + mapHeight + ")");
        } catch (Exception e) {
            System.err.println("Error loading map: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private MapObject createObject(int type, float x, float y) {
        switch (type) {
            case 1: // item
                return new MapObject(x, y, 16, 16, "coin", "level2.csv"); // Set the map filename
            case 2: // Enemy
                return new MapObject(x, y, 32, 32, "enemy", null);
            default:
                return null;
        }
    }

    public void render(float playerX, float playerY, int screenWidth, int screenHeight) {
        // Update camera position to follow player

        if (!texturesLoaded) {
            try {
                // Load textures for tiles
                tileTextures.put(1, TextureManager.loadTexture("maps/tiles/wall.png"));
                tileTextures.put(2, TextureManager.loadTexture("maps/tiles/grass.png"));
                tileTextures.put(3, TextureManager.loadTexture("maps/tiles/dirt.png"));
                tileTextures.put(4, TextureManager.loadTexture("maps/tiles/water.png"));

                // Load textures for objects
                objectTextures.put("coin", TextureManager.loadTexture("textures/objects/coin.png"));
                objectTextures.put("enemy", TextureManager.loadTexture("textures/objects/enemy.png"));

                texturesLoaded = true;
            } catch (Exception e) {
                System.err.println("Error initializing textures: " + e.getMessage());
                e.printStackTrace();
            }
        }

        cameraX = playerX;
        cameraY = playerY;

        // Check if player reached the specific point to change the map
        checkPlayerPosition(playerX, playerY);

        // Calculate visible area
        int startTileX = Math.max(0, (int)((cameraX - screenWidth/2) / tileSize));
        int endTileX = Math.min(mapWidth, (int)((cameraX + screenWidth/2) / tileSize) + 1);
        int startTileY = Math.max(0, (int)((cameraY - screenHeight/2) / tileSize));
        int endTileY = Math.min(mapHeight, (int)((cameraY + screenHeight/2) / tileSize) + 1);

        // Enable texturing
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f); // Reset color to not tint textures

        // Render visible tiles
        for (int y = startTileY; y < endTileY; y++) {
            for (int x = startTileX; x < endTileX; x++) {
                int tileType = mapData[y][x];
                if (tileType == 0) continue;

                float drawX = x * tileSize;
                float drawY = y * tileSize;

                // Draw tile based on type
                renderTile(tileType, drawX, drawY);

                // Draw collision box if enabled and tile is solid
                if (showCollision && (tileType == 1 || tileType == 4)) {
                    renderCollisionBox(drawX, drawY, tileSize, tileSize);
                }
            }
        }

        // Render visible objects
        for (MapObject obj : objects) {
            float objX = obj.getX();
            float objY = obj.getY();

            // Only render objects close to the player
            if (Math.abs(objX - playerX) < screenWidth/2 + tileSize &&
                    Math.abs(objY - playerY) < screenHeight/2 + tileSize) {
                renderObject(obj);

                // Draw collision boxes for objects if enabled
                if (showCollision) {
                    renderCollisionBox(obj.getX(), obj.getY(), obj.getWidth(), obj.getHeight());
                }
            }
        }

        // Disable texturing when done
        GL11.glDisable(GL11.GL_TEXTURE_2D);
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

    private void renderObject(MapObject obj) {
        // Bind the appropriate texture based on object type
        Integer textureId = objectTextures.get(obj.getType());
        if (textureId != null) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        } else {
            // Fallback to colored rectangles if texture not found
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            if ("coin".equals(obj.getType())) {
                GL11.glColor3f(1.0f, 1.0f, 0.0f); // Yellow
            } else if ("enemy".equals(obj.getType())) {
                GL11.glColor3f(1.0f, 0.0f, 0.0f); // Red
            }
        }

        // Draw object as a textured quad
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0); GL11.glVertex2f(obj.getX(), obj.getY());
        GL11.glTexCoord2f(1, 0); GL11.glVertex2f(obj.getX() + obj.getWidth(), obj.getY());
        GL11.glTexCoord2f(1, 1); GL11.glVertex2f(obj.getX() + obj.getWidth(), obj.getY() + obj.getHeight());
        GL11.glTexCoord2f(0, 1); GL11.glVertex2f(obj.getX(), obj.getY() + obj.getHeight());
        GL11.glEnd();

        // Re-enable texturing if it was disabled
        if (textureId == null) {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor3f(1.0f, 1.0f, 1.0f); // Reset color
        }
    }

    public boolean isSolid(float x, float y) {
        int tileX = (int)(x / tileSize);
        int tileY = (int)(y / tileSize);

        // Check bounds
        if (tileX < 0 || tileX >= mapWidth || tileY < 0 || tileY >= mapHeight) {
            return true; // Out of bounds is solid
        }

        // Consider specific tile types as solid
        int tileType = mapData[tileY][tileX];
        return tileType == 1 || tileType == 4; // Wall and water are solid
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