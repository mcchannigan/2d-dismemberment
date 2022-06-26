package com.esotericsoftware.spine.attachments;

import java.util.HashSet;
import java.util.Set;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Transform;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.physics.box2d.joints.WeldJointDef;
import com.badlogic.gdx.utils.ShortArray;
import com.esotericsoftware.spine.Bone;
import com.esotericsoftware.spine.BoneData;
import com.esotericsoftware.spine.Slot;
import com.esotericsoftware.spine.SpineSpriteBatch;
import com.parrotfishsw.dismemberment.DsConstants;
import com.parrotfishsw.dismemberment.item.Armor;
import com.parrotfishsw.dismemberment.item.Shield;
import com.parrotfishsw.dismemberment.item.Weapon;
import com.parrotfishsw.dismemberment.util.Box2DTools;
import com.parrotfishsw.lumberjack.entities.Person;

public class PhysicsBoundingBoxAttachment extends BoundingBoxAttachment {
	private static final EarClippingTriangulator tri = new EarClippingTriangulator();
	private static final Vector2 upForce = new Vector2(0, 20);
	// private static final Vector3 tmpVec3 = new Vector3();
	private static final float[] localBounds = new float[4];
	public static Texture lastNormal = null;

	public Body body;
	/** The person that owns this attachment */
	public Person person;
	public Armor armor = null;
	public Shield shield = null;
	public Weapon weapon = null;
	public PhysicsBoundingBoxAttachment parent;
	public PhysicsBoundingBoxAttachment armorChild = null;
	public Set<PhysicsBoundingBoxAttachment> children;
	// Slice fields
	public boolean cutPiece = false;
	public Vector2 entryPt = new Vector2(0, 0);
	public Vector2 exitPt = new Vector2(0, 0);
	public boolean sliceEntered = false;
	public boolean sliceExited = false;
	// Fields for drawing chunks after dismemberment
	public Slot regionSlot;
	private Polygon poly = null;
	private Vector2 offset = null;
	private float[] renderVerts;
	private ShortArray indices = null;
	float initialRotation = 0;
	boolean flipped = false;

	public PhysicsBoundingBoxAttachment(String name) {
		super(name);
		children = new HashSet<PhysicsBoundingBoxAttachment>();
	}

	public PhysicsBoundingBoxAttachment(PhysicsBoundingBoxAttachment pb) {
		super(pb);
		children = new HashSet<PhysicsBoundingBoxAttachment>();
		offset = pb.offset;
	}

	public boolean isArmor() {
		return armor != null;
	}

