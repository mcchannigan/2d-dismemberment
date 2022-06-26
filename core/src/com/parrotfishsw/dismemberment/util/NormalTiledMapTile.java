package com.parrotfishsw.dismemberment.util;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;

public class NormalTiledMapTile extends StaticTiledMapTile {
	private TextureRegion normalRegion;
	
	public NormalTiledMapTile(NormalTiledMapTile copy) {
		super(copy);
		normalRegion = copy.normalRegion;
	}

	public NormalTiledMapTile(TextureRegion region, TextureRegion normal) {
		super(region);
		normalRegion = normal;
	}
	
	public TextureRegion getNormalRegion() {
		return normalRegion;
	}
}
