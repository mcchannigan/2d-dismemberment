package com.parrotfishsw.lumberjack.box2d;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.spine.attachments.PhysicsBoundingBoxAttachment;
import com.parrotfishsw.dismemberment.item.Weapon;
import com.parrotfishsw.lumberjack.entities.CuttableObject;
import com.parrotfishsw.lumberjack.entities.Person;

public class LjRayCastCallback implements RayCastCallback {
	public Person attacker;
	public int type;
	public Array<Person> victims = new Array<Person>(3);

	public void setup(Person attacker, int type) {
		this.attacker = attacker;
		victims.clear();
		this.type = type;
	}

	@Override
	public float reportRayFixture(Fixture fixture, Vector2 point,
			Vector2 normal, float fraction) {
		int ret = 0;
		//if(type == Weapon.SLASH) {
			ret = 1;
		//}
		Body body = fixture.getBody();
		if (body.getUserData() != null) {
			Object dat = body.getUserData();
			if (dat instanceof CuttableObject) {
				CuttableObject gObj = (CuttableObject) dat;
				if (!gObj.sliceEntered) {
					gObj.sliceEntered = true;

					// you need to get the point coordinates within the shape
					gObj.entryPt.set(body.getLocalPoint(point));
				} else if (!gObj.sliceExited) {
					gObj.exitPt.set(body.getLocalPoint(point));
					gObj.sliceExited = true;
				}
			} else if (dat instanceof PhysicsBoundingBoxAttachment) {
				PhysicsBoundingBoxAttachment bbAtt = (PhysicsBoundingBoxAttachment) dat;
				if (bbAtt.person == attacker)
					return -1f;
				if (!victims.contains(bbAtt.person, true)) {
					//bbAtt.person.doBloodEffect(point, attacker.getX() < bbAtt.person.getX());
					victims.add(bbAtt.person);
				}
				bbAtt.person.notifyHit(bbAtt);
				if (!bbAtt.sliceEntered) {
					bbAtt.sliceEntered = true;
					bbAtt.entryPt.set(body.getLocalPoint(point));
				} else if (!bbAtt.sliceExited) {
					bbAtt.sliceExited = true;
					bbAtt.exitPt.set(body.getLocalPoint(point));
				}
			}
		}
		return ret;
	}

}
