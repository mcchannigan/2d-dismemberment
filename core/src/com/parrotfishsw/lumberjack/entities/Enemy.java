package com.parrotfishsw.lumberjack.entities;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.parrotfishsw.dismemberment.DsConstants;
import com.parrotfishsw.dismemberment.GameConfig;

public class Enemy extends Person {
	private static final float DELAY = 0.8f;
	State state = State.IDLE;
	float aggroDistance = 20;
	float aggroDelay = DELAY;
	int aggressiveness;
	Person target = null;
	AI ai;

	public Enemy(String name, World world) {
		super(name, world);
		ai = new BerserkerAI(this);
	}

	public Enemy(Person p) {
		super(p);
		ai = new BerserkerAI(this);
	}

	public Enemy(Person p, AI ai) {
		super(p);
		this.ai = ai;
	}

	public void doFrame(float delta) {
		super.doFrame(delta);
		if (!dead) {
			if (target != null && aggroDelay > 0) {
				aggroDelay -= delta;
				if (aggroDelay <= 0) {
					state = State.AGGRO;
				}
			}
			if (ai != null) {
				ai.doAI();
			}
		}
	}

	public void goIdle() {
		target = null;
		state = State.IDLE;
		aggroDelay = DELAY;
	}

	public void checkAggro(Person person) {
		if (dead)
			return;
		Vector2 myPos = body.getPosition();
		Vector2 targetPos = person.body.getPosition();
		myPos.sub(targetPos);
		boolean facing = !skeleton.getFlipX();
		boolean dir = myPos.x < 0;
		float dist = myPos.len();
		if (facing == dir) {
			if (dist < (aggroDistance)) {
				aggro(person, true);
			}
		} else if (dist < (person.noise * 10)) {
			aggro(person, false);
		}
	}

	public void setAI(AI ai) {
		this.ai = ai;
	}

	public void aggro(Person target, boolean instant) {
		this.target = target;
		if (instant) {
			aggroDelay = 0;
		}
		if (aggroDelay <= 0) {
			state = State.AGGRO;
		}
	}

	@Override
	public void checkDamage(Person attacker) {
		super.checkDamage(attacker);
		aggro(attacker, true);
	}

	public void kill() {
		super.kill();
		// drop item
	}

	public static enum State {
		IDLE, PATROL, AGGRO, FLEE
	}

	public static interface AI {
		public void doAI();
	}
}
