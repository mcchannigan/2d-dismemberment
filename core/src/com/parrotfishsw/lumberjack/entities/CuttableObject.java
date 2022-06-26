package com.parrotfishsw.lumberjack.entities;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.joints.WeldJointDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.parrotfishsw.dismemberment.DsConstants;
import com.parrotfishsw.lumberjack.box2d.B2Utils;

/**
 * Models a game object that will link the rendering engine to Box2D physics
 * 
 * @author Kyle
 * 
 */
public class CuttableObject {
	public static final byte ANCHOR_UP = 1;
	public static final byte ANCHOR_DOWN = 2;
	public static final byte ANCHOR_LEFT = 4;
	public static final byte ANCHOR_RIGHT = 8;
	
	public static final byte WELD_PARENT = 1;
	public static final byte FIND_PARENT = 2;
	public static final byte FIND_CHILD = 4;

	public Body body;
	private Sprite sprite;
	public CuttableObject parent = null;
	public final ArrayList<CuttableObject> children = new ArrayList<CuttableObject>(0);

	public byte anchor = 0;
	public byte id = -1;
	public byte parentId = -1;
	public boolean sliceEntered = false;
	public boolean sliceExited = false;
	public Vector2 entryPt = new Vector2(0, 0);
	public Vector2 exitPt = new Vector2(0, 0);
	
	public int health = -1;
	public byte flag = 0;

	public CuttableObject() {

	}

	public CuttableObject(Body b) {
		body = b;
	}

	/**
	 * Set the sprite position based on the physics engine, such that this
	 * entity is ready to render
	 */
	public void updateDrawPosition() {
		Vector2 bodyPosition = body.getWorldCenter();
		float x = DsConstants.PIXELS_PER_METER * (bodyPosition.x)
				- sprite.getWidth() / 2;
		float y = DsConstants.PIXELS_PER_METER * (bodyPosition.y)
				- sprite.getHeight() / 2;
		sprite.setX(x);
		sprite.setY(y);
	}

