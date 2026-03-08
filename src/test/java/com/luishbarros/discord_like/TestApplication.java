package com.luishbarros.discord_like;

import org.springframework.boot.SpringApplication;

public class TestApplication {

	public static void main(String[] args) {
		SpringApplication
				.from(DiscordLikeApplication::main)
				.with(TestcontainersConfiguration.class)
				.run(args);
	}
}