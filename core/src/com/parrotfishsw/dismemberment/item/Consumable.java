package com.parrotfishsw.dismemberment.item;

public abstract class Consumable extends Equipment {
	
	public int id;
	
	public Consumable(int id) {
		this.id = id;
	}
	
	public boolean consume() {
		if(quantity > 0) {
			quantity--;
			return true;
		}
		return false;
	}
	
	public boolean isUsable() {
		return true;
	}
}
