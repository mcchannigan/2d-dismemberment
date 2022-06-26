package com.parrotfishsw.dismemberment.item;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.SkeletonJson;
import com.esotericsoftware.spine.Skin;
import com.esotericsoftware.spine.attachments.AtlasNormalAttachmentLoader;
import com.esotericsoftware.spine.attachments.BoundingBoxAttachment;
import com.esotericsoftware.spine.attachments.PhysicsBoundingBoxAttachment;
import com.parrotfishsw.dismemberment.DsConstants;

public class Equipment {
	static TextureAtlas atlas = null;
	static TextureAtlas normals = null;
	JsonValue root;
	Skeleton skeleton;
	String name;
	String descriptionKey;
	String uiImage;
	float weight;
	int value;
	public int quantity;
	
	int minStr;
	int minDex;
	int minInt;
	
	public Equipment() {
		
	}
	
	public Equipment(Equipment eq) {
		skeleton = new Skeleton(eq.skeleton);
		name = eq.name;
		weight = eq.weight;
		value = eq.value;
		minStr = eq.minStr;
		minDex = eq.minDex;
		minInt = eq.minInt;
		uiImage = eq.uiImage;
		descriptionKey = eq.descriptionKey;
	}
	
	public Equipment(String path, String filename) {
		JsonReader reader = new JsonReader();
		root = reader.parse(Gdx.files.internal(path + filename + ".json"));
		name = root.getString("name");
		weight = root.getFloat("weight");
		value = root.getInt("value");
		minStr = root.getInt("minStr");
		minDex = root.getInt("minDex");
		minInt = root.getInt("minInt");
		uiImage = root.getString("uiImage");
		if(atlas == null) {
			atlas = new TextureAtlas(Gdx.files.internal("diffuse.atlas"));
			normals = new TextureAtlas(Gdx.files.internal("normals.atlas"));
		}
		//Texture atlasTexture = atlas.getRegions().first().getTexture();
		//Texture normalMapTexture = new Texture(Gdx.files.internal(path + filename + "-normal.png"));
		AtlasNormalAttachmentLoader attachLoader = new AtlasNormalAttachmentLoader(atlas, normals) {
			@Override
			public BoundingBoxAttachment newBoundingBoxAttachment(Skin skin,
					String name) {
				PhysicsBoundingBoxAttachment attachment = new PhysicsBoundingBoxAttachment(
						name);
				return attachment;
			}
		};
		SkeletonJson skel = new SkeletonJson(attachLoader);
		skel.setScale(DsConstants.METERS_PER_PIXEL);
		SkeletonData sData = skel.readSkeletonData(Gdx.files.internal(path + filename
				+ "-skeleton.json"));

		skeleton = new Skeleton(sData);
	}
	
	public Skeleton getSkeleton() {
		return skeleton;
	}
	
	public String getName() {
		return name;
	}

	public String getDescriptionKey() {
		return descriptionKey;
	}

	public void setDescriptionKey(String descriptionKey) {
		this.descriptionKey = descriptionKey;
	}

	public String getUiImage() {
		return uiImage;
	}

	public void setUiImage(String uiImage) {
		this.uiImage = uiImage;
	}

	public float getWeight() {
		return weight;
	}

	public void setWeight(float weight) {
		this.weight = weight;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public int getMinStr() {
		return minStr;
	}

	public void setMinStr(int minStr) {
		this.minStr = minStr;
	}

	public int getMinDex() {
		return minDex;
	}

	public void setMinDex(int minDex) {
		this.minDex = minDex;
	}

	public int getMinInt() {
		return minInt;
	}

	public void setMinInt(int minInt) {
		this.minInt = minInt;
	}
	
	public boolean isUsable() {
		return false;
	}
}