	public void slice() {
		// orig shape attributes
		// ArrayList<Fixture> origFixtureList = body.getFixtureList();
		Fixture origFixture = body.getFixtureList().get(0);
		PolygonShape origPoly = (PolygonShape) origFixture.getShape();
		int vertCount = origPoly.getVertexCount();
		//boolean isStatic = body.getType().equals(BodyType.StaticBody);
		float x1 = 0;
		float x2 = 0;
		float y1 = 0;
		float y2 = 0;

		// for later calc
		float determinant;
		int i;

		// to store verts of new sprites
		ArrayList<Vector2> sprite1Verts = new ArrayList<Vector2>(25);
		ArrayList<Vector2> sprite2Verts = new ArrayList<Vector2>(25);
		ArrayList<Vector2> sprite1VertsSorted;
		ArrayList<Vector2> sprite2VertsSorted;

		// entry and exit points are added as vertices to new shapes
		sprite1Verts.add(entryPt);
		sprite1Verts.add(exitPt);
		sprite2Verts.add(entryPt);
		sprite2Verts.add(exitPt);

		World world = body.getWorld();

		// iterate through all the vertices and add them to each sprite's shape
		for (i = 0; i < vertCount; i++) {
			// get our vertex from the polygon
			Vector2 point = new Vector2();
			origPoly.getVertex(i, point);

			// you check if our point is not the same as our entry or exit point
			// first
			Vector2 diffFromentryPt = point.cpy().sub(entryPt);
			Vector2 diffFromexitPt = point.cpy().sub(exitPt);

			if ((diffFromentryPt.x == 0 && diffFromentryPt.y == 0)
					|| (diffFromexitPt.x == 0 && diffFromexitPt.y == 0)) {
			} else {
				determinant = B2Utils.calculateDeterminant2x3(entryPt.x,
						entryPt.y, exitPt.x, exitPt.y, point.x, point.y);

				if (determinant > 0) {
					// if the determinant is positive, then the three points are
					// in clockwise order
					sprite1Verts.add(point);
					x1 += point.x;
					y1 += point.y;
				} else {
					// if the determinant is 0, the points are on the same line.
					// if the determinant is negative, then they are in
					// counter-clockwise order
					sprite2Verts.add(point);
					x2 += point.x;
					y2 += point.y;
				}
			}
		}
		// calculate average point position for anchoring later. The entry and exit points are in common, so they are not factored in. As such, the average subtracts 2 from the count
		x1 /= (sprite1Verts.size() - 2);
		y1 /= (sprite1Verts.size() - 2);
		x2 /= (sprite2Verts.size() - 2);
		y2 /= (sprite2Verts.size() - 2);

		// Box2D needs vertices to be arranged in counter-clockwise order so you
		// reorder our points using a custom function
		sprite1VertsSorted = B2Utils.arrangeVertices(sprite1Verts);
		sprite2VertsSorted = B2Utils.arrangeVertices(sprite2Verts);

		// only cut the shape if both shapes pass certain requirements from our
		// function
		boolean sprite1VerticesAcceptable = B2Utils
				.areVerticesAcceptable(sprite1VertsSorted);
		boolean sprite2VerticesAcceptable = B2Utils
				.areVerticesAcceptable(sprite2VertsSorted);

		// destroy the old shape and create the new shapes
		if (sprite1VerticesAcceptable && sprite2VerticesAcceptable) {
			CuttableObject newObj1 = new CuttableObject();
			CuttableObject newObj2 = new CuttableObject();
			// Re-parent the pieces
			newObj1.parent = parent;
			newObj2.parent = parent;
			if(parent != null) {
				parent.children.remove(this);
			}
			
			BodyType bType = BodyType.DynamicBody;
			BodyType bType2 = BodyType.DynamicBody;

			if (anchor > 0) {
				if ((anchor & ANCHOR_DOWN) > 0) {
					if (y1 < y2) {
						bType = BodyType.StaticBody;
						newObj1.anchor = anchor;
					} else {
						bType2 = BodyType.StaticBody;
						newObj2.anchor = anchor;
					}
				} else if ((anchor & ANCHOR_UP) > 0) {
					if (y1 > y2) {
						bType = BodyType.StaticBody;
						newObj1.anchor = anchor;
					} else {
						bType2 = BodyType.StaticBody;
						newObj2.anchor = anchor;
					}
				} else if ((anchor & ANCHOR_LEFT) > 0) {
					if (x1 < x2) {
						bType = BodyType.StaticBody;
						newObj1.anchor = anchor;
					} else {
						bType2 = BodyType.StaticBody;
						newObj2.anchor = anchor;
					}
				} else if ((anchor & ANCHOR_RIGHT) > 0) {
					if (x1 > x2) {
						bType = BodyType.StaticBody;
						newObj1.anchor = anchor;
					} else {
						bType2 = BodyType.StaticBody;
						newObj2.anchor = anchor;
					}
				}
			}
			
			// Determine which children belong to which piece
			newObj1.flag |= FIND_CHILD;
			newObj2.flag |= FIND_CHILD;
			for(CuttableObject child : children) {
				child.body.setType(BodyType.DynamicBody);
				child.flag |= FIND_PARENT;
			}

			// create the first sprite's body
			Body body1 = B2Utils.createBodyWithPosition(world, bType,
					body.getPosition(), body.getAngle(), sprite1VertsSorted,
					origFixture.getDensity(), origFixture.getFriction(),
					origFixture.getRestitution(), origFixture.getFilterData());
			newObj1.body = body1;
			body1.setUserData(newObj1);
			// create the first sprite

			// create the second sprite's body
			Body body2 = B2Utils.createBodyWithPosition(world, bType2,
					body.getPosition(), body.getAngle(), sprite2VertsSorted,
					origFixture.getDensity(), origFixture.getFriction(),
					origFixture.getRestitution(), origFixture.getFilterData());
			newObj2.body = body2;
			body2.setUserData(newObj2);
			// create the second sprite

			world.destroyBody(body);
			// clean up sprite and this object
		} else {
			sliceEntered = false;
			sliceExited = false;
		}
	}

	public void setAnchor(String anchorString) {
		if (anchorString.contains("s")) {
			anchor |= ANCHOR_DOWN;
		} else if (anchorString.contains("n")) {
			anchor |= ANCHOR_UP;
		} else if (anchorString.contains("w")) {
			anchor |= ANCHOR_LEFT;
		} else if (anchorString.contains("e")) {
			anchor |= ANCHOR_RIGHT;
		}
	}
	
	public void weldToParent(World world) {
		WeldJointDef jointDef = new WeldJointDef();
		jointDef.initialize(body, parent.body, body.getWorldCenter());
		world.createJoint(jointDef);
	}
	
	public void damage(int amt) {
		if(health > 0) {
			health -= amt;
			if(health < 1) {
				// die
			}
		}
	}
}