	public void slice() {
		// Set all to dynamic bodies
		PhysicsBoundingBoxAttachment root = getRoot();
		if (root.body != null) {
			root.body.setType(BodyType.DynamicBody);
			root.body.getFixtureList().get(0).setSensor(false);
			if (root.armorChild != null) {
				root.armorChild.body.setType(BodyType.DynamicBody);
				root.weld(root.armorChild);
			}
		}
		root.makeDynamicChildren(true);

		/*
		 * Remove any joints attached to this body for (JointEdge j :
		 * body.getJointList()) { body.getWorld().destroyJoint(j.joint); }
		 */
		if (body != null) {
			float rot = body.getTransform().getRotation();
			float[] avgPositions = new float[4];
			Body[] newBodies = Box2DTools.slice(body, entryPt, exitPt,
					avgPositions);
			if (newBodies != null) {
				if (offset == null) {
					calculateOffset();
				}
				body = null;
				int index = person.removeBox(this);
				Vector2 pos0 = new Vector2(avgPositions[0], avgPositions[1]);
				Vector2 pos1 = new Vector2(avgPositions[2], avgPositions[3]);
				PhysicsBoundingBoxAttachment newAtt1 = new PhysicsBoundingBoxAttachment(
						name + "_a");
				newAtt1.body = newBodies[0];
				newAtt1.body.setUserData(newAtt1);
				newAtt1.person = person;
				newAtt1.regionSlot = regionSlot;
				newAtt1.initialRotation = rot;
				newAtt1.offset = offset;
				newAtt1.setVertsFromBody();
				newAtt1.cutPiece = true;
				person.addBox(newAtt1, index);
				PhysicsBoundingBoxAttachment newAtt2 = new PhysicsBoundingBoxAttachment(
						name + "_b");
				newAtt2.body = newBodies[1];
				newAtt2.body.setUserData(newAtt2);
				newAtt2.regionSlot = regionSlot;// new RegionAttachment(region);
				newAtt2.person = person;
				newAtt2.initialRotation = rot;
				newAtt2.offset = offset;
				newAtt2.setVertsFromBody();
				newAtt2.cutPiece = true;
				person.addBox(newAtt2, index);
				// Apply forces
				PhysicsBoundingBoxAttachment up;
				if (pos0.y > pos1.y) {
					up = newAtt1;
				} else {
					up = newAtt2;
				}
				up.body.applyLinearImpulse(upForce, entryPt.lerp(
						up.body.getPosition(), (up.body.getMass() * 0.25f)),
						true);
				// Need to set all child bodies to dynamic and Revolute them to
				// the
				// proper parent
				for (PhysicsBoundingBoxAttachment child : children) {
					child.parent = null;
					if (child.body != null) {
						Body newParent = newBodies[0];
						PhysicsBoundingBoxAttachment pAtt = newAtt1;
						Vector2 childPos = child.body.getPosition();
						if (childPos.dst2(pos1) < childPos.dst2(pos0)) {
							newParent = newBodies[1];
							pAtt = newAtt2;
						}
						child.parent = pAtt;
						pAtt.children.add(child);
						createRagdollJoint(newParent, child.body,
								child.regionSlot.getBone(), false);
					}
				}
				// Set proper piece to child of parent
				if (parent != null && parent.body != null) {
					parent.children.remove(this);
					PhysicsBoundingBoxAttachment cAtt = newAtt1;
					Body newChild = newBodies[0];
					Vector2 parentPos = parent.body.getPosition();
					if (parentPos.dst2(pos1) < parentPos.dst2(pos0)) {
						newChild = newBodies[1];
						cAtt = newAtt2;
						newAtt1.setToChunk(true);
					} else {
						newAtt2.setToChunk(true);
					}
					parent.children.add(cAtt);
					createRagdollJoint(parent.body, newChild,
							regionSlot.getBone(), false);
				}
				// Attempt to slice and reparent armor
				if (armorChild != null) {
					if (armorChild.sliceEntered && armorChild.sliceExited) {
						newBodies = Box2DTools.slice(armorChild.body,
								armorChild.entryPt, armorChild.exitPt,
								avgPositions);
						if (newBodies != null) {
							if (armorChild.offset == null) {
								armorChild.calculateOffset();
							}
							Vector2 apos0 = new Vector2(avgPositions[0],
									avgPositions[1]);
							// Vector2 apos1 = new Vector2(avgPositions[2],
							// avgPositions[3]);
							PhysicsBoundingBoxAttachment armAtt1 = new PhysicsBoundingBoxAttachment(
									armorChild.name + "_a");
							armAtt1.body = newBodies[0];
							armAtt1.regionSlot = armorChild.regionSlot;
							armAtt1.person = person;
							armAtt1.cutPiece = true;
							armAtt1.offset = armorChild.offset;
							armAtt1.setVertsFromBody();

							PhysicsBoundingBoxAttachment armAtt2 = new PhysicsBoundingBoxAttachment(
									armorChild.name + "_a");
							armAtt2.body = newBodies[1];
							armAtt2.regionSlot = armorChild.regionSlot;
							armAtt2.person = person;
							armAtt2.cutPiece = true;
							armAtt2.offset = armorChild.offset;
							armAtt2.setVertsFromBody();

							if (apos0.dst2(pos0) < apos0.dst2(pos1)) {
								newAtt1.armorChild = armAtt1;
								newAtt1.weld(armAtt1);
								newAtt2.armorChild = armAtt2;
								newAtt2.weld(armAtt2);
							} else {
								newAtt1.armorChild = armAtt2;
								newAtt1.weld(armAtt2);
								newAtt2.armorChild = armAtt1;
								newAtt2.weld(armAtt1);
							}
						}
					} else {
						Vector2 armorBodyPos = armorChild.body.getPosition();
						if (pos0.dst2(armorBodyPos) < pos1.dst2(armorBodyPos)) {
							newAtt1.armorChild = armorChild;
							newAtt1.weld(armorChild);
						} else {
							newAtt2.armorChild = armorChild;
							newAtt2.weld(armorChild);
						}
					}
				}
			}
		}
	}

