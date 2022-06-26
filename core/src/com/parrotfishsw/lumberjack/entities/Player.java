package com.parrotfishsw.lumberjack.entities;

import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Pool.Poolable;
import com.parrotfishsw.lumberjack.box2d.LjRayCastCallback;

/**
 * Models the player entity, including physics, game logic, and sprite display
 * 
 * @author Kyle
 * 
 */
public class Player extends Person implements Poolable {
	public static final boolean RIGHT = true;
	public static final boolean LEFT = false;
	LjRayCastCallback ray = new LjRayCastCallback();
	
	public Player(World world) {		
		super("skeleton", world);
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}
	
}
