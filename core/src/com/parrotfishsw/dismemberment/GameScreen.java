package com.parrotfishsw.dismemberment;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Logger;
import com.esotericsoftware.spine.SpineSpriteBatch;
import com.esotericsoftware.spine.attachments.PhysicsBoundingBoxAttachment;
import com.parrotfishsw.dismemberment.input.ControllerHandler;
import com.parrotfishsw.dismemberment.input.GameInputHandler;
import com.parrotfishsw.dismemberment.input.HudInputHandler;
import com.parrotfishsw.dismemberment.item.Armor;
import com.parrotfishsw.dismemberment.item.Shield;
import com.parrotfishsw.dismemberment.item.Weapon;
import com.parrotfishsw.dismemberment.util.NormalTiledMapRenderer;
import com.parrotfishsw.dismemberment.util.ParticleEffectBox2d;
import com.parrotfishsw.dismemberment.util.ParticleEffectPoolBox2d;
import com.parrotfishsw.dismemberment.util.ParticleEffectPoolBox2d.PooledEffectBox2d;
import com.parrotfishsw.lumberjack.box2d.LjCollisionListener;
import com.parrotfishsw.lumberjack.entities.CuttableObject;
import com.parrotfishsw.lumberjack.entities.DuelistAI;
import com.parrotfishsw.lumberjack.entities.Enemy;
import com.parrotfishsw.lumberjack.entities.Person;
import com.parrotfishsw.lumberjack.entities.Player;
import com.parrotfishsw.lumberjack.map.MapBodyManager;

public class GameScreen implements Screen {
	final DismembermentGame game;
	final FPSLogger fpsLog = new FPSLogger();
	final int PAUSED = 1;
	Logger log = new Logger("GameScreen", Logger.INFO);

	private OrthographicCamera camera;
	private SpineSpriteBatch spineBatch;
	private SpriteBatch batch;
	private ShapeRenderer shapeRenderer;
	private ShaderProgram shader;
	private float[] lpos = null;
	private float[] lcol = null;

	public Vector3 lightDiff = new Vector3(0f, 0f, 0f);
	float defaultZ = 0.08f;
	float defaultX = 1f;
	float defaultY = 0f;
	final Vector3 ambientColor = new Vector3(1, 1, 1);
	final Vector2 resolution = new Vector2();
	final Vector3 attenuation = new Vector3(0.2f, 0.1f, 0.7f);
	// final Matrix3 normalMatrix = new Matrix3();

	private int counter = 0;
	private int state = 0;
	World world;
	DirectionalLight directLight = null;
	Array<PointLight> lights = new Array<PointLight>();
	NormalTiledMapRenderer mapRenderer;
	HUDRenderer hudRenderer;
	MapBodyManager mbMgr;
	LjCollisionListener collisionListener = null;
	Box2DDebugRenderer ren;
	Vector3 camTarget = new Vector3();
	public Vector2 center = new Vector2(0, 0);
	public float zoom = 0;

	public Player player;
	Array<Enemy> enemies = new Array<Enemy>();
	public static ParticleEffectPoolBox2d bloodPool;
	public static ParticleEffectPoolBox2d sparkPool;
	public static final Array<PooledEffectBox2d> effects = new Array<PooledEffectBox2d>();
	TextureAtlas effectsAtlas;

