/* Copyright 2011 See AUTHORS file.
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
******************************************************************************/

package com.esotericsoftware.spine;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Mesh.VertexDataType;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.NumberUtils;
import com.esotericsoftware.spine.attachments.PhysicsBoundingBoxAttachment;

/** <p>
* A SpineSpriteBatch is used to draw 2D rectangles that reference a texture (region). The class will batch the drawing commands and
* optimize them for processing by the GPU.
* </p>
* 
* <p>
* To draw something with a SpineSpriteBatch one has to first call the {@link SpineSpriteBatch#begin()} method which will setup appropriate
* render states. When you are done with drawing you have to call {@link SpineSpriteBatch#end()} which will actually draw the things
* you specified.
* </p>
* 
* <p>
* All drawing commands of the SpineSpriteBatch operate in screen coordinates. The screen coordinate system has an x-axis pointing to
* the right, an y-axis pointing upwards and the origin is in the lower left corner of the screen. You can also provide your own
* transformation and projection matrices if you so wish.
* </p>
* 
* <p>
* A SpineSpriteBatch is managed. In case the OpenGL context is lost all OpenGL resources a SpineSpriteBatch uses internally get
* invalidated. A context is lost when a user switches to another application or receives an incoming call on Android. A
* SpineSpriteBatch will be automatically reloaded after the OpenGL context is restored.
* </p>
* 
* <p>
* A SpineSpriteBatch is a pretty heavy object so you should only ever have one in your program.
* </p>
* 
* <p>
* A SpineSpriteBatch works with OpenGL ES 1.x and 2.0. In the case of a 2.0 context it will use its own custom shader to draw all
* provided sprites. You can set your own custom shader via {@link #setShader(ShaderProgram)}.
* </p>
* 
* <p>
* A SpineSpriteBatch has to be disposed if it is no longer used.
* </p>
* 
* @author mzechner */
public class SpineSpriteBatch implements Batch {
	public static final int VERTEX_SIZE = 7;
	public static final int SPRITE_SIZE = 4 * VERTEX_SIZE;
	private Mesh mesh;

	private final float[] vertices;
	private final short[] triangles;
	private int vertexIndex, triangleIndex;
	private Texture lastTexture;
	private float invTexWidth = 0, invTexHeight = 0;
	private boolean drawing;

	private final Matrix4 transformMatrix = new Matrix4();
	private final Matrix4 projectionMatrix = new Matrix4();
	private final Matrix4 combinedMatrix = new Matrix4();

	private boolean blendingDisabled;
	private int blendSrcFunc = GL20.GL_SRC_ALPHA;
	private int blendDstFunc = GL20.GL_ONE_MINUS_SRC_ALPHA;

	private final ShaderProgram shader;
	private ShaderProgram customShader;
	private boolean ownsShader;

	float color = Color.WHITE.toFloatBits();
	private Color tempColor = new Color(1, 1, 1, 1);

	/** Number of render calls since the last {@link #begin()}. **/
	public int renderCalls = 0;

	/** Number of rendering calls, ever. Will not be reset unless set manually. **/
	public int totalRenderCalls = 0;

	/** The maximum number of triangles rendered in one batch so far. **/
	public int maxTrianglesInBatch = 0;

	/** Constructs a new SpineSpriteBatch with a size of 2000, the default shader, and one buffer.
	 * @see SpineSpriteBatch#SpineSpriteBatch(int, ShaderProgram) */
	public SpineSpriteBatch () {
		this(2000, null);
	}

	/** Constructs a SpineSpriteBatch with the default shader and one buffer.
	 * @see SpineSpriteBatch#SpineSpriteBatch(int, ShaderProgram) */
	public SpineSpriteBatch (int size) {
		this(size, null);
	}

	/** Constructs a new SpineSpriteBatch. Sets the projection matrix to an orthographic projection with y-axis point upwards,
	 * x-axis point to the right and the origin being in the bottom left corner of the screen. The projection will be pixel perfect
	 * with respect to the current screen resolution.
	 * <p>
	 * The defaultShader specifies the shader to use. Note that the names for uniforms for this default shader are different than
	 * the ones expect for shaders set with {@link #setShader(ShaderProgram)}. See {@link SpriteBatch#createDefaultShader()}.
	 * @param size The max number of vertices and number of triangles in a single batch. Max of 10920.
	 * @param defaultShader The default shader to use. This is not owned by the SpineSpriteBatch and must be disposed separately. */
	public SpineSpriteBatch (int size, ShaderProgram defaultShader) {
		// 32767 is max index, so 32767 / 3 - (32767 / 3 % 3) = 10920.
		if (size > 10920) throw new IllegalArgumentException("Can't have more than 10920 triangles per batch: " + size);

		mesh = new Mesh(VertexDataType.VertexArray, false, size, size * 3, new VertexAttribute(Usage.Position, 2,
			ShaderProgram.POSITION_ATTRIBUTE), new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
			new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"),
			new VertexAttribute(Usage.Generic, 1, "a_rot"), new VertexAttribute(Usage.Generic, 1, "a_flipped"));

		vertices = new float[size * VERTEX_SIZE];
		triangles = new short[size * 3];

		if (defaultShader == null) {
			shader = SpriteBatch.createDefaultShader();
			ownsShader = true;
		} else
			shader = defaultShader;

		projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}

