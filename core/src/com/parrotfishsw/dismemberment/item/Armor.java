package com.parrotfishsw.dismemberment.item;

import com.badlogic.gdx.utils.JsonValue;

public class Armor extends Equipment {
	public Class armorClass;
	public Type type;
	public int defense;
	public int defFire;
	public int defLightning;

	public Armor(String name) {
		super("data/armor/", name);
		JsonValue armorValues = root.get("armor");
		defense = armorValues.getInt("defense");
		defFire = armorValues.getInt("fireDefense");
		defLightning = armorValues.getInt("lightningDefense");

		int clas = armorValues.getInt("class");
		switch (clas) {
		case (2):
			armorClass = Class.MEDIUM;
			break;
		case (3):
			armorClass = Class.HEAVY;
			break;
		case (1):
		default:
			armorClass = Class.LIGHT;
		}

		int t = armorValues.getInt("type");
		switch (t) {
		case (1):
			type = Type.HEAD;
			break;
		case (2):
			type = Type.TORSO;
			break;
		case (3):
			type = Type.ARMS;
			break;
		default:
		case(4):
			type = Type.LEGS;
		}
	}
	
	public Armor(Armor a) {
		super(a);
		armorClass = a.armorClass;
		type = a.type;
		defense = a.defense;
		defFire = a.defFire;
		defLightning = a.defLightning;
	}
	
	public Type getType() {
		return type;
	}
	
	public Class getArmorClass() {
		return armorClass;
	}

	public static enum Class {
		LIGHT, MEDIUM, HEAVY
	}

	public static enum Type {
		HEAD, TORSO, ARMS, LEGS
	}
}
