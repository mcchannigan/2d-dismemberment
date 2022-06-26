package com.parrotfishsw.lumberjack.box2d;

import java.util.Comparator;

import com.badlogic.gdx.math.Vector2;
/**
 * Compares the x componentes of two instances of Vector2
 * @author Kyle
 *
 */
public class Vector2XComparator implements Comparator<Vector2> {

	@Override
	public int compare(Vector2 o1, Vector2 o2) {
		if (o1.x > o2.x) {
	        return 1;
	    } else if (o1.x < o2.x) {
	        return -1;
	    }
	    return 0;  
	}

}
