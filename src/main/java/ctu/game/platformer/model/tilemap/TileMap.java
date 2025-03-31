package ctu.game.platformer.model.tilemap;

import ctu.game.platformer.model.common.GameObject;
import ctu.game.platformer.model.common.Position;
import org.lwjgl.opengl.GL11;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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
            case 1: // Coin
                return new MapObject(x, y, 16, 16, "coin");
            case 2: // Enemy
                return new MapObject(x, y, 32, 32, "enemy");
            default:
                return null;
        }
    }

    public void render(float playerX, float playerY, int screenWidth, int screenHeight) {
        // Update camera position to follow player
        cameraX = playerX;
        cameraY = playerY;

        // Calculate visible area
        int startTileX = Math.max(0, (int)((cameraX - screenWidth/2) / tileSize));
        int endTileX = Math.min(mapWidth, (int)((cameraX + screenWidth/2) / tileSize) + 1);
        int startTileY = Math.max(0, (int)((cameraY - screenHeight/2) / tileSize));
        int endTileY = Math.min(mapHeight, (int)((cameraY + screenHeight/2) / tileSize) + 1);

        // Render visible tiles
        for (int y = startTileY; y < endTileY; y++) {
            for (int x = startTileX; x < endTileX; x++) {
                int tileType = mapData[y][x];
                if (tileType == 0) continue; // Skip empty tiles

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
    }

    private void renderTile(int tileType, float x, float y) {
        // Set color based on tile type
        switch (tileType) {
            case 1: // Wall
                GL11.glColor3f(0.5f, 0.5f, 0.5f); // Gray
                break;
            case 2: // Grass
                GL11.glColor3f(0.0f, 0.8f, 0.0f); // Green
                break;
            case 3: // Dirt
                GL11.glColor3f(0.6f, 0.3f, 0.0f); // Brown
                break;
            case 4: // Water
                GL11.glColor3f(0.0f, 0.0f, 0.8f); // Blue
                break;
            default:
                GL11.glColor3f(1.0f, 1.0f, 1.0f); // White
        }

        // Draw tile
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + tileSize, y);
        GL11.glVertex2f(x + tileSize, y + tileSize);
        GL11.glVertex2f(x, y + tileSize);
        GL11.glEnd();
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
        // Set color based on object type
        if ("coin".equals(obj.getType())) {
            GL11.glColor3f(1.0f, 1.0f, 0.0f); // Yellow
        } else if ("enemy".equals(obj.getType())) {
            GL11.glColor3f(1.0f, 0.0f, 0.0f); // Red
        }

        // Draw object
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(obj.getX(), obj.getY());
        GL11.glVertex2f(obj.getX() + obj.getWidth(), obj.getY());
        GL11.glVertex2f(obj.getX() + obj.getWidth(), obj.getY() + obj.getHeight());
        GL11.glVertex2f(obj.getX(), obj.getY() + obj.getHeight());
        GL11.glEnd();
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