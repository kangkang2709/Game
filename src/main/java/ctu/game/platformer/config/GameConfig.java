// src/main/java/ctu/game/flatformer/config/GameConfig.java
package ctu.game.platformer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GameConfig {

    @Bean
    public int windowWidth() {
        return 800;
    }

    @Bean
    public int windowHeight() {
        return 600;
    }

    @Bean
    public String windowTitle() {
        return "Platformer + Visual Novel";
    }

    @Bean
    public boolean vSync() {
        return true;
    }
    // Add to GameConfig.java
    @Bean
    public double targetFps() {
        return 60.0; // Default to 60 FPS
    }

    @Bean
    public double updateRate() {
        return 60.0; // Updates per second (game logic)
    }
}