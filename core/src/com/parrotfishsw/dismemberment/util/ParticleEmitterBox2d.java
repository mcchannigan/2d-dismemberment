package com.parrotfishsw.dismemberment.util;

import java.io.BufferedReader;
import java.io.IOException;

import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.parrotfishsw.dismemberment.DsConstants;

/**
 * @author kalle_h
 * 
 *         ParticleEmitterBox2D use box2d rayCast:ing to achieve continuous
 *         collision detection against box2d fixtures. If particle detect
 *         collision it change it's direction before actual collision would
 *         occur. Velocity is 100% reflected.
 * 
 *         These particles does not have any other physical attributes or
 *         functionality. Particles can't collide to other particles.
 */
public class ParticleEmitterBox2d extends ParticleEmitter {
	final World world;
	final Vector2 startPoint = new Vector2();
	final Vector2 endPoint = new Vector2();
	/** collision flag */
	boolean particleCollided;
	float normalAngle;
	/**
	 * If velocities squared is shorter than this it could lead 0 length rayCast
	 * that cause c++ assertion at box2d
	 */
	private final static float EPSILON = 0.001f;

	/** default visibility to prevent synthetic accessor creation */
	final RayCastCallback rayCallBack = new RayCastCallback() {
		public float reportRayFixture(Fixture fixture, Vector2 point,
				Vector2 normal, float fraction) {
			if ((fixture.getFilterData().categoryBits & DsConstants.Categories.MAP) > 0) {
				ParticleEmitterBox2d.this.particleCollided = true;
				ParticleEmitterBox2d.this.normalAngle = MathUtils.atan2(
						normal.y, normal.x) * MathUtils.radiansToDegrees;
			}
			return fraction;
		}
	};

	/**
	 * Constructs default ParticleEmitterBox2D. Box2d World is used for
	 * rayCasting. Assumes that particles use same unit system that box2d world
	 * does.
	 * 
	 * @param world
	 */
	public ParticleEmitterBox2d(World world) {
		super();
		this.world = world;
	}

	/**
	 * /**Constructs ParticleEmitterBox2D using bufferedReader. Box2d World is
	 * used for rayCasting. Assumes that particles use same unit system that
	 * box2d world does.
	 * 
	 * @param world
	 * @param reader
	 * @throws IOException
	 */
	public ParticleEmitterBox2d(World world, BufferedReader reader)
			throws IOException {
		super(reader);
		this.world = world;
	}

	/**
	 * Constructs ParticleEmitterBox2D fully copying given emitter attributes.
	 * Box2d World is used for rayCasting. Assumes that particles use same unit
	 * system that box2d world does.
	 *
	 * @param emitter
	 */
	public ParticleEmitterBox2d(ParticleEmitterBox2d emitter) {
		super(emitter);
		this.world = emitter.world;
	}

	@Override
	protected Particle newParticle(Sprite sprite) {
		return new ParticleBox2D(sprite);
	}

	/** Particle that can collide to box2d fixtures */
	private class ParticleBox2D extends Particle {
		int collideLife = 1800;

		public ParticleBox2D(Sprite sprite) {
			super(sprite);
		}

		/**
		 * translate particle given amount. Continuous collision detection
		 * achieved by using RayCast from oldPos to newPos.
		 * 
		 * @param velocityX
		 * @param velocityY
		 */
		@Override
		public void translate(float velocityX, float velocityY) {
			/**
			 * If velocities squares summed is shorter than Epsilon it could
			 * lead ~0 length rayCast that cause nasty c++ assertion inside
			 * box2d. This is so short distance that moving particle has no
			 * effect so this return early.
			 */
			if ((velocityX * velocityX + velocityY * velocityY) < EPSILON)
				return;

			/** Position offset is half of sprite texture size. */
			final float x = getX() + getWidth() / 2f;
			final float y = getY() + getHeight() / 2f;

			/** collision flag to false */
			particleCollided = false;
			startPoint.set(x, y);
			endPoint.set(x + velocityX, y + velocityY);
			if (world != null)
				world.rayCast(rayCallBack, startPoint, endPoint);

			/** If ray collided boolean has set to true at rayCallBack */
			if (collideLife > 0) {
				if (particleCollided) {
					// destroy particle and make spot on ground
					velocityX = MathUtils.random();
					velocityY = MathUtils.random();
					currentLife = collideLife;
					collideLife = 0;
				}
			} else {
				velocityX = velocityY = 0;
			}
			super.translate(velocityX, velocityY);
		}

		@Override
		public void setScale(float scale) {
			super.setScale(scale);
			// hack to set collideLife, since setScale is the first method
			// called on a particle after initialization by the ParticleEmitter
			if (currentLife == life) {
				collideLife = life * 2;
			}
		}

	}
}