	public GameScreen(final DismembermentGame game, String mapname) {
		this.game = game;
		shader = createShader("shaders/normal-vert.s", "shaders/normal-frag.s");
		batch = new SpriteBatch();
		spineBatch = new SpineSpriteBatch(1000, shader);
		// batch.setShader(shader);
		shapeRenderer = new ShapeRenderer();

		int w = Gdx.graphics.getWidth();
		int h = Gdx.graphics.getHeight();
		resolution.set(w, h);
		// Setup camera and physics
		float vpw = DsConstants.METERS_PER_PIXEL * 3 * 1280;// w;
		camera = new OrthographicCamera(vpw, vpw * ((float) h) / w);
		// camera.setToOrtho(false);
		// camera.zoom -= 0.9f;
		camera.update();
		Person.camera = camera;
		world = new World(new Vector2(0, -30), true);
		float mapUnits = DsConstants.METERS_PER_PIXEL * 10;
		mbMgr = new MapBodyManager(world, null, Logger.DEBUG, mapUnits);

		// Setup particle effects TODO fix particle loaders
		ParticleEffectBox2d blood = new ParticleEffectBox2d(world);
		ParticleEffectBox2d sparks = new ParticleEffectBox2d(world, false);
		blood.load(Gdx.files.internal("data/particles/blood1.p"),
				(TextureAtlas) game.manager.get("data/particles/effects.atlas"));
		sparks.load(Gdx.files.internal("data/particles/spark.p"),
				Gdx.files.internal("data/particles"));
		bloodPool = new ParticleEffectPoolBox2d(blood, 1, 2);
		sparkPool = new ParticleEffectPoolBox2d(sparks, 1, 2);

		// Get map
		TiledMap map = game.manager.get(mapname);
		ShaderProgram mapShader = createShader("shaders/map-vert.s",
				"shaders/map-frag.s");
		mapRenderer = new NormalTiledMapRenderer(map, mapUnits, mapShader);
		mbMgr.createPhysics(map);
		mapRenderer.setView(camera);

		/* TODO: test bed. Delete this section later */
		PointLight light = new PointLight();
		light.color.set(1.0f, 1.0f, 1.0f, 1.0f);
		lights.add(light);
		Weapon claymore = new Weapon("claymore");
		Weapon spear = new Weapon("spear");
		Weapon sword = new Weapon("shortsword");
		Armor helm = new Armor("knight-helm");
		Armor plate = new Armor("knight-breastplate");
		Armor gloves = new Armor("knight-gauntlets");
		Armor leggings = new Armor("knight-leggings");
		Armor eHelm = new Armor("fullhelm");
		Shield shield = new Shield("shield");
		/* End test bed */
		float playerX = (Float) map.getProperties().get("px");
		float playerY = (Float) map.getProperties().get("py");
		player = new Player(world);
		player.setPosition(playerX, playerY);
		camera.position.set(player.getX(), player.getCenterY(), 0);

		for (Vector2 pos : mbMgr.enemyPositions) {
			Enemy enemy = new Enemy(player, null);
			enemy.setAI(new DuelistAI(enemy));
			enemy.setPosition(pos.x, pos.y);
			enemy.equipWeapon(new Weapon(spear));
			enemy.equipArmor(new Armor(eHelm));
			enemy.equipArmor(new Armor(leggings));
			enemies.add(enemy);
		}

		player.inventory.addEquipment(claymore);
		player.inventory.addEquipment(spear);
		player.inventory.addEquipment(shield);
		player.inventory.addEquipment(helm);
		player.inventory.addEquipment(plate);
		player.inventory.addEquipment(gloves);
		player.inventory.addEquipment(leggings);
		player.equipWeapon(claymore);
		player.equipShield(shield);
		player.equipArmor(helm);
		player.equipArmor(plate);
		player.equipArmor(gloves);
		player.equipArmor(leggings);

		hudRenderer = new HUDRenderer(resolution, player, enemies,
				game.manager.get("ui.atlas", TextureAtlas.class), game.batch);
		// Setup input listeners and config
		InputMultiplexer inptMulti = new InputMultiplexer();
		inptMulti.addProcessor(new GameInputHandler(this));
		inptMulti.addProcessor(new HudInputHandler(hudRenderer, this));
		hudRenderer.setupInput(inptMulti);
		Gdx.input.setInputProcessor(inptMulti);
		ControllerHandler handler = new ControllerHandler(this);
		Controllers.addListener(handler);

		// Setup contact listener
		collisionListener = new LjCollisionListener(player, world);
		world.setContactListener(collisionListener);
		Gdx.input.setCursorCatched(true);
		center.set(w / 2, h / 2);
		Gdx.input.setCursorPosition(w / 2, h / 2);
		Gdx.input.setCatchBackKey(true);
		Gdx.input.setCatchMenuKey(true);

		ren = new Box2DDebugRenderer();
	}

	@Override
	public void dispose() {
		// mbMgr.destroyPhysics();
		spineBatch.dispose();
		hudRenderer.dispose();
		world.dispose();
	}

