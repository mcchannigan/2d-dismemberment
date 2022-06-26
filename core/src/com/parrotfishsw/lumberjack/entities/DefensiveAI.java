package com.parrotfishsw.lumberjack.entities;

import com.badlogic.gdx.math.Vector2;
import com.parrotfishsw.lumberjack.entities.Enemy.State;

public class DefensiveAI implements Enemy.AI {
	private Enemy enemy;
	
	public DefensiveAI(Enemy e) {
		enemy = e;
	}

	@Override
	public void doAI() {
		Vector2 myPos = enemy.body.getPosition();
		if (enemy.state == State.AGGRO) {
			Vector2 targetPos = enemy.target.body.getPosition();
			myPos.sub(targetPos);
			if(enemy.target.dead) {
				enemy.state = State.IDLE;
				enemy.target = null;
				return;
			}
			
			float dist = myPos.len();
			boolean facing = !enemy.skeleton.getFlipX();
			boolean dir = myPos.x < 0;
			
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
