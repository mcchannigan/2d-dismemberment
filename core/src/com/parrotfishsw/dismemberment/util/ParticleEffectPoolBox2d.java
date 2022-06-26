package com.parrotfishsw.dismemberment.util;


import com.badlogic.gdx.utils.Pool;
import com.parrotfishsw.dismemberment.util.ParticleEffectPoolBox2d.PooledEffectBox2d;

public class ParticleEffectPoolBox2d extends Pool<PooledEffectBox2d> {
	private final ParticleEffectBox2d effect;

	public ParticleEffectPoolBox2d (ParticleEffectBox2d effect, int initialCapacity, int max) {
		super(initialCapacity, max);
		this.effect = effect;
	}

	protected PooledEffectBox2d newObject () {
		return new PooledEffectBox2d(effect);
	}

	public PooledEffectBox2d obtain () {
		PooledEffectBox2d effect = super.obtain();
		effect.reset();
		return effect;
	}

	public class PooledEffectBox2d extends ParticleEffectBox2d {
		PooledEffectBox2d (ParticleEffectBox2d effect) {
			super(effect);
		}

		@Override
		public void reset () {
			super.reset();
		}

		public void free () {
			ParticleEffectPoolBox2d.this.free(this);
		}
	}
}