	public void drawPoly(SpineSpriteBatch renderer, boolean flipX, boolean flipY) {
		// draw the body shape
		if (body != null) {
			if (regionSlot != null) {
				RegionAttachment region = (RegionAttachment) regionSlot
						.getAttachment();
				Texture tex = region.getRegion().getTexture();
				Transform trans = body.getTransform();

				boolean flip = person.getSkeleton().getFlipX();
				float rot = trans.getRotation();
				float flipped = 0.0f;
				if (flip) {
					rot *= -1;
					flipped = 1.0f;
				}
				// rot += initialRotation;
				rot -= (regionSlot.getBone().getData().getWorldRotation() * MathUtils.degRad);
				rot = -rot;
				if (flip) {
					rot = MathUtils.PI - rot;
				}

				TextureRegion normalRegion = region.getNormalRegion();
				if (normalRegion != null) {
					if (lastNormal == null
							|| (normalRegion.getTexture() != lastNormal)) {
						renderer.flush();
						lastNormal = normalRegion.getTexture();
						lastNormal.bind(1);
					}
					tex.bind(0);
				}

				poly.setRotation(trans.getRotation() * MathUtils.radDeg);
				poly.setPosition(trans.getPosition().x, trans.getPosition().y);
				float[] transVerts = poly.getTransformedVertices();

				for (int i = 0, n = transVerts.length; i < n; i += 2) {
					int j = (i / 2) * SpineSpriteBatch.VERTEX_SIZE;
					float vertX = transVerts[i];
					float vertY = transVerts[i + 1];
					renderVerts[j] = vertX;
					renderVerts[j + 1] = vertY;
					renderVerts[j + 5] = rot;
					renderVerts[j + 6] = flipped;
					// uvs[i + 1] = ;
				}

				// indices = tri.computeTriangles(transVerts);

				renderer.draw(tex, renderVerts, 0, renderVerts.length,
						indices.items, 0, indices.size);
				/*
				 * for (int i = 0; i < indices.size; i += 3) { int v1 =
				 * indices.get(i) * 2; int v2 = indices.get(i + 1) * 2; int v3 =
				 * indices.get(i + 2) * 2; float[] verts = new float[]
				 * {transVerts[v1 + 0], transVerts[v1 + 1], color, uvs[v1],
				 * uvs[v1 + 1], rot, flipped, transVerts[v2 + 0], transVerts[v2
				 * + 1], color, uvs[v2], uvs[v2 + 1], rot, flipped,
				 * transVerts[v3 + 0], transVerts[v3 + 1], color, uvs[v3],
				 * uvs[v3 + 1], rot, flipped }; renderer.draw(tex, verts, 0,
				 * SpineSpriteBatch.VERTEX_SIZE * 3, indices.items, 0,
				 * indices.size);
				 * 
				 * }
				 */
				if (armorChild != null) {
					armorChild.drawRegion(renderer, flipX, flipY);
				}
				// renderer.polygon(transVerts);
			}
		}
	}