	/** Sets up the SpineSpriteBatch for drawing. This will disable depth buffer writing. It enables blending and texturing. If
	 * you have more texture units enabled than the first one you have to disable them before calling this. Uses a screen
	 * coordinate system by default where everything is given in pixels. You can specify your own projection and modelview matrices
	 * via {@link #setProjectionMatrix(Matrix4)} and {@link #setTransformMatrix(Matrix4)}. */
	public void begin () {
		if (drawing) throw new IllegalStateException("SpineSpriteBatch.end must be called before begin.");
		renderCalls = 0;

		Gdx.gl.glDepthMask(false);
		if (customShader != null)
			customShader.begin();
		else
			shader.begin();
		setupMatrices();

		drawing = true;
		PhysicsBoundingBoxAttachment.lastNormal = null;
	}

	/** Finishes off rendering. Enables depth writes, disables blending and texturing. Must always be called after a call to
	 * {@link #begin()} */
	public void end () {
		if (!drawing) throw new IllegalStateException("SpineSpriteBatch.begin must be called before end.");
		if (vertexIndex > 0) flush();
		lastTexture = null;
		drawing = false;

		GL20 gl = Gdx.gl;
		gl.glDepthMask(true);
		if (isBlendingEnabled()) gl.glDisable(GL20.GL_BLEND);

		if (customShader != null)
			customShader.end();
		else
			shader.end();
	}

	/** Sets the color used to tint images when they are added to the SpineSpriteBatch. Default is {@link Color#WHITE}. */
	public void setColor (Color tint) {
		color = tint.toFloatBits();
	}

	/** @see #setColor(Color) */
	public void setColor (float r, float g, float b, float a) {
		int intBits = (int)(255 * a) << 24 | (int)(255 * b) << 16 | (int)(255 * g) << 8 | (int)(255 * r);
		color = NumberUtils.intToFloatColor(intBits);
	}

	/** @see #setColor(Color)
	 * @see Color#toFloatBits() */
	public void setColor (float color) {
		this.color = color;
	}

	/** @return the rendering color of this SpineSpriteBatch. Manipulating the returned instance has no effect. */
	public Color getColor () {
		int intBits = NumberUtils.floatToIntColor(color);
		Color color = this.tempColor;
		color.r = (intBits & 0xff) / 255f;
		color.g = ((intBits >>> 8) & 0xff) / 255f;
		color.b = ((intBits >>> 16) & 0xff) / 255f;
		color.a = ((intBits >>> 24) & 0xff) / 255f;
		return color;
	}

	@Override
	public void setPackedColor(float packedColor) {
		this.color = packedColor;
	}

	@Override
	public float getPackedColor() {
		return this.color;
	}