	@Override
	public void render(float delta) {
		if (state != PAUSED) {
			player.doFrame(delta);
			for (Enemy enemy : enemies) {
				if (player.alive()) {
					enemy.checkAggro(player);
				}
				enemy.doFrame(delta);
			}
		}

		Vector3 lightPosition = lights.first().position;

		Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		/*
		 * Gdx.gl.glClearDepthf(1f); Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
		 * Gdx.gl.glDepthFunc(GL20.GL_LESS);
		 * Gdx.gl.glEnable(GL20.GL_DEPTH_TEST); Gdx.gl.glDepthMask(true);
		 * Gdx.gl.glColorMask(false, false, false, false);
		 */

		if (player.alive()) {
			float off = 15 * player.getFacing();
			float yDiff = 0;/*
							 * player.getTorsoAngle(); if(Math.abs(yDiff) >
							 * 0.2f) { yDiff *= (yDiff * 10); } else { yDiff =
							 * 0; }
							 */
			camTarget.set(player.getX() + off, player.getCenterY() + yDiff, 0);
			float dist = camTarget.dst(camera.position);
			if (dist > DsConstants.CAM_SPD) {
				camera.position.set(camera.position.lerp(camTarget,
						DsConstants.CAM_SPD / dist));
			} else {
				camera.position.set(camTarget);
			}
		}
		defaultX += lightDiff.x;
		defaultY += lightDiff.y;
		defaultZ += lightDiff.z;
		lightPosition.set(defaultX, defaultY, defaultZ);
		// camera.zoom += zoom;
		camera.update();

		spineBatch.setProjectionMatrix(camera.combined);
		batch.setProjectionMatrix(camera.combined);

		shapeRenderer.setProjectionMatrix(camera.combined);
		shapeRenderer.setColor(Color.RED);
		/*
		 * shapeRenderer.begin(ShapeType.Filled); // log.info("GL Error: " +
		 * Gdx.gl20.glGetError()); player.drawDepthPrimitives(shapeRenderer);
		 * for (Enemy enemy : enemies) {
		 * enemy.drawDepthPrimitives(shapeRenderer); } shapeRenderer.end();
		 */

		// Gdx.gl.glDepthFunc(GL20.GL_ALWAYS);
		// Draw unmasked objects
		mapRenderer.setView(camera);
		mapRenderer.render(this);
		spineBatch.begin();
		setDefaultUniforms(shader);
		player.drawUnmasked(spineBatch, shader);
		for (Enemy enemy : enemies) {
			enemy.drawUnmasked(spineBatch, shader);
		}
		spineBatch.end();
		batch.begin();
		for (int i = effects.size - 1; i >= 0; i--) {
			PooledEffectBox2d effect = effects.get(i);
			effect.draw(batch, delta);
			if (effect.isComplete()) {
				effect.free();
				effects.removeIndex(i);
			}
		}
		batch.end();
		hudRenderer.render(state == PAUSED);

		if (GameConfig.Video.debug) {
			shapeRenderer.begin(ShapeType.Filled);
			shapeRenderer.circle(defaultX, defaultY, 0.5f, 12);
			shapeRenderer.end();

			ren.render(world, camera.combined);
		}
		fpsLog.log();

		lpos = null;
		lcol = null;

		if (state != PAUSED) {
			checkAndSliceObjects();
			world.step(1 / 60f, 6, 2);
		}
	}

	@Override
	public void resize(int width, int height) {
		center.set(width / 2, height / 2);
		resolution.set(width, height);
		hudRenderer.resize(width, height);
	}

	@Override
	public void show() {
	}

	@Override
	public void hide() {
	}

	@Override
	public void pause() {
		Gdx.input.setCursorCatched(false);
		state = PAUSED;
	}

	public boolean isPaused() {
		return state == PAUSED;
	}

	@Override
	public void resume() {
		Gdx.input.setCursorCatched(!hudRenderer.menuOpen);
		state = 0;
	}

