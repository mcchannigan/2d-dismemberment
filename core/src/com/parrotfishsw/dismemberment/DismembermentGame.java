package com.parrotfishsw.dismemberment;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.utils.I18NBundle;
import com.parrotfishsw.dismemberment.util.NormalTiledMapLoader;

public class DismembermentGame extends Game {
	public AssetManager manager = null;
	public SpriteBatch batch;

	public void create() {
		batch = new SpriteBatch();
		manager = new AssetManager();
		manager.setLoader(TiledMap.class, ".tmx", new NormalTiledMapLoader());
		DsConstants.i18nBundle = I18NBundle.createBundle(Gdx.files.internal("strings/strings"));
		// Queue up loading screen assets and block until loaded
		TextureParameter param = new TextureParameter();
		param.minFilter = TextureFilter.Linear;
		param.genMipMaps = true;
		manager.load("data/particles/effects.atlas", TextureAtlas.class);
		manager.load("ui.atlas", TextureAtlas.class);
		//manager.load("data/diffuse.atlas", TextureAtlas.class);
		//manager.load("data/normals.atlas", TextureAtlas.class);
		manager.finishLoading();

		//Gdx.graphics.setDisplayMode(GameConfig.Video.width, GameConfig.Video.height, GameConfig.Video.fullscreen);
		if(GameConfig.Video.fullscreen) {
			//Gdx.graphics.setFullscreenMode(new DisplayMode());
		} else {
			Gdx.graphics.setWindowedMode(GameConfig.Video.width, GameConfig.Video.height);
		}
		// Queue up the rest of the assets to be loaded
		//manager.setLoader(TiledMap.class, new TmxMapLoader(new InternalFileHandleResolver()));
		//manager.load("data/test.tmx", TiledMap.class);

		setScreen(new MainMenuScreen(this));
	}

	public void pause() {
		super.pause();
	}

	public void resume() {
		super.resume();
	}

	public void dispose() {
		manager.dispose();
		batch.dispose();
	}

	public void render() {
		super.render();
	}
}