	/** Draws a polygon region with the bottom left corner at x,y having the width and height of the region. */
	public void draw (PolygonRegion region, float x, float y) {
		if (!drawing) throw new IllegalStateException("SpineSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final short[] regionTriangles = region.getTriangles();
		final int regionTrianglesLength = regionTriangles.length;
		final float[] regionVertices = region.getVertices();
		final int regionVerticesLength = regionVertices.length;

		final Texture texture = region.getRegion().getTexture();
		if (texture != lastTexture)
			switchTexture(texture);
		else if (triangleIndex + regionTrianglesLength > triangles.length || vertexIndex + regionVerticesLength > vertices.length)
			flush();

		int triangleIndex = this.triangleIndex;
		int vertexIndex = this.vertexIndex;
		final int startVertex = vertexIndex / VERTEX_SIZE;

		for (int i = 0; i < regionTrianglesLength; i++)
			triangles[triangleIndex++] = (short)(regionTriangles[i] + startVertex);
		this.triangleIndex = triangleIndex;

		final float[] vertices = this.vertices;
		final float color = this.color;
		final float[] textureCoords = region.getTextureCoords();

		for (int i = 0; i < regionVerticesLength; i += 2) {
			vertices[vertexIndex++] = regionVertices[i] + x;
			vertices[vertexIndex++] = regionVertices[i + 1] + y;
			vertices[vertexIndex++] = color;
			vertices[vertexIndex++] = textureCoords[i];
			vertices[vertexIndex++] = textureCoords[i + 1];
			//vertices[vertexIndex++] = //
		}
		this.vertexIndex = vertexIndex;
	}

	/** Draws a polygon region with the bottom left corner at x,y and stretching the region to cover the given width and height. */
	public void draw (PolygonRegion region, float x, float y, float width, float height) {
		if (!drawing) throw new IllegalStateException("SpineSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final short[] regionTriangles = region.getTriangles();
		final int regionTrianglesLength = regionTriangles.length;
		final float[] regionVertices = region.getVertices();
		final int regionVerticesLength = regionVertices.length;
		final TextureRegion textureRegion = region.getRegion();

		final Texture texture = textureRegion.getTexture();
		if (texture != lastTexture)
			switchTexture(texture);
		else if (triangleIndex + regionTrianglesLength > triangles.length || vertexIndex + regionVerticesLength > vertices.length)
			flush();

		int triangleIndex = this.triangleIndex;
		int vertexIndex = this.vertexIndex;
		final int startVertex = vertexIndex / VERTEX_SIZE;

		for (int i = 0, n = regionTriangles.length; i < n; i++)
			triangles[triangleIndex++] = (short)(regionTriangles[i] + startVertex);
		this.triangleIndex = triangleIndex;

		final float[] vertices = this.vertices;
		final float color = this.color;
		final float[] textureCoords = region.getTextureCoords();
		final float sX = width / textureRegion.getRegionWidth();
		final float sY = height / textureRegion.getRegionHeight();

		for (int i = 0; i < regionVerticesLength; i += 2) {
			vertices[vertexIndex++] = regionVertices[i] * sX + x;
			vertices[vertexIndex++] = regionVertices[i + 1] * sY + y;
			vertices[vertexIndex++] = color;
			vertices[vertexIndex++] = textureCoords[i];
			vertices[vertexIndex++] = textureCoords[i + 1];
		}
		this.vertexIndex = vertexIndex;
	}

	/** Draws the polygon region with the bottom left corner at x,y and stretching the region to cover the given width and height.
	 * The polygon region is offset by originX, originY relative to the origin. Scale specifies the scaling factor by which the
	 * polygon region should be scaled around originX, originY. Rotation specifies the angle of counter clockwise rotation of the
	 * rectangle around originX, originY. */
	public void draw (PolygonRegion region, float x, float y, float originX, float originY, float width, float height,
		float scaleX, float scaleY, float rotation) {
		if (!drawing) throw new IllegalStateException("SpineSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final short[] regionTriangles = region.getTriangles();
		final int regionTrianglesLength = regionTriangles.length;
		final float[] regionVertices = region.getVertices();
		final int regionVerticesLength = regionVertices.length;
		final TextureRegion textureRegion = region.getRegion();

		Texture texture = textureRegion.getTexture();
		if (texture != lastTexture)
			switchTexture(texture);
		else if (triangleIndex + regionTrianglesLength > triangles.length || vertexIndex + regionVerticesLength > vertices.length)
			flush();

		int triangleIndex = this.triangleIndex;
		int vertexIndex = this.vertexIndex;
		final int startVertex = vertexIndex / VERTEX_SIZE;

		for (int i = 0; i < regionTrianglesLength; i++)
			triangles[triangleIndex++] = (short)(regionTriangles[i] + startVertex);
		this.triangleIndex = triangleIndex;

		final float[] vertices = this.vertices;
		final float color = this.color;
		final float[] textureCoords = region.getTextureCoords();

		final float worldOriginX = x + originX;
		final float worldOriginY = y + originY;
		final float sX = width / textureRegion.getRegionWidth();
		final float sY = height / textureRegion.getRegionHeight();
		final float cos = MathUtils.cosDeg(rotation);
		final float sin = MathUtils.sinDeg(rotation);

		float fx, fy;
		for (int i = 0; i < regionVerticesLength; i += 2) {
			fx = (regionVertices[i] * sX - originX) * scaleX;
			fy = (regionVertices[i + 1] * sY - originY) * scaleY;
			vertices[vertexIndex++] = cos * fx - sin * fy + worldOriginX;
			vertices[vertexIndex++] = sin * fx + cos * fy + worldOriginY;
			vertices[vertexIndex++] = color;
			vertices[vertexIndex++] = textureCoords[i];
			vertices[vertexIndex++] = textureCoords[i + 1];
		}
		this.vertexIndex = vertexIndex;
	}

	/** Draws the polygon using the given vertices and triangles. Each vertices must be made up of 6 elements in this order: x, y,
	 * color, u, v, rotation. */
	public void draw (Texture texture, float[] polygonVertices, int verticesOffset, int verticesCount, short[] polygonTriangles,
		int trianglesOffset, int trianglesCount) {
		if (!drawing) throw new IllegalStateException("SpineSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final float[] vertices = this.vertices;

		if (texture != lastTexture)
			switchTexture(texture);
		else if (triangleIndex + trianglesCount > triangles.length || vertexIndex + verticesCount > vertices.length) //
			flush();

		int triangleIndex = this.triangleIndex;
		final int vertexIndex = this.vertexIndex;
		final int startVertex = vertexIndex / VERTEX_SIZE;

		for (int i = trianglesOffset, n = i + trianglesCount; i < n; i++)
			triangles[triangleIndex++] = (short)(polygonTriangles[i] + startVertex);
		this.triangleIndex = triangleIndex;

		System.arraycopy(polygonVertices, verticesOffset, vertices, vertexIndex, verticesCount);
		this.vertexIndex += verticesCount;
	}

	public void draw (Texture texture, float x, float y, float originX, float originY, float width, float height, float scaleX,
		float scaleY, float rotation, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
		if (!drawing) throw new IllegalStateException("SpineSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final float[] vertices = this.vertices;

		if (texture != lastTexture)
			switchTexture(texture);
		else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length) //
			flush();

		int triangleIndex = this.triangleIndex;
		final int startVertex = vertexIndex / VERTEX_SIZE;
		triangles[triangleIndex++] = (short)startVertex;
		triangles[triangleIndex++] = (short)(startVertex + 1);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 3);
		triangles[triangleIndex++] = (short)startVertex;
		this.triangleIndex = triangleIndex;

		// bottom left and top right corner points relative to origin
		final float worldOriginX = x + originX;
		final float worldOriginY = y + originY;
		float fx = -originX;
		float fy = -originY;
		float fx2 = width - originX;
		float fy2 = height - originY;

		// scale
		if (scaleX != 1 || scaleY != 1) {
			fx *= scaleX;
			fy *= scaleY;
			fx2 *= scaleX;
			fy2 *= scaleY;
		}

		// construct corner points, start from top left and go counter clockwise
		final float p1x = fx;
		final float p1y = fy;
		final float p2x = fx;
		final float p2y = fy2;
		final float p3x = fx2;
		final float p3y = fy2;
		final float p4x = fx2;
		final float p4y = fy;

		float x1;
		float y1;
		float x2;
		float y2;
		float x3;
		float y3;
		float x4;
		float y4;

		// rotate
		if (rotation != 0) {
			final float cos = MathUtils.cosDeg(rotation);
			final float sin = MathUtils.sinDeg(rotation);

			x1 = cos * p1x - sin * p1y;
			y1 = sin * p1x + cos * p1y;

			x2 = cos * p2x - sin * p2y;
			y2 = sin * p2x + cos * p2y;

			x3 = cos * p3x - sin * p3y;
			y3 = sin * p3x + cos * p3y;

			x4 = x1 + (x3 - x2);
			y4 = y3 - (y2 - y1);
		} else {
			x1 = p1x;
			y1 = p1y;

			x2 = p2x;
			y2 = p2y;

			x3 = p3x;
			y3 = p3y;

			x4 = p4x;
			y4 = p4y;
		}

		x1 += worldOriginX;
		y1 += worldOriginY;
		x2 += worldOriginX;
		y2 += worldOriginY;
		x3 += worldOriginX;
		y3 += worldOriginY;
		x4 += worldOriginX;
		y4 += worldOriginY;

		float u = srcX * invTexWidth;
		float v = (srcY + srcHeight) * invTexHeight;
		float u2 = (srcX + srcWidth) * invTexWidth;
		float v2 = srcY * invTexHeight;

		if (flipX) {
			float tmp = u;
			u = u2;
			u2 = tmp;
		}

		if (flipY) {
			float tmp = v;
			v = v2;
			v2 = tmp;
		}

		float color = this.color;
		int idx = this.vertexIndex;
		vertices[idx++] = x1;
		vertices[idx++] = y1;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;

		vertices[idx++] = x2;
		vertices[idx++] = y2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;

		vertices[idx++] = x3;
		vertices[idx++] = y3;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;

		vertices[idx++] = x4;
		vertices[idx++] = y4;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		this.vertexIndex = idx;
	}

	public void draw (Texture texture, float x, float y, float width, float height, int srcX, int srcY, int srcWidth,
		int srcHeight, boolean flipX, boolean flipY) {
		if (!drawing) throw new IllegalStateException("SpineSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final float[] vertices = this.vertices;

		if (texture != lastTexture)
			switchTexture(texture);
		else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length) //
			flush();

		int triangleIndex = this.triangleIndex;
		final int startVertex = vertexIndex / VERTEX_SIZE;
		triangles[triangleIndex++] = (short)startVertex;
		triangles[triangleIndex++] = (short)(startVertex + 1);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 3);
		triangles[triangleIndex++] = (short)startVertex;
		this.triangleIndex = triangleIndex;

		float u = srcX * invTexWidth;
		float v = (srcY + srcHeight) * invTexHeight;
		float u2 = (srcX + srcWidth) * invTexWidth;
		float v2 = srcY * invTexHeight;
		final float fx2 = x + width;
		final float fy2 = y + height;

		if (flipX) {
			float tmp = u;
			u = u2;
			u2 = tmp;
		}

		if (flipY) {
			float tmp = v;
			v = v2;
			v2 = tmp;
		}

		float color = this.color;
		int idx = this.vertexIndex;
		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;

		vertices[idx++] = x;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;

		vertices[idx++] = fx2;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		this.vertexIndex = idx;
	}

	public void draw (Texture texture, float x, float y, int srcX, int srcY, int srcWidth, int srcHeight) {
		if (!drawing) throw new IllegalStateException("SpineSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final float[] vertices = this.vertices;

		if (texture != lastTexture)
			switchTexture(texture);
		else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length) //
			flush();

		int triangleIndex = this.triangleIndex;
		final int startVertex = vertexIndex / VERTEX_SIZE;
		triangles[triangleIndex++] = (short)startVertex;
		triangles[triangleIndex++] = (short)(startVertex + 1);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 3);
		triangles[triangleIndex++] = (short)startVertex;
		this.triangleIndex = triangleIndex;

		final float u = srcX * invTexWidth;
		final float v = (srcY + srcHeight) * invTexHeight;
		final float u2 = (srcX + srcWidth) * invTexWidth;
		final float v2 = srcY * invTexHeight;
		final float fx2 = x + srcWidth;
		final float fy2 = y + srcHeight;

		float color = this.color;
		int idx = this.vertexIndex;
		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;

		vertices[idx++] = x;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;

		vertices[idx++] = fx2;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		this.vertexIndex = idx;
	}

	public void draw (Texture texture, float x, float y, float width, float height, float u, float v, float u2, float v2) {
		if (!drawing) throw new IllegalStateException("SpineSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final float[] vertices = this.vertices;

		if (texture != lastTexture)
			switchTexture(texture);
		else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length) //
			flush();

		int triangleIndex = this.triangleIndex;
		final int startVertex = vertexIndex / VERTEX_SIZE;
		triangles[triangleIndex++] = (short)startVertex;
		triangles[triangleIndex++] = (short)(startVertex + 1);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 3);
		triangles[triangleIndex++] = (short)startVertex;
		this.triangleIndex = triangleIndex;

		final float fx2 = x + width;
		final float fy2 = y + height;

		float color = this.color;
		int idx = this.vertexIndex;
		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;

		vertices[idx++] = x;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;

		vertices[idx++] = fx2;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		this.vertexIndex = idx;
	}