	public void drawRegion(SpineSpriteBatch batch, boolean flipX, boolean flipY) {
		if (cutPiece) {
			drawPoly(batch, flipX, flipY);
			return;
		}
		if (regionSlot != null) {
			RegionAttachment region = (RegionAttachment) regionSlot
					.getAttachment();
			Texture tex = region.getRegion().getTexture();
			float[] matrix = getRotationMatrix(flipX, flipY);
			Transform trans = body.getTransform();
			boolean flip = person.getSkeleton().getFlipX();
			float rot = trans.getRotation();
			if (flip) {
				rot *= -1;
			}
			rot -= (regionSlot.getBone().getData().getWorldRotation() * MathUtils.degRad);
			rot = -rot;
			if (flip) {
				rot = MathUtils.PI - rot;
			}
			region.updateWorldVertices(trans.getPosition().x,
					trans.getPosition().y, rot, matrix[0], matrix[1],
					matrix[2], matrix[3], -1.7014117E38f, false, flip);
			float[] vertices = region.getWorldVertices();

			TextureRegion normalRegion = region.getNormalRegion();
			if (normalRegion != null) {
				if (lastNormal == null
						|| (normalRegion.getTexture() != lastNormal)) {
					batch.flush();
					lastNormal = normalRegion.getTexture();
					lastNormal.bind(1);
				}
				tex.bind(0);
			}
			/*
			 * batch.setBlendFunction(GL20.GL_SRC_ALPHA,
			 * GL20.GL_ONE_MINUS_SRC_ALPHA);
			 */
			batch.draw(tex, vertices, 0, SpineSpriteBatch.SPRITE_SIZE);
			if (armorChild != null && armorChild.regionSlot != null) {
				RegionAttachment armorRegion = (RegionAttachment) armorChild.regionSlot
						.getAttachment();
				if (armorRegion != null) {
					float arot = trans.getRotation();
					if (flip) {
						arot *= -1;
					}
					arot -= (armorChild.regionSlot.getBone().getData()
							.getWorldRotation() * MathUtils.degRad);
					arot = -arot;
					if (flip) {
						arot = MathUtils.PI - arot;
					}
					armorRegion.updateWorldVertices(trans.getPosition().x,
							trans.getPosition().y, arot, matrix[0], matrix[1],
							matrix[2], matrix[3], -1.7014117E38f, false, flip);
					vertices = armorRegion.getWorldVertices();
					normalRegion = armorRegion.getNormalRegion();
					if (lastNormal == null
							|| (normalRegion.getTexture() != lastNormal)) {
						batch.flush();
						lastNormal = normalRegion.getTexture();
						lastNormal.bind(1);
					}
					armorRegion.getRegion().getTexture().bind(0);
					batch.draw(armorRegion.getRegion().getTexture(), vertices,
							0, SpineSpriteBatch.SPRITE_SIZE);
				}
			}
		}
	}

	private void weld(PhysicsBoundingBoxAttachment weldChild) {
		WeldJointDef jointDef = new WeldJointDef();
		jointDef.initialize(body, weldChild.body, body.getWorldCenter());
		body.getWorld().createJoint(jointDef);
	}

	private void makeDynamicChildren(boolean recursive) {
		for (PhysicsBoundingBoxAttachment child : children) {
			if (child.body != null
					&& !child.body.getType().equals(BodyType.DynamicBody)) {
				child.body.setType(BodyType.DynamicBody);
				child.body.getFixtureList().get(0).setSensor(false);
				Bone childBone = child.regionSlot.getBone();
				createRagdollJoint(body, child.body, childBone, true);
				if (child.armorChild != null && child.armorChild.body != null) {
					child.armorChild.body.setType(BodyType.DynamicBody);
					child.weld(child.armorChild);
				}
			}
			if (recursive) {
				child.makeDynamicChildren(true);
			}
		}
	}

	private PhysicsBoundingBoxAttachment getRoot() {
		if (parent == null) {
			return this;
		}
		return parent.getRoot();
	}

	private void createRagdollJoint(Body parent, Body child, Bone bone,
			boolean useBonePos) {
		BoneData bDat = bone.getData();
		float rotDiff = (bDat.getRotation() - bone.getRotation())
				* MathUtils.degRad;
		Vector2 worldPos;
		if (useBonePos) {
			worldPos = new Vector2(person.getX() + bone.getWorldX(),
					person.getY() + bone.getWorldY());
		} else {
			worldPos = child.getWorldCenter();
		}
		RevoluteJointDef jointDef = new RevoluteJointDef();
		jointDef.enableLimit = true;
		jointDef.lowerAngle = bDat.getRagdollMin() + rotDiff;
		jointDef.upperAngle = bDat.getRagdollMax() + rotDiff;
		jointDef.collideConnected = false;
		jointDef.initialize(parent, child, worldPos);
		child.getWorld().createJoint(jointDef);
	}

