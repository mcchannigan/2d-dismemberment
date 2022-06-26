/******************************************************************************
 * Spine Runtimes Software License
 * Version 2
 * 
 * Copyright (c) 2013, Esoteric Software
 * All rights reserved.
 * 
 * You are granted a perpetual, non-exclusive, non-sublicensable and
 * non-transferable license to install, execute and perform the Spine Runtimes
 * Software (the "Software") solely for internal use. Without the written
 * permission of Esoteric Software, you may not (a) modify, translate, adapt or
 * otherwise create derivative works, improvements of the Software or develop
 * new applications using the Software or (b) remove, delete, alter or obscure
 * any trademarks or any copyright, trademark, patent or other intellectual
 * property or proprietary rights notices on or in the Software, including
 * any copy thereof. Redistributions in binary or source form must include
 * this license and terms. THIS SOFTWARE IS PROVIDED BY ESOTERIC SOFTWARE
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL ESOTERIC SOFTARE BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *****************************************************************************/

package com.esotericsoftware.spine.attachments;

import java.util.logging.Logger;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.NumberUtils;
import com.esotericsoftware.spine.Bone;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.Slot;
import com.esotericsoftware.spine.SpineSpriteBatch;

/** Attachment that displays a texture region. */
public class RegionAttachment extends Attachment {
	static public final int BLX = 0;
	static public final int BLY = 1;
	static public final int ULX = 2;
	static public final int ULY = 3;
	static public final int URX = 4;
	static public final int URY = 5;
	static public final int BRX = 6;
	static public final int BRY = 7;

	private TextureRegion region;
	private TextureRegion normalRegion;
	private String path;
	private float x, y, scaleX = 1, scaleY = 1, rotation, width, height;
	private final float[] vertices = new float[SpineSpriteBatch.SPRITE_SIZE];
	private final float[] offset = new float[8];
	private final Color color = new Color(1, 1, 1, 1);

	public RegionAttachment (String name) {
		super(name);
	}

	public RegionAttachment (RegionAttachment orig) {
		this(orig.getName() + "_b");
		width = orig.width;
		height = orig.height;
		scaleX = orig.scaleX;
		scaleY = orig.scaleY;
		rotation = orig.rotation;
		x = orig.x;
		y = orig.y;
		setRegion(orig.getRegion());
		setNormalRegion(orig.getNormalRegion());
	}
	
	public float[] getLocalBounds(float[] toFill) {
		float localX2 = width / 2;
		float localY2 = height / 2;
		float localX = -localX2;
		float localY = -localY2;
		/*if (region instanceof AtlasRegion) {
			AtlasRegion region = (AtlasRegion)this.region;
			if (region.rotate) {
				localX += region.offsetX / region.originalWidth * width;
				localY += region.offsetY / region.originalHeight * height;
				localX2 -= (region.originalWidth - region.offsetX - region.packedHeight) / region.originalWidth * width;
				localY2 -= (region.originalHeight - region.offsetY - region.packedWidth) / region.originalHeight * height;
			} else {
				localX += region.offsetX / region.originalWidth * width;
				localY += region.offsetY / region.originalHeight * height;
				localX2 -= (region.originalWidth - region.offsetX - region.packedWidth) / region.originalWidth * width;
				localY2 -= (region.originalHeight - region.offsetY - region.packedHeight) / region.originalHeight * height;
			}
		}*/
		float scaleX = getScaleX();
		float scaleY = getScaleY();
		localX *= scaleX;
		localY *= scaleY;
		localX2 *= scaleX;
		localY2 *= scaleY;
		toFill[0] = x + localX;
		toFill[1] = y + localY;
		toFill[2] = x + localX2;
		toFill[3] = y + localY2;
		return toFill;
	}
	