	public void draw (Texture texture, float x, float y) {
		draw(texture, x, y, texture.getWidth(), texture.getHeight());
	}

	public void draw (Texture texture, float x, float y, float width, float height) {
		if (!drawing) throw new IllegalStateException("SpineSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final float[] vertices = this.vertices;

		if (texture != lastTexture)
			switchTexture(texture);
		else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length) //
			flush();

		int triangleIndex = this.triangleIndex;
		final int startVertex = vertexIndex / VERTEX_SIZE;
		triangles[triangleIndex++] = (short)startVertex;
		triangles[triangleIndex++] = (short)(startVertex + 1);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 3);
		triangles[triangleIndex++] = (short)startVertex;
		this.triangleIndex = triangleIndex;

		final float fx2 = x + width;
		final float fy2 = y + height;
		final float u = 0;
		final float v = 1;
		final float u2 = 1;
		final float v2 = 0;

		float color = this.color;
		int idx = this.vertexIndex;
		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;

		vertices[idx++] = x;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;

		vertices[idx++] = fx2;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		this.vertexIndex = idx;
	}

	public void draw (Texture texture, float[] spriteVertices, int offset, int count) {
		if (!drawing) throw new IllegalStateException("SpineSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final float[] vertices = this.vertices;

		final int triangleCount = count / SPRITE_SIZE * 6;
		if (texture != lastTexture)
			switchTexture(texture);
		else if (triangleIndex + triangleCount > triangles.length || vertexIndex + count > vertices.length) //
			flush();

		final int vertexIndex = this.vertexIndex;
		int triangleIndex = this.triangleIndex;
		short vertex = (short)(vertexIndex / VERTEX_SIZE);
		for (int n = triangleIndex + triangleCount; triangleIndex < n; triangleIndex += 6, vertex += 4) {
			triangles[triangleIndex] = vertex;
			triangles[triangleIndex + 1] = (short)(vertex + 1);
			triangles[triangleIndex + 2] = (short)(vertex + 2);
			triangles[triangleIndex + 3] = (short)(vertex + 2);
			triangles[triangleIndex + 4] = (short)(vertex + 3);
			triangles[triangleIndex + 5] = vertex;
		}
		this.triangleIndex = triangleIndex;

		System.arraycopy(spriteVertices, offset, vertices, vertexIndex, count);
		this.vertexIndex += count;
	}

	public void draw (TextureRegion region, float x, float y) {
		draw(region, x, y, region.getRegionWidth(), region.getRegionHeight());
	}

	public void draw (TextureRegion region, float x, float y, float width, float height) {
		if (!drawing) throw new IllegalStateException("SpineSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final float[] vertices = this.vertices;

		Texture texture = region.getTexture();
		if (texture != lastTexture)
			switchTexture(texture);
		else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length) //
			flush();

		int triangleIndex = this.triangleIndex;
		final int startVertex = vertexIndex / VERTEX_SIZE;
		triangles[triangleIndex++] = (short)startVertex;
		triangles[triangleIndex++] = (short)(startVertex + 1);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 3);
		triangles[triangleIndex++] = (short)startVertex;
		this.triangleIndex = triangleIndex;

		final float fx2 = x + width;
		final float fy2 = y + height;
		final float u = region.getU();
		final float v = region.getV2();
		final float u2 = region.getU2();
		final float v2 = region.getV();

		float color = this.color;
		int idx = this.vertexIndex;
		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;

		vertices[idx++] = x;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;

		vertices[idx++] = fx2;
		vertices[idx++] = y;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		this.vertexIndex = idx;
	}

	public void draw (TextureRegion region, float x, float y, float originX, float originY, float width, float height,
		float scaleX, float scaleY, float rotation) {
		if (!drawing) throw new IllegalStateException("SpineSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final float[] vertices = this.vertices;

		Texture texture = region.getTexture();
		if (texture != lastTexture)
			switchTexture(texture);
		else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length) //
			flush();

		int triangleIndex = this.triangleIndex;
		final int startVertex = vertexIndex / VERTEX_SIZE;
		triangles[triangleIndex++] = (short)startVertex;
		triangles[triangleIndex++] = (short)(startVertex + 1);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 3);
		triangles[triangleIndex++] = (short)startVertex;
		this.triangleIndex = triangleIndex;

		// bottom left and top right corner points relative to origin
		final float worldOriginX = x + originX;
		final float worldOriginY = y + originY;
		float fx = -originX;
		float fy = -originY;
		float fx2 = width - originX;
		float fy2 = height - originY;

		// scale
		if (scaleX != 1 || scaleY != 1) {
			fx *= scaleX;
			fy *= scaleY;
			fx2 *= scaleX;
			fy2 *= scaleY;
		}

		// construct corner points, start from top left and go counter clockwise
		final float p1x = fx;
		final float p1y = fy;
		final float p2x = fx;
		final float p2y = fy2;
		final float p3x = fx2;
		final float p3y = fy2;
		final float p4x = fx2;
		final float p4y = fy;

		float x1;
		float y1;
		float x2;
		float y2;
		float x3;
		float y3;
		float x4;
		float y4;

		// rotate
		if (rotation != 0) {
			final float cos = MathUtils.cosDeg(rotation);
			final float sin = MathUtils.sinDeg(rotation);

			x1 = cos * p1x - sin * p1y;
			y1 = sin * p1x + cos * p1y;

			x2 = cos * p2x - sin * p2y;
			y2 = sin * p2x + cos * p2y;

			x3 = cos * p3x - sin * p3y;
			y3 = sin * p3x + cos * p3y;

			x4 = x1 + (x3 - x2);
			y4 = y3 - (y2 - y1);
		} else {
			x1 = p1x;
			y1 = p1y;

			x2 = p2x;
			y2 = p2y;

			x3 = p3x;
			y3 = p3y;

			x4 = p4x;
			y4 = p4y;
		}

		x1 += worldOriginX;
		y1 += worldOriginY;
		x2 += worldOriginX;
		y2 += worldOriginY;
		x3 += worldOriginX;
		y3 += worldOriginY;
		x4 += worldOriginX;
		y4 += worldOriginY;

		final float u = region.getU();
		final float v = region.getV2();
		final float u2 = region.getU2();
		final float v2 = region.getV();

		float color = this.color;
		int idx = this.vertexIndex;
		vertices[idx++] = x1;
		vertices[idx++] = y1;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v;

		vertices[idx++] = x2;
		vertices[idx++] = y2;
		vertices[idx++] = color;
		vertices[idx++] = u;
		vertices[idx++] = v2;

		vertices[idx++] = x3;
		vertices[idx++] = y3;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;

		vertices[idx++] = x4;
		vertices[idx++] = y4;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		this.vertexIndex = idx;
	}

	public void draw (TextureRegion region, float x, float y, float originX, float originY, float width, float height,
		float scaleX, float scaleY, float rotation, boolean clockwise) {
		if (!drawing) throw new IllegalStateException("SpineSpriteBatch.begin must be called before draw.");

		final short[] triangles = this.triangles;
		final float[] vertices = this.vertices;

		Texture texture = region.getTexture();
		if (texture != lastTexture)
			switchTexture(texture);
		else if (triangleIndex + 6 > triangles.length || vertexIndex + SPRITE_SIZE > vertices.length) //
			flush();

		int triangleIndex = this.triangleIndex;
		final int startVertex = vertexIndex / VERTEX_SIZE;
		triangles[triangleIndex++] = (short)startVertex;
		triangles[triangleIndex++] = (short)(startVertex + 1);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 2);
		triangles[triangleIndex++] = (short)(startVertex + 3);
		triangles[triangleIndex++] = (short)startVertex;
		this.triangleIndex = triangleIndex;

		// bottom left and top right corner points relative to origin
		final float worldOriginX = x + originX;
		final float worldOriginY = y + originY;
		float fx = -originX;
		float fy = -originY;
		float fx2 = width - originX;
		float fy2 = height - originY;

		// scale
		if (scaleX != 1 || scaleY != 1) {
			fx *= scaleX;
			fy *= scaleY;
			fx2 *= scaleX;
			fy2 *= scaleY;
		}

		// construct corner points, start from top left and go counter clockwise
		final float p1x = fx;
		final float p1y = fy;
		final float p2x = fx;
		final float p2y = fy2;
		final float p3x = fx2;
		final float p3y = fy2;
		final float p4x = fx2;
		final float p4y = fy;

		float x1;
		float y1;
		float x2;
		float y2;
		float x3;
		float y3;
		float x4;
		float y4;

		// rotate
		if (rotation != 0) {
			final float cos = MathUtils.cosDeg(rotation);
			final float sin = MathUtils.sinDeg(rotation);

			x1 = cos * p1x - sin * p1y;
			y1 = sin * p1x + cos * p1y;

			x2 = cos * p2x - sin * p2y;
			y2 = sin * p2x + cos * p2y;

			x3 = cos * p3x - sin * p3y;
			y3 = sin * p3x + cos * p3y;

			x4 = x1 + (x3 - x2);
			y4 = y3 - (y2 - y1);
		} else {
			x1 = p1x;
			y1 = p1y;

			x2 = p2x;
			y2 = p2y;

			x3 = p3x;
			y3 = p3y;

			x4 = p4x;
			y4 = p4y;
		}

		x1 += worldOriginX;
		y1 += worldOriginY;
		x2 += worldOriginX;
		y2 += worldOriginY;
		x3 += worldOriginX;
		y3 += worldOriginY;
		x4 += worldOriginX;
		y4 += worldOriginY;

		float u1, v1, u2, v2, u3, v3, u4, v4;
		if (clockwise) {
			u1 = region.getU2();
			v1 = region.getV2();
			u2 = region.getU();
			v2 = region.getV2();
			u3 = region.getU();
			v3 = region.getV();
			u4 = region.getU2();
			v4 = region.getV();
		} else {
			u1 = region.getU();
			v1 = region.getV();
			u2 = region.getU2();
			v2 = region.getV();
			u3 = region.getU2();
			v3 = region.getV2();
			u4 = region.getU();
			v4 = region.getV2();
		}

		float color = this.color;
		int idx = this.vertexIndex;
		vertices[idx++] = x1;
		vertices[idx++] = y1;
		vertices[idx++] = color;
		vertices[idx++] = u1;
		vertices[idx++] = v1;

		vertices[idx++] = x2;
		vertices[idx++] = y2;
		vertices[idx++] = color;
		vertices[idx++] = u2;
		vertices[idx++] = v2;

		vertices[idx++] = x3;
		vertices[idx++] = y3;
		vertices[idx++] = color;
		vertices[idx++] = u3;
		vertices[idx++] = v3;

		vertices[idx++] = x4;
		vertices[idx++] = y4;
		vertices[idx++] = color;
		vertices[idx++] = u4;
		vertices[idx++] = v4;
		this.vertexIndex = idx;
	}

	@Override
	public void draw(TextureRegion region, float width, float height, Affine2 transform) {

	}

	/** Causes any pending sprites to be rendered, without ending the SpineSpriteBatch. */
	public void flush () {
		if (vertexIndex == 0) return;

		renderCalls++;
		totalRenderCalls++;
		int trianglesInBatch = triangleIndex;
		if (trianglesInBatch > maxTrianglesInBatch) maxTrianglesInBatch = trianglesInBatch;

		lastTexture.bind();
		Mesh mesh = this.mesh;
		mesh.setVertices(vertices, 0, vertexIndex);
		mesh.setIndices(triangles, 0, triangleIndex);

		if (blendingDisabled) {
			Gdx.gl.glDisable(GL20.GL_BLEND);
		} else {
			Gdx.gl.glEnable(GL20.GL_BLEND);
			if (blendSrcFunc != -1) Gdx.gl.glBlendFunc(blendSrcFunc, blendDstFunc);
		}

		mesh.render(customShader != null ? customShader : shader, GL20.GL_TRIANGLES, 0, trianglesInBatch);

		vertexIndex = 0;
		triangleIndex = 0;
	}

	/** Disables blending for drawing sprites. Calling this within {@link #begin()}/{@link #end()} will flush the batch. */
	public void disableBlending () {
		flush();
		blendingDisabled = true;
	}

	/** Enables blending for sprites. Calling this within {@link #begin()}/{@link #end()} will flush the batch. */
	public void enableBlending () {
		flush();
		blendingDisabled = false;
	}

	/** Sets the blending function to be used when rendering sprites.
	 * @param srcFunc the source function, e.g. GL11.GL_SRC_ALPHA. If set to -1, SpineSpriteBatch won't change the blending
	 *           function.
	 * @param dstFunc the destination function, e.g. GL11.GL_ONE_MINUS_SRC_ALPHA */
	public void setBlendFunction (int srcFunc, int dstFunc) {
		if (blendSrcFunc == srcFunc && blendDstFunc == dstFunc) return;
		flush();
		blendSrcFunc = srcFunc;
		blendDstFunc = dstFunc;
	}

	@Override
	public void setBlendFunctionSeparate(int srcFuncColor, int dstFuncColor, int srcFuncAlpha, int dstFuncAlpha) {

	}

	public int getBlendSrcFunc () {
		return blendSrcFunc;
	}

	public int getBlendDstFunc () {
		return blendDstFunc;
	}

	@Override
	public int getBlendSrcFuncAlpha() {
		return blendSrcFunc;
	}

	@Override
	public int getBlendDstFuncAlpha() {
		return blendDstFunc;
	}

	/** Disposes all resources associated with this SpineSpriteBatch. */
	public void dispose () {
		mesh.dispose();
		if (ownsShader && shader != null) shader.dispose();
	}

	/** Returns the current projection matrix. Changing this within {@link #begin()}/{@link #end()} results in undefined behaviour. */
	public Matrix4 getProjectionMatrix () {
		return projectionMatrix;
	}

	/** Returns the current transform matrix. Changing this within {@link #begin()}/{@link #end()} results in undefined behaviour. */
	public Matrix4 getTransformMatrix () {
		return transformMatrix;
	}
	
	/** Returns the current projection matrix. Changing this within {@link #begin()}/{@link #end()} results in undefined behaviour. */
	public Matrix4 getCombinedMatrix () {
		return combinedMatrix;
	}

	/** Sets the projection matrix to be used by this SpineSpriteBatch. If this is called inside a {@link #begin()}/{@link #end()}
	 * block, the current batch is flushed to the gpu. */
	public void setProjectionMatrix (Matrix4 projection) {
		if (drawing) flush();
		projectionMatrix.set(projection);
		if (drawing) setupMatrices();
	}

	/** Sets the transform matrix to be used by this SpineSpriteBatch. If this is called inside a {@link #begin()}/{@link #end()}
	 * block, the current batch is flushed to the gpu. */
	public void setTransformMatrix (Matrix4 transform) {
		if (drawing) flush();
		transformMatrix.set(transform);
		if (drawing) setupMatrices();
	}

	private void setupMatrices () {
		combinedMatrix.set(projectionMatrix).mul(transformMatrix);
		if (customShader != null) {
			customShader.setUniformMatrix("u_projTrans", combinedMatrix);
			customShader.setUniformi("u_texture", 0);
		} else {
			shader.setUniformMatrix("u_projTrans", combinedMatrix);
			shader.setUniformi("u_texture", 0);
		}
	}

	private void switchTexture (Texture texture) {
		flush();
		lastTexture = texture;
		invTexWidth = 1.0f / texture.getWidth();
		invTexHeight = 1.0f / texture.getHeight();
	}

	/** @see Batch#setShader(ShaderProgram) */
	public void setShader (ShaderProgram shader) {
		if (drawing) {
			flush();
			if (customShader != null)
				customShader.end();
			else
				this.shader.end();
		}
		customShader = shader;
		if (drawing) {
			if (customShader != null)
				customShader.begin();
			else
				this.shader.begin();
			setupMatrices();
		}
	}

	@Override
	public ShaderProgram getShader() {
		return null;
	}

	/** @return whether blending for sprites is enabled */
	public boolean isBlendingEnabled () {
		return !blendingDisabled;
	}

	@Override
	public boolean isDrawing() {
		return drawing;
	}


	static public final int X1 = 0;
	static public final int Y1 = 1;
	static public final int C1 = 2;
	static public final int U1 = 3;
	static public final int V1 = 4;
	static public final int R1 = 5;
	static public final int F1 = 6;
	static public final int X2 = 7;
	static public final int Y2 = 8;
	static public final int C2 = 9;
	static public final int U2 = 10;
	static public final int V2 = 11;
	static public final int R2 = 12;
	static public final int F2 = 13;
	static public final int X3 = 14;
	static public final int Y3 = 15;
	static public final int C3 = 16;
	static public final int U3 = 17;
	static public final int V3 = 18;
	static public final int R3 = 19;
	static public final int F3 = 20;
	static public final int X4 = 21;
	static public final int Y4 = 22;
	static public final int C4 = 23;
	static public final int U4 = 24;
	static public final int V4 = 25;
	static public final int R4 = 26;
	static public final int F4 = 27;
}