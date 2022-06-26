package com.parrotfishsw.dismemberment.item;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;

public class Shield extends Equipment {
	public float physReduction;
	public float fireReduction;
	public float lightningReduction;
	public float stability;
	
	public Shield(String name) {
		super("data/armor/", name);
		JsonValue shieldData = root.get("shield");
		physReduction = shieldData.getFloat("physical");
		fireReduction = shieldData.getFloat("fire");
		lightningReduction = shieldData.getFloat("lightning");
		stability = shieldData.getFloat("stability");
		root = null; // dispose of JSON value, since it is no longer needed
	}
	
	public Shield(Shield w) {
		super(w);
		physReduction = w.physReduction;
		fireReduction = w.fireReduction;
		lightningReduction = w.lightningReduction;
		stability = w.stability;
	}

}
