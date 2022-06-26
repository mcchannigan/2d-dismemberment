package com.parrotfishsw.dismemberment.item;

import com.esotericsoftware.spine.attachments.PhysicsBoundingBoxAttachment;

/**
 * Class meant to hold user data for Box2D bodies that belong to instances of {@link PhysicsBoundingBoxAttachment}
 * @author Kyle Hannigan
 *
 */
public class ArmorData {
	Type type;
	
	public ArmorData(PhysicsBoundingBoxAttachment att) {
		if(att.getName().contains("armor")) {
			type = Type.ARMOR_MID;
		} else {
			type = Type.BASE;
		}
	}
	
	public Type getType() {
		return type;
	}
	
	public static enum Type {
		BASE, ARMOR_MID
	}
}