	private float[] getRotationMatrix(boolean flipX, boolean flipY) {
		if (body != null) {
			Transform trans = body.getTransform();
			float rotDeg = trans.getRotation() * MathUtils.radDeg;
			if (flipX) {
				rotDeg *= -1;
			}
			float cos = MathUtils.cosDeg(rotDeg);
			float sin = MathUtils.sinDeg(rotDeg);
			float m00 = cos;
			float m10 = sin;
			float m01 = -sin;
			float m11 = cos;
			if (flipX) {
				m00 = -m00;
				m01 = -m01;
			}
			if (flipY) {
				m10 = -m10;
				m11 = -m11;
			}
			float[] result = { m00, m01, m10, m11 };
			return result;
		} else {
			return null;
		}
	}

	private void setVertsFromBody() {
		PolygonShape shape = (PolygonShape) body.getFixtureList().get(0)
				.getShape();
		Vector2 vec = new Vector2();
		vertices = new float[shape.getVertexCount() * 2];
		for (int i = 0; i < shape.getVertexCount(); i++) {
			shape.getVertex(i, vec);
			vertices[i * 2] = vec.x;
			vertices[(i * 2) + 1] = vec.y;
		}
		setupPolygon();
	}

	public void setupBody(Person p, World w) {
		Integer fixId = null;
		PolygonShape poly = new PolygonShape();
		poly.set(getVertices());

		BodyDef bd = new BodyDef();
		if (weapon == null) {
			bd.type = BodyType.StaticBody;
		} else {
			bd.type = BodyType.DynamicBody;
		}
		body = w.createBody(bd);
		FixtureDef fd = new FixtureDef();
		fd.isSensor = true;
		fd.shape = poly;
		fd.density = 1.0f;
		fd.friction = 1.0f;

		if (weapon == null) {
			fd.filter.categoryBits = DsConstants.Categories.BODY;
			fd.filter.maskBits = DsConstants.Categories.MAP
					| DsConstants.Categories.DESTRUCTIBLE
					| DsConstants.Categories.WEAPON;
		} else {
			fixId = Weapon.ID;
			fd.filter.categoryBits = DsConstants.Categories.WEAPON;
			fd.filter.maskBits = DsConstants.Categories.MAP
					| DsConstants.Categories.DESTRUCTIBLE
					| DsConstants.Categories.BODY
					| DsConstants.Categories.WEAPON;
		}
		Fixture fix = body.createFixture(fd);
		fix.setUserData(fixId);
		body.setUserData(this);
		person = p;
		poly.dispose();

		if (person.getSkeleton().getFlipX() != flipped) {
			flipBody();
		}
	}

	private void setupPolygon() {
		// vertices are in object space, centered around (0,0)
		poly = new Polygon(vertices);
		renderVerts = new float[(vertices.length / 2)
				* SpineSpriteBatch.VERTEX_SIZE];
		if (regionSlot != null) {
			RegionAttachment regionAttachment = (RegionAttachment) regionSlot
					.getAttachment();
			if (regionAttachment != null) {
				boolean flip = (person.getFacing() < 1);
				AtlasRegion region = (AtlasRegion) regionAttachment.getRegion();
				setPolyRotation(regionAttachment, flip);
				// Rectangle bb = poly.getBoundingRectangle();
				float[] verts = poly.getTransformedVertices();

				/*
				 * regionAttachment.setRotation(0);
				 * regionAttachment.updateOffset();
				 * regionAttachment.updateWorldVertices(0, 0, 0, 1, 0, 0, 1, 1,
				 * false, person.getFacing() < 0); float[] regionVerts =
				 * regionAttachment.getWorldVertices();
				 */

				indices = new ShortArray(tri.computeTriangles(verts));

				// regionAttachment.getLocalBounds(localBounds);

				float uMin = region.getU();
				float uMax = region.getU2();
				float vMin = region.getV();
				float vMax = region.getV2();
				float xMin = -(regionAttachment.getWidth() * regionAttachment
						.getScaleX()) * 0.5f;// localBounds[0];
				float xMax = -xMin;
				float yMin = -(regionAttachment.getHeight() * regionAttachment
						.getScaleY()) * 0.5f;
				float yMax = -yMin;

				float xSize = xMax - xMin;
				float ySize = yMax - yMin;
				float uSize = uMax - uMin;
				float vSize = vMax - vMin;

				float color = -1.7014117E38f;

				for (int i = 0, n = verts.length; i < n; i += 2) {
					int j = (i / 2) * SpineSpriteBatch.VERTEX_SIZE;
					float vertX = verts[i] + offset.x;
					float vertY = verts[i + 1] + offset.y;
					float u;
					float v;
					float diffX = ((vertX) - xMin) / xSize;
					float diffY = ((vertY) - yMin) / ySize;
					if (region.rotate) {
						if (flip) {
							u = uMin + (uSize * diffY);
						} else {
							u = uMax - (uSize * diffY);
						}
						v = vMax - (vSize * diffX);
					} else {
						if (flip) {
							u = uMax - (uSize * diffX);
						} else {
							u = uMin + (uSize * diffX);
						}
						v = vMax - (vSize * diffY);
					}
					renderVerts[j] = vertX;
					renderVerts[j + 1] = vertY;
					renderVerts[j + 2] = color;
					renderVerts[j + 3] = u;
					renderVerts[j + 4] = v;
					renderVerts[j + 5] = 0;
					renderVerts[j + 6] = 0;
					// uvs[i + 1] = ;
				}

				//float dummy = renderVerts[0];
			}
		}
	}

