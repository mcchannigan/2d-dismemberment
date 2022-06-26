package com.parrotfishsw.lumberjack.entities;

import com.badlogic.gdx.math.Vector2;
import com.parrotfishsw.lumberjack.entities.Enemy.State;

public class DuelistAI implements Enemy.AI {
	private Enemy enemy;

	public DuelistAI(Enemy e) {
		enemy = e;
	}

	@Override
	public void doAI() {
		Vector2 myPos = enemy.body.getPosition();
		if (enemy.state == State.AGGRO) {
			if (enemy.target.dead) {
				enemy.state = State.IDLE;
				enemy.target = null;
				return;
			}
			if(enemy.animLock) {
				return;
			}
			Vector2 targetPos = enemy.target.body.getPosition();
			myPos.sub(targetPos);
			float dist = myPos.len();
			boolean facing = !enemy.skeleton.getFlipX();
			boolean dir = myPos.x < 0;
			if (dist > enemy.bodySize.x * 3 || (facing != dir)) {
				if (enemy.desiredMovement * myPos.x >= 0) {
					enemy.setMovement(myPos.x > 0 ? -1 : 1);
				}
			} else {
				enemy.setMovement(0);
				if (!enemy.target.isFacingPerson(enemy)) {
					enemy.heavyAttack();
				} else if (enemy.target.blocking > 0) {
					enemy.guardbreak();
				} else if (enemy.target.attacking > 0) {
					if (enemy.target.attacking == 1) {
						enemy.parry();
					} else {
						enemy.block();
					}
				} else if (enemy.target.parried) {
					enemy.heavyAttack();
				} else {
					enemy.lightAttack();
				}
			}
		} else if (enemy.state == State.FLEE) {
			Vector2 targetPos = enemy.target.body.getPosition();
			myPos.sub(targetPos);
			if (enemy.desiredMovement * myPos.x < 0) {
				enemy.setMovement(myPos.x > 0 ? 1 : -1);
			}
		} else {
			// IDLE or PATROL

		}
	}

}
