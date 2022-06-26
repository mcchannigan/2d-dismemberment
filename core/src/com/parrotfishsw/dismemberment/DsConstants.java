package com.parrotfishsw.dismemberment;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.Pool;

public class DsConstants {
	public static final float PIXELS_PER_METER = 50f;
	public static final float METERS_PER_PIXEL = 0.02f;
	public static final float MAP_SCALE = 1/2f;
	public static final float RAD_TO_DEG = (float)(180f / Math.PI);
	public static final float DEG_TO_RAD = (float)(Math.PI / 180f);
	public static final float CAM_SPD = 1f;
	public static final float DAMAGE_TICKS = 2;
	public static final float DESPAWN_TIME = 15;
	
	public static class Categories {
		public static final short MAP = 0x0001;
		public static final short PLAYER = 0x0002;
		public static final short DESTRUCTIBLE = 0x0004;
		public static final short ENEMY = 0x0008;
		public static final short GOAL = 0x0010;
		
		public static final short CHUNK = 0x0020;
		public static final short LIMB = 0x0040;
		public static final short BODY = 0x0080;
		public static final short WEAPON = 0x0100;
	}
	
	public static class Actions {
		public static final byte ATTACK_LIGHT = 0;
		public static final byte ATTACK_HEAVY = 1;
		public static final byte BACKSTEP = 2;
		public static final byte GUARDBREAK = 3;
	}
	
	public static class Groups {
		public static final int CHUNKS = -1;
	}
	
	public static class Id {
		public static final Integer GOAL_ID = 10;
	}
	
	public static class Colors {
		public static final Color GRAY = Color.valueOf("333333FF");
		public static final Color FOCUS = Color.valueOf("FFDD22FF");
	}
	
	public static final Pool<Vector2> vectorPool = new Pool<Vector2>() {

		@Override
		protected Vector2 newObject() {
			return new Vector2();
		}
		
	};
	
	public static I18NBundle i18nBundle = null;
}