	private void calculateOffset() {
		poly = new Polygon(vertices);
		if (regionSlot != null) {
			RegionAttachment regionAttachment = (RegionAttachment) regionSlot
					.getAttachment();
			if (regionAttachment != null) {
				setPolyRotation(regionAttachment, person.getSkeleton().getFlipX());
				float[] verts = poly.getTransformedVertices();
				float rxMin = -(regionAttachment.getWidth() * regionAttachment
						.getScaleX()) * 0.5f;
				float ryMin = -(regionAttachment.getHeight() * regionAttachment
						.getScaleY()) * 0.5f;
				float xMin = Float.MAX_VALUE;
				float yMin = Float.MAX_VALUE;
				for (int i = 0; i < verts.length; i += 2) {
					float x = verts[i];
					float y = verts[i + 1];
					if (x < xMin) {
						xMin = x;
					}
					if (y < yMin) {
						yMin = y;
					}
				}
				if (offset == null) {
					offset = new Vector2();
				}
				offset.x = (rxMin - xMin);
				offset.y = (ryMin - yMin);
			}
		}
	}
	
	private void setPolyRotation(RegionAttachment regionAttachment, boolean flip) {
		float baseRot = regionAttachment.getRotation();
		if(flip) {
			baseRot = 180 - baseRot;
		}
		poly.setRotation(-baseRot);
	}

	public void destroyBody(World world) {
		if (body == null)
			return;
		world.destroyBody(body);
		body = null;
		if (armorChild != null) {
			armorChild.destroyBody(world);
		}
	}

	public void checkDestroy(float timeSinceDeath, Camera cam) {
		if (body != null && timeSinceDeath > DsConstants.DESPAWN_TIME) {
			Vector2 vec = body.getWorldCenter();

			if (poly == null) {
				if (!cam.frustum.sphereInFrustum(vec.x, vec.y, 0, body
						.getFixtureList().first().getShape().getRadius())) {
					destroyBody(body.getWorld());
				}
			} else if (!cam.frustum.sphereInFrustum(vec.x, vec.y, 0,
					poly.getBoundingRectangle().width)) {
				destroyBody(body.getWorld());
			}
		}
	}

	public void flipBody() {
		flipped = !flipped;
		PolygonShape shape = (PolygonShape) body.getFixtureList().first()
				.getShape();
		for (int i = 0, n = vertices.length; i < n; i += 2) {
			vertices[i] = (-vertices[i]);
		}
		shape.set(vertices);
	}

	public void setToChunk(boolean recurse) {
		if (body != null) {
			Fixture fixture = body.getFixtureList().first();
			Filter old = fixture.getFilterData();
			old.categoryBits = DsConstants.Categories.CHUNK;
			old.maskBits = DsConstants.Categories.MAP
					| DsConstants.Categories.DESTRUCTIBLE;
			// f.getFilterData().groupIndex = DsConstants.Groups.CHUNKS;
			fixture.setFilterData(old);
			if (armorChild != null) {
				armorChild.setToChunk(false);
			}
		}
		if (recurse) {
			for (PhysicsBoundingBoxAttachment child : children) {
				child.setToChunk(true);
			}
		}
	}

}
