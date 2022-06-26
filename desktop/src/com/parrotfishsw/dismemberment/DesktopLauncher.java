package com.parrotfishsw.dismemberment;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.parrotfishsw.dismemberment.DismembermentGame;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.title = "Dismemberment Game";
		new LwjglApplication(new DismembermentGame(), config);
	}
}
