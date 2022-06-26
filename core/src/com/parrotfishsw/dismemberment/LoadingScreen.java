package com.parrotfishsw.dismemberment;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.parrotfishsw.dismemberment.util.NormalTiledMapLoader;

public class LoadingScreen implements Screen {
	final DismembermentGame game;
	String mapName;

	OrthographicCamera camera;

	/**
	 * Initialize a loading screen to be shown while the AssetManager loads data
	 * 
	 * @param gm
	 *            A reference to the main Game object
	 * @param splash
	 *            If true, the loading screen will show the splash image. This
	 *            is used when the game first runs. Subsequent loading screens
	 *            should pass false to show the in-game loading screen
	 */
	public LoadingScreen(final DismembermentGame gm, String mapname, boolean splash) {
		game = gm;
		
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		mapName = mapname;
		game.manager.setLoader(TiledMap.class, new NormalTiledMapLoader(new InternalFileHandleResolver()));
		game.manager.load(mapName, TiledMap.class);
		
		camera = new OrthographicCamera(1, h/w);
	}

	@Override
	public void render(float delta) {
		if (game.manager.update()) {
			game.setScreen(new GameScreen(game, mapName));
			dispose();
		}

		// Show loading screen
		Gdx.gl.glClearColor(0, 0, 0.2f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		camera.update();
		game.batch.setProjectionMatrix(camera.combined);

		game.batch.begin();
		// do drawing
		game.batch.end();
	}

	@Override
	public void resize(int width, int height) {
	}

	@Override
	public void show() {
	}

	@Override
	public void hide() {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void dispose() {
	}

}
