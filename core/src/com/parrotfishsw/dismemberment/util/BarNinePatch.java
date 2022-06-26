package com.parrotfishsw.dismemberment.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;

public class BarNinePatch extends NinePatch {
	private static BarNinePatch instance;

	private BarNinePatch() {
		super(new Texture(Gdx.files.internal("bar.png")), 18, 18, 18, 18);
	}
	
	public static BarNinePatch getInstance() {
		if(instance == null) {
			instance = new BarNinePatch();
		}
		return instance;
	}
}
