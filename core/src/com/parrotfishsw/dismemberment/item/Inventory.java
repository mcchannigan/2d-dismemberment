package com.parrotfishsw.dismemberment.item;

import com.badlogic.gdx.utils.Array;
import com.parrotfishsw.lumberjack.entities.Person;

public class Inventory {
	public static final int WEAPON_SLOTS = 2;
	Array<Weapon> weapons;
	public final Weapon[] weaponEquip = new Weapon[WEAPON_SLOTS];
	Array<Armor> helms;
	Array<Armor> armors;
	Array<Armor> gauntlets;
	Array<Armor> leggings;
	public final Armor[] helm = new Armor[1];
	public final Armor[] armor = new Armor[1];
	public final Armor[] arms = new Armor[1];
	public final Armor[] legs = new Armor[1];

	Array<Shield> shields;
	public final Shield[] shieldEquip = new Shield[WEAPON_SLOTS];
	Array<Consumable> consumables;
	final Array<Consumable> equippedConsumables;
	Person person;
	float burden;

	public Inventory(Person person) {
		this.person = person;
		weapons = new Array<Weapon>();
		armors = new Array<Armor>();
		helms = new Array<Armor>();
		gauntlets = new Array<Armor>();
		leggings = new Array<Armor>();
		shields = new Array<Shield>();
		consumables = new Array<Consumable>();
		equippedConsumables = new Array<Consumable>();
	}

	public void setWeaponSlot(int slot, Weapon weapon) {
		weaponEquip[slot] = weapon;
	}

	public void setShieldSlot(int slot, Shield shield) {
		shieldEquip[slot] = shield;
	}
	
	public void setArmorSlot(Armor armor) {
		/*if(armor.type == Armor.Type.ARMS) {
			setArms(armor);
		} else if(armor.type == Armor.Type.LEGS) {
			setLegs(armor);
		} else if(armor.type == Armor.Type.TORSO) {
			setBody(armor);
		} else if(armor.type == Armor.Type.HEAD) {
			setHelm(armor);
		}
		*/
		person.equipArmor(armor);
	}

	public Weapon getEquippedWeapon(int slot) {
		return weaponEquip[slot];
	}

	public Shield getEquippedShield(int slot) {
		return shieldEquip[slot];
	}

	public void setHelm(Armor head) {
		helm[0] = head;
	}

	public void setBody(Armor body) {
		armor[0] = body;
	}

	public void setArms(Armor arms) {
		this.arms[0] = arms;
	}

	public void setLegs(Armor legs) {
		this.legs[0] = legs;
	}

	public Armor getHelm() {
		return helm[0];
	}

	public Armor getBody() {
		return armor[0];
	}

	public Armor getArms() {
		return arms[0];
	}

	public Armor getLegs() {
		return legs[0];
	}

	public int unequipArmor(Armor armor) {
		int ret = -1;
		if (helm[0] == armor) {
			helm[0] = null;
			ret = 0;
		} else if (this.armor[0] == armor) {
			this.armor[0] = null;
			ret = 1;
		} else if (arms[0] == armor) {
			arms[0] = null;
			ret = 2;
		} else if (legs[0] == armor) {
			legs[0] = null;
			ret = 3;
		}
		return ret;
	}

	public boolean addEquipment(Equipment equip) {
		if (burden + equip.weight > person.getMaxBurden()) {
			return false;
		}
		burden += equip.weight;
		if (equip instanceof Weapon) {
			weapons.add((Weapon) equip);
		} else if (equip instanceof Armor) {
			Armor armor = (Armor) equip;
			if (armor.getType() == Armor.Type.ARMS) {
				gauntlets.add(armor);
			} else if (armor.getType() == Armor.Type.HEAD) {
				helms.add(armor);
			} else if (armor.getType() == Armor.Type.LEGS) {
				leggings.add(armor);
			} else if (armor.getType() == Armor.Type.TORSO) {
				armors.add((Armor) equip);
			}
		} else if (equip instanceof Shield) {
			shields.add((Shield) equip);
		}
		return true;
	}

	public void removeEquipment(Equipment equip) {
		boolean ok = false;
		if (equip instanceof Weapon) {
			ok = weapons.removeValue((Weapon) equip, true);
		} else if (equip instanceof Armor) {
			Armor armor = (Armor) equip;
			if (armor.getType() == Armor.Type.ARMS) {
				ok = gauntlets.removeValue(armor, true);
			} else if (armor.getType() == Armor.Type.HEAD) {
				ok = helms.removeValue(armor, true);
			} else if (armor.getType() == Armor.Type.LEGS) {
				ok = leggings.removeValue(armor, true);
			} else if (armor.getType() == Armor.Type.TORSO) {
				ok = armors.removeValue(armor, true);
			}
		} else if (equip instanceof Shield) {
			ok = shields.removeValue((Shield) equip, true);
		}
		if (ok) {
			burden -= equip.weight;
		}
	}

	public void addConsumable(Consumable obj) {
		boolean added = false;
		for (Consumable item : consumables) {
			if (obj.id == item.id) {
				item.quantity += obj.quantity;
				added = true;
			}
		}
		if (!added) {
			consumables.add(obj);
		}
	}

	public float getWeight() {
		return burden;
	}

	public Array<Weapon> getWeapons() {
		return weapons;
	}

	public Array<Armor> getArmors() {
		return armors;
	}

	public Array<Armor> getHelms() {
		return helms;
	}

	public Array<Armor> getGauntlets() {
		return gauntlets;
	}

	public Array<Armor> getLeggings() {
		return leggings;
	}

	public Array<Shield> getShields() {
		return shields;
	}

	public Array<Consumable> getConsumables() {
		return consumables;
	}
	
	public int getEquipSize(Equipment equipment) {
		if(equipment instanceof Weapon || equipment instanceof Shield) {
			return 2;
		} else if(equipment instanceof Armor) {
			return 1;
		} else {
			// consumable
			return 4;
		}
	}
}