	public void checkAndSliceObjects() {
		Array<Body> bodies = new Array<Body>(world.getBodyCount());
		world.getBodies(bodies);
		byte count = 0; // to determine count of cycles checking for new
						// child/parent combos
		// If child bodies in the map loader in future switch to welded dynamic
		// bodies, this count
		// logic will no longer be necessary
		for (Body b : bodies) {
			Object data = b.getUserData();
			if (data == null)
				continue;
			if (data instanceof CuttableObject) {
				CuttableObject obj = (CuttableObject) data;

				if ((obj.flag & CuttableObject.FIND_PARENT) > 0) {
					count = 1;
					if (counter > 0) {
						obj.flag ^= CuttableObject.FIND_PARENT;
					}
				}
				if ((obj.flag & CuttableObject.FIND_CHILD) > 0) {
					count = 1;
					if (counter > 0) {
						count = 2;
						obj.flag ^= CuttableObject.FIND_CHILD;
					}
				}

				if (obj.sliceEntered && obj.sliceExited) {
					obj.slice();
				} else {
					obj.sliceEntered = obj.sliceExited = false;
				}

				if ((obj.flag & CuttableObject.WELD_PARENT) > 0) {
					obj.weldToParent(world);
					obj.parent.children.add(obj);
					obj.flag ^= CuttableObject.WELD_PARENT;
				}

			} else if (data instanceof PhysicsBoundingBoxAttachment) {
				PhysicsBoundingBoxAttachment bb = (PhysicsBoundingBoxAttachment) data;
				if (bb.sliceEntered && !bb.person.alive()) {
					if (!bb.isArmor() && bb.sliceExited) {
						bb.person.ragdoll = true;
						bb.slice();
					} else {

						bb.sliceEntered = bb.sliceExited = false;
					}
				} else {
					bb.sliceEntered = bb.sliceExited = false;
				}
			}
		}

		if (count == 1) {
			counter++;
		} else if (count == 2) {
			counter = 0;
		}
	}

	public void togglePause() {
		if (state == PAUSED) {
			resume();
		} else {
			pause();
		}
	}

	public void toggleMenu() {
		hudRenderer.toggleMenu();
	}

	public void zoom(float diff) {
		camera.zoom += diff;
		camera.update();
	}

	public ShaderProgram createShader(String vertPath, String fragPath) {
		String vert = Gdx.files.internal(vertPath).readString();

		String frag = Gdx.files.internal(fragPath).readString();

		// System.out.println("VERTEX PROGRAM:\n------------\n\n" + vert);
		// System.out.println("FRAGMENT PROGRAM:\n------------\n\n" + frag);
		ShaderProgram program = new ShaderProgram(vert, frag);
		ShaderProgram.pedantic = false;
		if (!program.isCompiled())
			throw new IllegalArgumentException("Error compiling shader: "
					+ program.getLog());

		log.info(program.getLog());
		program.begin();
		program.setUniformi("u_texture", 0);
		program.setUniformi("u_normals", 1);
		program.end();

		return program;
	}

	public void setDefaultUniforms(ShaderProgram shader) {
		if (lpos == null) {
			Object[] lightArrays = makeLightArrays();
			lpos = (float[]) lightArrays[0];
			lcol = (float[]) lightArrays[1];
		}
		shader.setUniformf("resolution", resolution);
		shader.setUniformf("ambientColor", ambientColor);
		shader.setUniformf("ambientIntensity", 0.25f);
		shader.setUniformf("attenuation", attenuation);
		shader.setUniformi("lightCount", lights.size);
		shader.setUniform3fv("lights[0]", lpos, 0, lpos.length);
		shader.setUniform3fv("lightColors[0]", lcol, 0, lcol.length);
	}

	private Object[] makeLightArrays() {
		float[] colors = new float[lights.size * 3];
		float[] positions = new float[lights.size * 3];
		int i = 0;
		int j = 0;
		for (PointLight light : lights) {
			colors[i++] = light.color.r;
			colors[i++] = light.color.g;
			colors[i++] = light.color.b;

			float z = light.position.z;
			camera.project(light.position);
			light.position.z = z;
			positions[j++] = light.position.x;
			positions[j++] = light.position.y;
			positions[j++] = light.position.z;
		}
		return new Object[] { positions, colors };
	}
}