	public void updateOffset () {
		float width = getWidth();
		float height = getHeight();
		float localX2 = width / 2;
		float localY2 = height / 2;
		float localX = -localX2;
		float localY = -localY2;
		if (region instanceof AtlasRegion) {
			AtlasRegion region = (AtlasRegion)this.region;
			if (region.rotate) {
				localX += region.offsetX / region.originalWidth * width;
				localY += region.offsetY / region.originalHeight * height;
				localX2 -= (region.originalWidth - region.offsetX - region.packedHeight) / region.originalWidth * width;
				localY2 -= (region.originalHeight - region.offsetY - region.packedWidth) / region.originalHeight * height;
			} else {
				localX += region.offsetX / region.originalWidth * width;
				localY += region.offsetY / region.originalHeight * height;
				localX2 -= (region.originalWidth - region.offsetX - region.packedWidth) / region.originalWidth * width;
				localY2 -= (region.originalHeight - region.offsetY - region.packedHeight) / region.originalHeight * height;
			}
		}
		float scaleX = getScaleX();
		float scaleY = getScaleY();
		localX *= scaleX;
		localY *= scaleY;
		localX2 *= scaleX;
		localY2 *= scaleY;
		float rotation = getRotation();
		float cos = MathUtils.cosDeg(rotation);
		float sin = MathUtils.sinDeg(rotation);
		float x = getX();
		float y = getY();
		float localXCos = localX * cos + x;
		float localXSin = localX * sin;
		float localYCos = localY * cos + y;
		float localYSin = localY * sin;
		float localX2Cos = localX2 * cos + x;
		float localX2Sin = localX2 * sin;
		float localY2Cos = localY2 * cos + y;
		float localY2Sin = localY2 * sin;
		float[] offset = this.offset;
		offset[BLX] = localXCos - localYSin;
		offset[BLY] = localYCos + localXSin;
		offset[ULX] = localXCos - localY2Sin;
		offset[ULY] = localY2Cos + localXSin;
		offset[URX] = localX2Cos - localY2Sin;
		offset[URY] = localY2Cos + localX2Sin;
		offset[BRX] = localX2Cos - localYSin;
		offset[BRY] = localYCos + localX2Sin;
	}

	public void setRegion (TextureRegion region) {
		if (region == null) throw new IllegalArgumentException("region cannot be null.");
		this.region = region;
		float[] vertices = this.vertices;
		if (region instanceof AtlasRegion && ((AtlasRegion)region).rotate) {
			vertices[SpineSpriteBatch.U3] = region.getU();
			vertices[SpineSpriteBatch.V3] = region.getV2();
			vertices[SpineSpriteBatch.U4] = region.getU();
			vertices[SpineSpriteBatch.V4] = region.getV();
			vertices[SpineSpriteBatch.U1] = region.getU2();
			vertices[SpineSpriteBatch.V1] = region.getV();
			vertices[SpineSpriteBatch.U2] = region.getU2();
			vertices[SpineSpriteBatch.V2] = region.getV2();
		} else {
			vertices[SpineSpriteBatch.U2] = region.getU();
			vertices[SpineSpriteBatch.V2] = region.getV2();
			vertices[SpineSpriteBatch.U3] = region.getU();
			vertices[SpineSpriteBatch.V3] = region.getV();
			vertices[SpineSpriteBatch.U4] = region.getU2();
			vertices[SpineSpriteBatch.V4] = region.getV();
			vertices[SpineSpriteBatch.U1] = region.getU2();
			vertices[SpineSpriteBatch.V1] = region.getV2();
		}
		updateOffset();
	}
	
	public void setNormalRegion(TextureRegion region) {
		normalRegion = region;
	}

	public TextureRegion getRegion () {
		if (region == null) throw new IllegalStateException("Region has not been set: " + this);
		return region;
	}
	
	public TextureRegion getNormalRegion() {
		return normalRegion;
	}
	
	public void updateWorldVertices (Slot slot, boolean premultipliedAlpha) {
		Skeleton skeleton = slot.getSkeleton();
		Color skeletonColor = skeleton.getColor();
		Color slotColor = slot.getColor();
		Color regionColor = color;
		float a = skeletonColor.a * slotColor.a * regionColor.a * 255;
		float multiplier = premultipliedAlpha ? a : 255;
		float color = NumberUtils.intToFloatColor( //
			((int)a << 24) //
				| ((int)(skeletonColor.b * slotColor.b * regionColor.b * multiplier) << 16) //
				| ((int)(skeletonColor.g * slotColor.g * regionColor.g * multiplier) << 8) //
				| (int)(skeletonColor.r * slotColor.r * regionColor.r * multiplier));
		Bone bone = slot.getBone();
		float m00 = bone.getM00(), m01 = bone.getM01(), m10 = bone.getM10(), m11 = bone.getM11();
		float rot = bone.getWorldRotation();
		boolean flip = skeleton.getFlipX();
		rot = -(rot - bone.getData().getWorldRotation()) * MathUtils.degRad;
		if(flip) {
			rot = MathUtils.PI - rot;
		}
		updateWorldVertices(skeleton.getX() + bone.getWorldX(), skeleton.getY() + bone.getWorldY(), rot, m00, m01, m10, m11, color, premultipliedAlpha, flip);
	}
	
