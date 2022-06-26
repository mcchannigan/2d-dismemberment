package com.parrotfishsw.dismemberment.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonValue.JsonIterator;
import com.esotericsoftware.spine.Bone;
import com.esotericsoftware.spine.Skeleton;

public class RagdollLoader {
	public static void loadRagdollForSkeleton(FileHandle fh, Skeleton skel)
	throws NoSuchBoneException
	{
		JsonReader reader = new JsonReader();
		JsonValue root = reader.parse(fh);
		JsonIterator iter = root.iterator();
		while(iter.hasNext()) {
			JsonValue child = iter.next();
			Bone b = skel.findBone(child.name);
			if(b == null) throw new NoSuchBoneException(child.name);
			float min = MathUtils.degRad * child.getFloat("min");
			float max = MathUtils.degRad * child.getFloat("max");
			b.getData().setRagdollLimits(min, max);
		}
	}
}
