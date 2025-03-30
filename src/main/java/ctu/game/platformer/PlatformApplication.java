package ctu.game.platformer;

import ctu.game.platformer.service.GameEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

@SpringBootApplication
public class PlatformApplication implements CommandLineRunner{

	@Autowired
	private GameEngine gameEngine;

	public static void main(String[] args) {
		SpringApplication.run(PlatformApplication.class, args);
	}

	@Override
	public void run(String... args) {
		Thread gameThread = new Thread(() -> {
			try {
				gameEngine.start();
			} catch (Exception e) {
				System.err.println("Game crashed: " + e.getMessage());
				e.printStackTrace();
			}
		});
		gameThread.setName("Game-Thread");
		gameThread.start();
	}
}
