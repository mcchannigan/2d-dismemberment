package com.parrotfishsw.dismemberment.item;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;

public class Weapon extends Equipment {
	public int damage;
	public int damageFire;
	public int damageLightning;
	float speed;
	float minLength;
	float maxLength;
	public int scaleStr;
	public int scaleDex;
	public int scaleInt;
	Array<Strike> lightMoves;
	Array<Strike> heavyMoves;
	int lightIndex = 0;
	int heavyIndex = 0;
	public boolean twoHand = false;
	
	Strike currentStrike = null;

	public Weapon(String name) {
		super("data/weapons/", name);
		JsonValue weaponData = root.get("weapon");
		damage = weaponData.getInt("damage");
		damageFire = weaponData.getInt("fireDamage");
		damageLightning = weaponData.getInt("lightningDamage");
		scaleStr = weaponData.getInt("scaleStr");
		scaleDex = weaponData.getInt("scaleDex");
		scaleInt = weaponData.getInt("scaleInt");
		twoHand = weaponData.getBoolean("twoHand");

		// Load moveset
		lightMoves = new Array<Strike>(5);
		heavyMoves = new Array<Strike>(3);
		JsonValue moveSet = weaponData.get("moveSet");
		for (JsonValue move : moveSet) {
			Strike strike = new Strike();
			strike.animation = move.getString("animation");
			strike.order = move.getInt("order");
			strike.staminaCost = move.getInt("cost");
			strike.type = move.getInt("type");
			strike.connected = false;
			strike.withShield = move.getBoolean("withShield");
			if (move.name.startsWith("light")) {
				lightMoves.insert(strike.order, strike);
			} else {
				heavyMoves.insert(strike.order, strike);
			}
		}

		root = null; // dispose of JSON value, since it is no longer needed
	}
	
	public Weapon(Weapon w) {
		super(w);
		damage = w.damage;
		damageFire = w.damageFire;
		damageLightning = w.damageLightning;
		speed = w.speed;
		minLength = w.minLength;
		maxLength = w.maxLength;
		scaleStr = w.scaleStr;
		scaleDex = w.scaleDex;
		scaleInt = w.scaleInt;
		lightMoves = new Array<Weapon.Strike>(w.lightMoves);
		heavyMoves = new Array<Weapon.Strike>(w.heavyMoves);
		lightIndex = heavyIndex = 0;
		currentStrike = null;
	}

	public void resetStrikes() {
		lightIndex = 0;
		heavyIndex = 0;
		currentStrike = null;
	}

	/**
	 * Retrieves the next strike in the moveset and updates the strike state
	 * @param str
	 * @return The next strike to begin
	 */
	public Strike getStrike(Strike.Strength str) {
		//if(!comboHasNext(str)) return null;
		currentStrike = null;
		if (str == Strike.Strength.LIGHT) {
			if(heavyIndex > 0) {
				lightIndex = heavyIndex % 2;
			}
			if(lightIndex < lightMoves.size) {
				currentStrike = lightMoves.get(lightIndex);
				currentStrike.connected = false;
			}
			lightIndex = (lightIndex + 1) % lightMoves.size;
		} else if (str == Strike.Strength.HEAVY) {
			if(lightIndex > 0) {
				heavyIndex = lightIndex % 2;
			}
			if(heavyIndex < heavyMoves.size) {
				currentStrike = heavyMoves.get(heavyIndex);
				currentStrike.connected = false;
			}
			heavyIndex = (heavyIndex + 1) % heavyMoves.size;
		}
		return currentStrike;
	}
	
	/**
	 * Returns the current strike without changing the strike state. Usually called only during the current strike's animation
	 * @return
	 */
	public Strike getCurrentStrike() {
		return currentStrike;
	}
	
	public boolean comboHasNext(Strike.Strength str) {
		if (str == Strike.Strength.LIGHT) {
			return lightIndex < lightMoves.size;
		} else if (str == Strike.Strength.HEAVY) {
			return heavyIndex < heavyMoves.size;
		} else {
			return false;
		}
	}

	public static enum Hand {
		LEFT, RIGHT
	}

	public static final class Strike {
		private String animation;
		public int staminaCost;
		public int type;
		public boolean connected;
		public boolean withShield;
		int order;
		
		public String getAnimation(boolean flip) {
			return animation + (flip ? "_l" : "_r");
		}

		public static enum Strength {
			LIGHT, HEAVY
		}
	}
	
	public static final int SLASH = 0;
	public static final int STAB = 1;
	public static final int BLUNT = 2;
	public static final int CRUSH = 3;
	public static final Integer ID = 3;
}
