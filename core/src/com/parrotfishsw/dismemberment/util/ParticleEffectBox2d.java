package com.parrotfishsw.dismemberment.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.StreamUtils;

public class ParticleEffectBox2d extends ParticleEffect {
	World world;
	boolean physics;
	
	public ParticleEffectBox2d(World w) {
		this(w, true);
	}
	
	public ParticleEffectBox2d(World w, boolean physics) {
		super();
		world = w;
		this.physics = physics;
	}
	
	public ParticleEffectBox2d(ParticleEffectBox2d particle) {
		super();
		Array<ParticleEmitter> emitters = getEmitters();
		Array<ParticleEmitter> origEmitters = particle.getEmitters();
		for (int i = 0, n = origEmitters.size; i < n; i++) {
			if(particle.physics) {
				emitters.add(new ParticleEmitterBox2d((ParticleEmitterBox2d)origEmitters.get(i)));
			} else {
				emitters.add(new ParticleEmitter(origEmitters.get(i)));
			}
		}
		world = particle.world;
		physics = particle.physics;
	}
	
	public void loadEmitters (FileHandle effectFile) {
		if(!physics) {
			super.loadEmitters(effectFile);
			return;
		}
		Array<ParticleEmitter> emitters = getEmitters();
		InputStream input = effectFile.read();
		emitters.clear();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(input), 512);
			while (true) {
				ParticleEmitterBox2d emitter = new ParticleEmitterBox2d(world, reader);
				emitters.add(emitter);
				if (reader.readLine() == null) break;
			}
		} catch (IOException ex) {
			throw new GdxRuntimeException("Error loading effect: " + effectFile, ex);
		} finally {
			StreamUtils.closeQuietly(reader);
		}
	}
}
