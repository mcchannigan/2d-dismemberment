package com.parrotfishsw.dismemberment.util;

import java.util.ArrayList;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Transform;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.parrotfishsw.lumberjack.box2d.B2Utils;

public class Box2DTools {
	public static Body[] slice(Body body, Vector2 entryPt, Vector2 exitPt, float[] avgPositions) {
		Body[] result = null;
		// orig shape attributes
		Array<Fixture> origFixtureList = body.getFixtureList();
		if(origFixtureList == null) return null;
		Fixture origFixture = origFixtureList.first();
		if(origFixture == null) return null;
		PolygonShape origPoly = (PolygonShape) origFixture.getShape();
		int vertCount = origPoly.getVertexCount();

		// for later calc
		float determinant;
		int i;
		Vector2 polyPos1 = new Vector2(entryPt);
		polyPos1.add(exitPt);
		Vector2 polyPos2 = new Vector2(polyPos1);

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
					polyPos1.add(point);
				} else {
					// if the determinant is 0, the points are on the same line.
					// if the determinant is negative, then they are in
					// counter-clockwise order
					sprite2Verts.add(point);
					polyPos2.add(point);
				}
			}
		}
		// calculate average point position for anchoring later. The entry and
		// exit points are in common, so they are not factored in. As such, the
		// average subtracts 2 from the count
		int size1 = sprite1Verts.size();
		int size2 = sprite2Verts.size();
		polyPos1.x /= (size1);
		polyPos1.y /= (size1);
		polyPos2.x /= (size2);
		polyPos2.y /= (size2);
		if(avgPositions != null) {
			Transform trans = body.getTransform();
			trans.mul(polyPos1);
			trans.mul(polyPos2);
			avgPositions[0] = polyPos1.x;
			avgPositions[1] = polyPos1.y;
			avgPositions[2] = polyPos2.x;
			avgPositions[3] = polyPos2.y;
		}

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
			result = new Body[2];

			BodyType bType = BodyType.DynamicBody;
			BodyType bType2 = BodyType.DynamicBody;

			// create the first sprite's body
			Body body1 = B2Utils.createBodyWithPosition(world, bType,
					body.getPosition(), body.getAngle(), sprite1VertsSorted,
					origFixture.getDensity(), origFixture.getFriction(),
					origFixture.getRestitution(), origFixture.getFilterData());
			result[0] = body1;
			// create the first sprite

			// create the second sprite's body
			Body body2 = B2Utils.createBodyWithPosition(world, bType2,
					body.getPosition(), body.getAngle(), sprite2VertsSorted,
					origFixture.getDensity(), origFixture.getFriction(),
					origFixture.getRestitution(), origFixture.getFilterData());
			result[1] = body2;
			// create the second sprite

			world.destroyBody(body);
			// clean up sprite and this object
		}

		return result;
	}
}
