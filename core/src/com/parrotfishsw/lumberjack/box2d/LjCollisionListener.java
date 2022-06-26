package com.parrotfishsw.lumberjack.box2d;

import java.util.logging.Logger;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.esotericsoftware.spine.attachments.PhysicsBoundingBoxAttachment;
import com.parrotfishsw.dismemberment.DsConstants;
import com.parrotfishsw.dismemberment.item.Weapon;
import com.parrotfishsw.lumberjack.entities.Person;
import com.parrotfishsw.lumberjack.entities.Player;

public class LjCollisionListener implements ContactListener {
	Player player;
	World world;
	//Vector2 contactPoint = new Vector2();

	public LjCollisionListener(Player p, World w) {
		player = p;
		world = w;
	}

	@Override
	public void beginContact(Contact contact) {
		Fixture fA = contact.getFixtureA();
		Fixture fB = contact.getFixtureB();
		Fixture other = null;
		Integer idA = (Integer) fA.getUserData();
		Integer idB = (Integer) fB.getUserData();
		Body bA = fA.getBody();
		Body bB = fB.getBody();
		Person foot = null;
		Person side = null;
		PhysicsBoundingBoxAttachment weaponBox = null;
		PhysicsBoundingBoxAttachment victimBox = null;
		if (Person.FOOT_ID.equals(idA)) {
			foot = (Person) bA.getUserData();
		} else if (Person.FOOT_ID.equals(idB)) {
			foot = (Person) bB.getUserData();
		} else if (Weapon.ID.equals(idA)) {
			weaponBox = (PhysicsBoundingBoxAttachment) bA
					.getUserData();
			other = fB;
		} else if (Weapon.ID.equals(idB)) {
			weaponBox = (PhysicsBoundingBoxAttachment) bB
					.getUserData();
			other = fA;
		} else if(Person.SIDE_ID.equals(idA)) {
			side = (Person) bA.getUserData();
			side.handleSideCollision(fA);
		} else if(Person.SIDE_ID.equals(idB)) {
			side = (Person) bB.getUserData();
			side.handleSideCollision(fB);
		}
		if (foot != null) {
			if (DsConstants.Id.GOAL_ID.equals(idA)
					|| DsConstants.Id.GOAL_ID.equals(idB)) {
				// TODO victory condition
				Logger.getAnonymousLogger().info("Victory!");
			} else {
				foot.notifyFootTouch(1);
			}
		} else if (weaponBox != null && weaponBox.person.swingingWeapon > 0) {
			Object data = other.getBody().getUserData();
			if (data instanceof PhysicsBoundingBoxAttachment) {
				victimBox = (PhysicsBoundingBoxAttachment) data;
				if (victimBox.person != weaponBox.person) {
					weaponBox.person.addVictim(victimBox.person);
					victimBox.person.notifyHit(victimBox);
				}
			}
		} else if (bA.getUserData() instanceof Person && bB.getUserData() instanceof Person) {
			Person pA = (Person) bA.getUserData();
			Person pB = (Person) bB.getUserData();
			pA.touching.add(pB);
			pB.touching.add(pA);
			if(pA.kicking > 0 && pA.isFacingPerson(pB)) {
				pB.stagger(!pB.isFacingPerson(pA));
			} else if(pB.kicking > 0) {
				pA.stagger(!pA.isFacingPerson(pB));
			}
		}
	}

	@Override
	public void endContact(Contact contact) {
		if (contact.getFixtureB() != null && contact.getFixtureA() != null) {
			Fixture fA = contact.getFixtureA();
			Fixture fB = contact.getFixtureB();
			Integer idA = (Integer) fA.getUserData();
			Integer idB = (Integer) fB.getUserData();
			Body bA = fA.getBody();
			Body bB = fB.getBody();
			Person foot = null;
			if (Person.FOOT_ID.equals(idA)) {
				foot = (Person) fA.getBody().getUserData();
			} else if (Person.FOOT_ID.equals(idB)) {
				foot = (Person) fB.getBody().getUserData();
			} else if(Person.SIDE_ID.equals(idA)) {
				((Person) fA.getBody().getUserData()).handleSideEndCollision(fA);
			} else if(Person.SIDE_ID.equals(idB)) {
				((Person) fB.getBody().getUserData()).handleSideEndCollision(fB);
			} else if (bA.getUserData() instanceof Person && bB.getUserData() instanceof Person) {
				Person pA = (Person) bA.getUserData();
				Person pB = (Person) bB.getUserData();
				pA.touching.removeValue(pB, true);
				pB.touching.removeValue(pA, true);
			}
			if (foot != null) {
				foot.notifyFootTouch(-1);
			}
		}
	}

	@Override
	public void preSolve(Contact contact, Manifold oldManifold) {
		/*WorldManifold man = contact.getWorldManifold();
		if(man.getNumberOfContactPoints() > 0) {
			contactPoint.set(man.getPoints()[0]);
			contactPoint.add(man.getNormal());
		} else {
			contactPoint.set(0, 0);
		}*/

	}

	@Override
	public void postSolve(Contact contact, ContactImpulse impulse) {
/*		int impCt = impulse.getCount();
		float[] impulses = impulse.getNormalImpulses();
		float avgImpulse = 0;
		for (int i = 0; i < impCt; i++) {
			avgImpulse += impulses[i];
		}
		avgImpulse /= impCt;

		Fixture fA = contact.getFixtureA();
		Fixture fB = contact.getFixtureB();
		Object o = fA.getBody().getUserData();
		Object p = fB.getBody().getUserData();
		Person pers;
		if (avgImpulse > 20) {
			avgImpulse *= 0.5f;
			if (o != null && o instanceof Person) {
				pers = (Person) o;
				// go.damage((int) avgImpulse);
			}
			if (p != null && p instanceof Person) {
				pers = (Person) p;
				// go.damage((int) avgImpulse);
			}
		}*/
	}
}
