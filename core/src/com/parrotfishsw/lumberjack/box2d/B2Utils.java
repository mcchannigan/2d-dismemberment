package com.parrotfishsw.lumberjack.box2d;

import java.util.ArrayList;
import java.util.Collections;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

public class B2Utils {

	public static Body createBodyWithPosition(World world, BodyType type, Vector2 position,
			float rotation, ArrayList<Vector2> vertices, float density,
			float friction, float restitution, Filter filter) {
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = type;
		bodyDef.position.set(position);
		bodyDef.angle = rotation;
		Body body = world.createBody(bodyDef);

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.density = density;
		fixtureDef.friction = friction;
		fixtureDef.restitution = restitution;
		if(filter != null) {
			fixtureDef.filter.categoryBits = filter.categoryBits;
			fixtureDef.filter.maskBits = filter.maskBits;
			fixtureDef.filter.groupIndex = filter.groupIndex;
		}

		PolygonShape shape = new PolygonShape();
		Vector2[] vertArr = vertices.toArray(new Vector2[vertices.size()]);
		shape.set(vertArr);
		fixtureDef.shape = shape;
		body.createFixture(fixtureDef);

		shape.dispose();

		return body;
	}

	public static ArrayList<Vector2> arrangeVertices(ArrayList<Vector2> vertices) {
		float determinant;
		int iCounterClockWise = 1;
		int count = vertices.size();
		int iClockWise = count - 1;
		int i;

		Vector2 referencePointA, referencePointB;
		ArrayList<Vector2> sortedVertices = new ArrayList<Vector2>(count);
		for(i = 0; i < count; i++) {
			// initialize so that specific indices can be set
			sortedVertices.add(null);
		}

		// sort all vertices in ascending order according to their x-coordinate
		// so you can get two points of a line
		Collections.sort(vertices, new Vector2XComparator());
		// qsort(vertices, count, sizeof(b2Vec2), comparator);

		sortedVertices.set(0, vertices.get(0));
		referencePointA = vertices.get(0); // leftmost point
		referencePointB = vertices.get(count - 1); // rightmost point

		// you arrange the points by filling our vertices in both clockwise and
		// counter-clockwise directions using the determinant function
		for (i = 1; i < count - 1; i++) {
			determinant = calculateDeterminant2x3(referencePointA.x,
					referencePointA.y, referencePointB.x, referencePointB.y,
					vertices.get(i).x, vertices.get(i).y);
			if (determinant < 0) {
				sortedVertices.set(iCounterClockWise++, vertices.get(i));
			} else {
				sortedVertices.set(iClockWise--, vertices.get(i));
			}// endif
		}// endif

		sortedVertices.set(iCounterClockWise, vertices.get(count - 1));
		return sortedVertices;
	}
	
	public static boolean areVerticesAcceptable(ArrayList<Vector2> vertices) {
		int count = vertices.size();
	    //check 1: polygons need to at least have 3 vertices
	    if (count < 3)
	    {
	        return false;
	    }
	 
	    //check 2: the number of vertices cannot exceed b2_maxPolygonVertices
	    if (count > 8)
	    {
	        return false;
	    }
	 
	    //check 3: Box2D needs the distance from each vertex to be greater than b2_epsilon
	    for (int i=0; i<count; ++i)
	    {
	        int i1 = i;
	        int i2 = i + 1 < count ? i + 1 : 0;
	        Vector2 edge = new Vector2(vertices.get(i2)).sub(vertices.get(i1));
	        if (edge.len2() <= 0.005f * 0.5f)
	        {
	            return false;
	        }
	    }
	 
	    //check 4: Box2D needs the area of a polygon to be greater than b2_epsilon
	    float area = 0.0f;
	 
	    Vector2 pRef = new Vector2(0.0f,0.0f);
	 
	    for (int i=0; i<count; ++i)
	    {
	        Vector2 p1 = new Vector2(pRef);
	        Vector2 p2 = new Vector2(vertices.get(i));
	        Vector2 p3 = new Vector2(i + 1 < count ? vertices.get(i+1) : vertices.get(0));
	 
	        Vector2 e1 = p2.sub(p1);
	        Vector2 e2 = p3.sub(p1);
	 
	        float D = e1.crs(e2);
	 
	        float triangleArea = 0.5f * D;
	        area += triangleArea;
	    }
	 
	    if (area <= 0.0001)
	    {
	        return false;
	    }
	 
	    //check 5: Box2D requires that the shape be Convex.
	    float determinant;
	    float referenceDeterminant;
	    Vector2 v1 = new Vector2(vertices.get(0)).sub(vertices.get(count-1));
	    Vector2 v2 = new Vector2(vertices.get(1)).sub(vertices.get(0));
	    referenceDeterminant = calculateDeterminant2x2(v1.x, v1.y, v2.x, v2.y);
	 
	    for (int i=1; i<count-1; i++)
	    {
	        v1 = v2;
	        v2 = new Vector2(vertices.get(i+1)).sub(vertices.get(i));
	        determinant = calculateDeterminant2x2(v1.x, v1.y, v2.x, v2.y);
	        //you use the determinant to check direction from one point to another. A convex shape's points should only go around in one direction. The sign of the determinant determines that direction. If the sign of the determinant changes mid-way, then you have a concave shape.
	        if (referenceDeterminant * determinant < 0.0f)
	        {
	            //if multiplying two determinants result to a negative value, you know that the sign of both numbers differ, hence it is concave
	            return false;
	        }
	    }
	    v1 = v2;
	    v2 = new Vector2(vertices.get(0)).sub(vertices.get(count-1));
	    determinant = calculateDeterminant2x2(v1.x, v1.y, v2.x, v2.y);
	    if (referenceDeterminant * determinant < 0.0f)
	    {
	        return false;
	    }
	    return true;
	}
	
	public static float calculateDeterminant2x2(float x1, float y1, float x2, float y2) {
		return x1*y2-y1*x2;
	}
	
	public static float calculateDeterminant2x3(float x1, float y1, float x2, float y2, float x3, float y3) {
		return x1*y2+x2*y3+x3*y1-y1*x2-y2*x3-y3*x1;
	}
}