	public void updateWorldVertices (float x, float y, float rotation, float m00, float m01, float m10, float m11, float color, boolean premultipliedAlpha, boolean flipped) {
		float[] vertices = this.vertices;
		float[] offset = this.offset;
		float offsetX, offsetY;
		float rot = rotation;
		float flip = flipped ? 1.0f : 0.0f;

		offsetX = offset[BRX];
		offsetY = offset[BRY];
		vertices[SpineSpriteBatch.X1] = offsetX * m00 + offsetY * m01 + x; // br
		vertices[SpineSpriteBatch.Y1] = offsetX * m10 + offsetY * m11 + y;
		vertices[SpineSpriteBatch.C1] = color;
		vertices[SpineSpriteBatch.R1] = rot;
		vertices[SpineSpriteBatch.F1] = flip;

		offsetX = offset[BLX];
		offsetY = offset[BLY];
		vertices[SpineSpriteBatch.X2] = offsetX * m00 + offsetY * m01 + x; // bl
		vertices[SpineSpriteBatch.Y2] = offsetX * m10 + offsetY * m11 + y;
		vertices[SpineSpriteBatch.C2] = color;
		vertices[SpineSpriteBatch.R2] = rot;
		vertices[SpineSpriteBatch.F2] = flip;

		offsetX = offset[ULX];
		offsetY = offset[ULY];
		vertices[SpineSpriteBatch.X3] = offsetX * m00 + offsetY * m01 + x; // ul
		vertices[SpineSpriteBatch.Y3] = offsetX * m10 + offsetY * m11 + y;
		vertices[SpineSpriteBatch.C3] = color;
		vertices[SpineSpriteBatch.R3] = rot;
		vertices[SpineSpriteBatch.F3] = flip;

		offsetX = offset[URX];
		offsetY = offset[URY];
		vertices[SpineSpriteBatch.X4] = offsetX * m00 + offsetY * m01 + x; // ur
		vertices[SpineSpriteBatch.Y4] = offsetX * m10 + offsetY * m11 + y;
		vertices[SpineSpriteBatch.C4] = color;
		vertices[SpineSpriteBatch.R4] = rot;
		vertices[SpineSpriteBatch.F4] = flip;
	}

	public float[] getWorldVertices () {
		return vertices;
	}
	
	public float[] getWorldVertsXY() {
		return new float[] {vertices[SpineSpriteBatch.X1], vertices[SpineSpriteBatch.Y1],
				vertices[SpineSpriteBatch.X2], vertices[SpineSpriteBatch.Y2],
				vertices[SpineSpriteBatch.X3], vertices[SpineSpriteBatch.Y3],
				vertices[SpineSpriteBatch.X4], vertices[SpineSpriteBatch.Y4],
		};
	}

	public float[] getOffset () {
		return offset;
	}

	public float getX () {
		return x;
	}

	public void setX (float x) {
		this.x = x;
	}

	public float getY () {
		return y;
	}

	public void setY (float y) {
		this.y = y;
	}

	public float getScaleX () {
		return scaleX;
	}

	public void setScaleX (float scaleX) {
		this.scaleX = scaleX;
	}

	public float getScaleY () {
		return scaleY;
	}

	public void setScaleY (float scaleY) {
		this.scaleY = scaleY;
	}

	public float getRotation () {
		return rotation;
	}

	public void setRotation (float rotation) {
		this.rotation = rotation;
	}

	public float getWidth () {
		return width;
	}

	public void setWidth (float width) {
		this.width = width;
	}

	public float getHeight () {
		return height;
	}

	public void setHeight (float height) {
		this.height = height;
	}

	public Color getColor () {
		return color;
	}

	public String getPath () {
		return path;
	}

	public void setPath (String path) {
		this.path = path;
	}
}
