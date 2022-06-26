package com.parrotfishsw.lumberjack.entities;

import java.util.logging.Logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.AnimationState.AnimationStateListener;
import com.esotericsoftware.spine.AnimationState.TrackEntry;
import com.esotericsoftware.spine.AnimationStateData;
import com.esotericsoftware.spine.Bone;
import com.esotericsoftware.spine.Event;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.SkeletonJson;
import com.esotericsoftware.spine.SkeletonRenderer;
import com.esotericsoftware.spine.Skin;
import com.esotericsoftware.spine.Slot;
import com.esotericsoftware.spine.SpineSpriteBatch;
import com.esotericsoftware.spine.attachments.AtlasNormalAttachmentLoader;
import com.esotericsoftware.spine.attachments.Attachment;
import com.esotericsoftware.spine.attachments.BoundingBoxAttachment;
import com.esotericsoftware.spine.attachments.PhysicsBoundingBoxAttachment;
import com.esotericsoftware.spine.attachments.RegionAttachment;
import com.parrotfishsw.dismemberment.DsConstants;
import com.parrotfishsw.dismemberment.GameScreen;
import com.parrotfishsw.dismemberment.item.Armor;
import com.parrotfishsw.dismemberment.item.Inventory;
import com.parrotfishsw.dismemberment.item.Shield;
import com.parrotfishsw.dismemberment.item.Weapon;
import com.parrotfishsw.dismemberment.item.Weapon.Strike;
import com.parrotfishsw.dismemberment.util.NoSuchBoneException;
import com.parrotfishsw.dismemberment.util.ParticleEffectPoolBox2d.PooledEffectBox2d;
import com.parrotfishsw.dismemberment.util.RagdollLoader;
import com.parrotfishsw.lumberjack.box2d.LjRayCastCallback;

public class Person implements AnimationStateListener {
	// STATIC /////////////
	public static final Integer FOOT_ID = 2;
	public static final Integer SIDE_ID = 5;
	private static final SkeletonRenderer renderer = new SkeletonRenderer();
	private static final LjRayCastCallback ray = new LjRayCastCallback();
	private static final float ANG_MIN = -0.68f;
	private static final float ANG_MAX = 0.5f;
	private static final Vector3 tmpVec3 = new Vector3();
	public static Camera camera;
	// STATE //////////////
	boolean dead = false;
	boolean disposed = false;
	boolean jumped = false;
	boolean parried = false;
	float speed = 20f;
	float time = 0;
	float torsoAngle = 0;
	public float torsoAngleToAdd = 0;
	byte queuedAction = -1;
	boolean walk = false;
	public byte crouched = 0;
	public byte blocking = 0;
	byte noise = 0;
	public byte parrying = 0;
	public byte attacking = 0;
	public byte kicking = 0;
	public byte staggered = 0;
	public short swingingWeapon = 0;
	public boolean locked = false;
	int footContacts = 0;
	boolean animLock = false;
	boolean torsoLock = false;
	boolean comboReady = false;
	private Array<PhysicsBoundingBoxAttachment> hitBoxes = new Array<PhysicsBoundingBoxAttachment>(
			10);
	private final Array<Person> victims = new Array<Person>(3);
	public final Array<Person> touching = new Array<Person>(3);
	int recentDamage = 0;
	float lastDamageTime = -1;
	// PHYSICS ////////////
	protected World world;
	public boolean ragdoll = false;
	boolean setBodyVerts = false;
	Body body;
	Fixture leftSensor;
	Fixture rightSensor;
	Vector2 bodySize = new Vector2(4f, 8f);
	public boolean crushStartSet = false;
	public Vector2 crushStart = new Vector2();
	float desiredMovement = 0;
	boolean direction = true;
	// STATS //////////////
	public float maxHp = 100;
	public float hp = maxHp;
	public float maxSp = 20;
	public float sp = maxSp;
	float spRegen = 5.5f;
	public float maxMp = 0;
	public float mp = maxMp;

	int vit = 8;
	int end = 8;
	int str = 8;
	int dex = 8;
	int intl = 8;
	int def = 8;
	int defFire = 0;
	int defLightning = 0;
	// EQUIPMENT ///////////
	int weaponSlot = 0;
	int shieldSlot = 0;
	int itemSlot = 0;
	public Inventory inventory;
	// RENDERING ///////////
	private float backAngle = 0;
	private boolean stuckRight = false;
	private boolean stuckLeft = false;
	private float stopMovement = 0;
	protected Skeleton skeleton;
	protected AnimationState flipState;
	protected AnimationState animState;
	protected AnimationState secondaryState;
	protected Array<PhysicsBoundingBoxAttachment> boxes = new Array<PhysicsBoundingBoxAttachment>(true, 20);

	// private Map<String, RegionAttachment> origRegions = new HashMap<String,
	// RegionAttachment>();

	// END RENDERING ///////

	private void init(Skeleton skel, World world) {
		skeleton = skel;
		skeleton.setSlotsToSetupPose();
		setSkeletonPosition(0, 0);
		skeleton.updateWorldTransform();

		this.world = world;
		for (Slot slot : skeleton.getDrawOrder()) {
			Attachment attachment = slot.getAttachment();
			if (attachment instanceof PhysicsBoundingBoxAttachment) {
				PhysicsBoundingBoxAttachment boxAttach = (PhysicsBoundingBoxAttachment) attachment;

				boxAttach.setupBody(this, world);
				//boxAttach.setupPolygon();

				// Build hierarchy
				if (!boxAttach.isArmor()) {
					boxes.add(boxAttach);
					Bone bone = slot.getBone();
					for (Slot bSlot : bone.getSlots()) {
						Attachment bAttach = bSlot.getAttachment();
						if (bAttach instanceof RegionAttachment) {
							boxAttach.regionSlot = bSlot;
						}
					}
					if (bone.getParent() != null) {
						boolean foundParent = false;
						Bone p = bone;
						while (!foundParent) {
							p = p.getParent();
							if (p == null) {
								break;
							}
							Array<Slot> parentSlots = p.getSlots();
							for (Slot ps : parentSlots) {
								Attachment attach = ps.getAttachment();
								if (attach != null
										&& attach.getName().contains("-box")) {
									PhysicsBoundingBoxAttachment pAtt = (PhysicsBoundingBoxAttachment) attach;
									boxAttach.parent = pAtt;
									pAtt.children.add(boxAttach);
									foundParent = true;
								}
							}
						}
						for (Bone childBone : bone.getChildren()) {
							Array<Slot> childSlots = childBone.getSlots();
							for (Slot cs : childSlots) {
								Attachment attach = cs.getAttachment();
								if (attach != null
										&& attach.getName().contains("-box")) {
									boxAttach.children
											.add((PhysicsBoundingBoxAttachment) attach);
								}

							}
						}
					}
				}
			}
		}

		AnimationStateData stateData = new AnimationStateData(
				skeleton.getData());
		stateData.setDefaultMix(0.1f);

		stateData.setMix("stand_r", "smash_heavy_2h_l", 0);
		stateData.setMix("stand_r", "smash_heavy_2h_r", 0);
		stateData.setMix("stand_r", "stab_2h_r", 0);
		stateData.setMix("stand_r", "stab_2h_l", 0);
		stateData.setMix("stand_r", "parry_2h_r", 0);
		stateData.setMix("stand_r", "parry_2h_l", 0);
		stateData.setMix("run_r", "parry_2h_r", 0);
		stateData.setMix("run_r", "parry_2h_l", 0);
		stateData.setMix("stand_r", "jump_r", 0);
		stateData.setMix("walk_r", "jump_r", 0);
		stateData.setMix("run_r", "jump_r", 0);
		stateData.setMix("stand_r", "backstep", 0);
		animState = new AnimationState(stateData);
		secondaryState = new AnimationState(stateData);
		secondaryState.setClearOnFinish(false);
		flipState = new AnimationState(stateData);
		flipState.setClearOnFinish(false);
		animState.addListener(this);
		animState.setAnimation(0, "stand_r", false);
		flipState.setAnimation(0, "flip_r", false);

		BodyDef bd = new BodyDef();
		bd.type = BodyType.DynamicBody;
		bd.fixedRotation = true;
		PolygonShape poly = new PolygonShape();
		poly.setAsBox(bodySize.x, bodySize.y);
		FixtureDef fd = new FixtureDef();
		fd.shape = poly;
		fd.friction = 0;
		fd.density = 1.0f;
		fd.isSensor = false;
		fd.restitution = 0f;
		fd.filter.categoryBits = DsConstants.Categories.PLAYER;
		fd.filter.maskBits = DsConstants.Categories.MAP
				| DsConstants.Categories.DESTRUCTIBLE
				| DsConstants.Categories.GOAL | DsConstants.Categories.PLAYER
				| DsConstants.Categories.ENEMY;
		body = world.createBody(bd);
		body.createFixture(fd);
		body.setUserData(this);
		body.setTransform(0, 0, 0);

		PolygonShape footSensor = new PolygonShape();
		footSensor.setAsBox(bodySize.x * 0.95f, 0.45f, new Vector2(0,
				-bodySize.y), 0);

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = footSensor;
		fixtureDef.isSensor = true;
		fixtureDef.friction = 0.2f;
		fixtureDef.filter.categoryBits = DsConstants.Categories.PLAYER;
		fixtureDef.filter.maskBits = DsConstants.Categories.MAP
				| DsConstants.Categories.DESTRUCTIBLE
				| DsConstants.Categories.GOAL;
		Fixture foot = body.createFixture(fixtureDef);
		foot.setUserData(FOOT_ID);

		PolygonShape sideSensor = new PolygonShape();
		sideSensor.setAsBox(0.45f, bodySize.y * 0.9f, new Vector2(-bodySize.x,
				0), 0);
		fixtureDef = new FixtureDef();
		fixtureDef.shape = sideSensor;
		fixtureDef.isSensor = true;
		fixtureDef.friction = 0.2f;
		fixtureDef.filter.categoryBits = DsConstants.Categories.PLAYER;
		fixtureDef.filter.maskBits = DsConstants.Categories.MAP
				| DsConstants.Categories.DESTRUCTIBLE
				| DsConstants.Categories.PLAYER;
		leftSensor = body.createFixture(fixtureDef);
		leftSensor.setUserData(SIDE_ID);
		sideSensor.dispose();

		sideSensor = new PolygonShape();
		sideSensor.setAsBox(0.45f, bodySize.y * 0.9f,
				new Vector2(bodySize.x, 0), 0);
		fixtureDef = new FixtureDef();
		fixtureDef.shape = sideSensor;
		fixtureDef.isSensor = true;
		fixtureDef.friction = 0.2f;
		fixtureDef.filter.categoryBits = DsConstants.Categories.PLAYER;
		fixtureDef.filter.maskBits = DsConstants.Categories.MAP
				| DsConstants.Categories.DESTRUCTIBLE
				| DsConstants.Categories.PLAYER;
		rightSensor = body.createFixture(fixtureDef);
		rightSensor.setUserData(SIDE_ID);

		footSensor.dispose();
		sideSensor.dispose();
		poly.dispose();

		inventory = new Inventory(this);
	}

	public Person(Person p) {
		init(new Skeleton(p.skeleton), p.world);
	}

	public Person(String name, World world) {
		TextureAtlas atlas = new TextureAtlas(Gdx.files.internal(name
				+ ".atlas"));
		TextureAtlas normal = new TextureAtlas(Gdx.files.internal(name
				+ "-normal.atlas"));
		AtlasNormalAttachmentLoader attachLoader = new AtlasNormalAttachmentLoader(
				atlas, normal) {
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
		SkeletonData sData = skel.readSkeletonData(Gdx.files.internal(name
				+ ".json"));

		Skeleton skeleton = new Skeleton(sData);
		skeleton.setSkin("main");
		try {
			RagdollLoader.loadRagdollForSkeleton(
					Gdx.files.internal("data/" + name + "-ragdoll.json"),
					skeleton);
		} catch (NoSuchBoneException ex) {
			Logger.getAnonymousLogger().severe(ex.getMessage());
		}
		init(skeleton, world);
	}

	public void doFrame(float delta) {
		if (!dead) {
			Weapon right = getWeapon();
			lastDamageTime += delta;
			if (lastDamageTime > DsConstants.DAMAGE_TICKS) {
				recentDamage = 0;
			}

			time += delta;
			sp += (spRegen * delta);
			if (sp > maxSp) {
				sp = maxSp;
			}
			setSkeletonPosition(body.getPosition().x, body.getPosition().y
					- (bodySize.y));
			if (!jumped && footContacts > 0) {
				body.setLinearVelocity(body.getLinearVelocity().x, 0);
			}
			move();
			if (blocking == 1) {
				block();
			}
			if (crouched == 1) {
				crouch();
			}
			if (kicking > 0) {
				for (Person p : touching) {
					if (p.staggered == 0) {
						p.stagger(!p.isFacingPerson(this));
					}
				}
			}
			if (torsoAngleToAdd != 0) {
				addUpperBodyAngle(torsoAngleToAdd);
			}
			TrackEntry track = animState.getTracks().get(0);
			if (crouched < 2
					&& (track == null || (!track.getLoop() && track
							.isComplete()))) {
				if (right != null) {
					right.resetStrikes();
				}
				comboReady = false;
				animLock = false;
				checkAnimTwoHand();
				if (footContacts < 1) {
					animState.setAnimation(0, "fall_r", false);
				} else if (queuedAction > -1) {
					switch (queuedAction) {
					case (DsConstants.Actions.ATTACK_LIGHT):
						lightAttack();
						break;
					case (DsConstants.Actions.ATTACK_HEAVY):
						heavyAttack();
						break;
					case (DsConstants.Actions.BACKSTEP):
						backstep();
						break;
					}
					queuedAction = -1;
				} else {
					if (desiredMovement == 0) {
						noise = 0;
						animState.setAnimation(0, "stand_r", false);
					} else {
						float movMagnitude = Math.abs(desiredMovement);
						if (movMagnitude > 0.5f * speed) {
							if (skeleton.getFlipX() != direction) {
								noise = 2;
								animState.setAnimation(0, "run_r", true);
							} else {
								noise = 1;
								animState.setAnimation(0, "backup", true);
							}

						} else {
							noise = 1;
							if (skeleton.getFlipX() != direction) {
								animState.setAnimation(0, "walk_r", true);
							} else {
								animState.setAnimation(0, "backup", true);
							}
						}
					}
				}
			}
		} else {
			time += delta;
			// person is dead, do blood on cut pieces (if any)
			for (PhysicsBoundingBoxAttachment att : boxes) {
				att.checkDestroy(time, camera);
			}
			if (time > DsConstants.DESPAWN_TIME && !disposed) {
				if (camera.frustum.sphereInFrustum(getX(), getY(), 0,
						bodySize.y * 2)) {
					disposed = true;
				}
			}
		}
		if (!disposed) {
			skeleton.update(delta);
			flipState.apply(skeleton);
			animState.apply(skeleton);
			secondaryState.apply(skeleton);
			if (!dead) {
				Bone backbone = skeleton.findBone("back");
				if (animState.getCurrent(0) == null) {
					// For crouching and such, cannot just add back angle
					// because it
					// did not get overwritten
					int diff = 80;
					backbone.setRotation(backAngle + diff);
				} else {
					backbone.setRotation(backbone.getRotation() + backAngle);
				}
			}
			skeleton.updateWorldTransform();

			if (!ragdoll) {
				// Position each attachment body.
				for (Slot slot : skeleton.getSlots()) {
					if (slot.getAttachment() instanceof PhysicsBoundingBoxAttachment) {
						PhysicsBoundingBoxAttachment attachment = (PhysicsBoundingBoxAttachment) slot
								.getAttachment();
						if (attachment.body != null) {
							if (setBodyVerts) {
								attachment.flipBody();
							}
							float x = skeleton.getX()
									+ slot.getBone().getWorldX();
							float y = skeleton.getY()
									+ slot.getBone().getWorldY();
							float rotation = slot.getBone().getWorldRotation();
							if (skeleton.getFlipX()) {
								rotation *= -1;
							}
							attachment.body.setTransform(x, y, rotation
									* MathUtils.degRad);
						}
					}
				}
				setBodyVerts = false;
				animState.update(delta);
				secondaryState.update(delta);
			}
		}
	}

	public void handleSideCollision(Fixture side) {
		if (desiredMovement != 0) {
			stuckRight = (desiredMovement > 0 && side == rightSensor);
			stuckLeft = (desiredMovement < 0 && side == leftSensor);

			if (stuckRight || stuckLeft) {
				// stop this faka
				stopMovement = desiredMovement;
				setMovement(0, false);
			}
		}
	}

	public void handleSideEndCollision(Fixture side) {
		if (side == rightSensor) {
			stuckRight = false;
		} else if (side == leftSensor) {
			stuckLeft = false;
		}

		if ((!stuckRight && stopMovement > 0)
				|| (!stuckLeft && stopMovement < 0)) {
			setMovement(stopMovement, false);
			stopMovement = 0;
		}
	}

	public void drawUnmasked(SpineSpriteBatch batch, ShaderProgram shader) {
		// setupShader(shader);
		if (ragdoll) {
			boolean flipX = skeleton.getFlipX();
			boolean flipY = skeleton.getFlipY();
			for (PhysicsBoundingBoxAttachment box : boxes) {
				if (box.body != null) {
					box.drawRegion(batch, flipX, flipY);
				}
			}

		} else if (!disposed) {
			renderer.draw2(batch, skeleton);
		}
	}
	
	public void strike(int type) {
		if (!dead) {
			World world = body.getWorld();
			Slot slot = skeleton.findSlot("weapon-box");
			PhysicsBoundingBoxAttachment box = (PhysicsBoundingBoxAttachment) slot
					.getAttachment();
			PolygonShape shape = (PolygonShape) box.body.getFixtureList()
					.first().getShape();
			// float rotation = slot.getBone().getWorldRotation();

			if (type == Weapon.SLASH) {
				// Only need from both sides if slashing and checking for
				// dismemberment
				Vector2 end = DsConstants.vectorPool.obtain();
				Vector2 start = DsConstants.vectorPool.obtain();
				shape.getVertex(0, start);
				start.set(box.body.getWorldPoint(start));
				shape.getVertex(skeleton.getFlipX() ? 2 : 1, end);
				end.set(box.body.getWorldPoint(end));
				ray.setup(this, type);
				world.rayCast(ray, start, end);
				world.rayCast(ray, end, start);
				for (Person p : ray.victims) {
					p.checkDamage(this);
				}
				DsConstants.vectorPool.free(start);
				DsConstants.vectorPool.free(end);
			} else if (type == Weapon.STAB || type == Weapon.BLUNT) {
				for (Person p : victims) {
					p.checkDamage(this);
				}
				victims.clear();
			} else if (type == Weapon.CRUSH) {
				Vector2 weaponEnd = DsConstants.vectorPool.obtain();
				Vector2 endSwing = DsConstants.vectorPool.obtain();
				Vector2 myPos = body.getWorldCenter();
				shape.getVertex(skeleton.getFlipX() ? 2 : 0, weaponEnd);
				weaponEnd.set(box.body.getWorldPoint(weaponEnd));
				float weapDiff = Math.abs(weaponEnd.x - myPos.x);
				ray.setup(this, type);
				for (Person p : victims) {
					Vector2 hisPos = p.dead ? new Vector2() : p.body
							.getWorldCenter();
					float diff = Math.abs(hisPos.x - myPos.x);
					if (!p.dead && diff < weapDiff) {
						hisPos.y = weaponEnd.y;
					} else {
						hisPos.set(weaponEnd);
					}
					endSwing.set(hisPos.x, hisPos.y + (bodySize.y * 1.5f));
					world.rayCast(ray, hisPos, endSwing);
					world.rayCast(ray, endSwing, hisPos);
					p.checkDamage(this);
				}
				DsConstants.vectorPool.free(weaponEnd);
				DsConstants.vectorPool.free(endSwing);
				victims.clear();
			}
		}
	}

	public void lightAttack() {
		attack(Strike.Strength.LIGHT);
	}

	public void heavyAttack() {
		attack(Strike.Strength.HEAVY);
	}

	public void attack(Strike.Strength str) {
		noise = 3;
		if (!dead && footContacts > 0) {
			Weapon right = getWeapon();
			if (right == null) {
				return;
			}
			if (comboReady || !animLock) {
				crushStartSet = false;
				comboReady = false;
				setAnimationLock();
				Strike strike = right.getStrike(str);
				if (strike != null) {
					if (sp > 0) {
						attacking = 1;
						sp -= strike.staminaCost;
						if (right.twoHand) {
							secondaryState.clearTrack(0);
						} else if (!strike.withShield) {
							unblock();
						}
						animState
								.setAnimation(0, strike.getAnimation(skeleton
										.getFlipX()), false);
					}
				}
			} else {
				if (time > 2) {
					queuedAction = str == Strike.Strength.LIGHT ? DsConstants.Actions.ATTACK_LIGHT
							: DsConstants.Actions.ATTACK_HEAVY;
					time = 0;
				}
			}
		}
	}

	public void kill() {
		if (!dead) {
			dead = true;
			secondaryState.clearTrack(0);
			animState.setAnimation(0, "die", false);
			World world = body.getWorld();
			world.destroyBody(body);
			body = null;
			time = 0;
			// ragdoll = true;
			
			// Setup ragdoll hierarchy
			for (Slot slot : skeleton.getDrawOrder()) {
				Attachment attachment = slot.getAttachment();
				if (attachment instanceof PhysicsBoundingBoxAttachment) {
					PhysicsBoundingBoxAttachment boxAttach = (PhysicsBoundingBoxAttachment) attachment;
					if (!boxAttach.isArmor()) {
						boxes.add(boxAttach);
					}
				}
			}
		}
	}

	public void addUpperBodyAngle(float add) {
		float ang = torsoAngle + add;
		if (!dead && !torsoLock) {
			if (ang > ANG_MAX)
				ang = ANG_MAX;
			if (ang < ANG_MIN)
				ang = ANG_MIN;
			torsoAngle = ang;
			int diff = 0;
			if (skeleton.getFlipX()) {
				ang = -MathUtils.PI + ang;
				diff = 180;
			}
			backAngle = (ang * MathUtils.radDeg) + diff;
		}
	}

	public float getTorsoAngle() {
		return torsoAngle;
	}

	private void setSkeletonPosition(float x, float y) {
		skeleton.setX(x);
		skeleton.setY(y);
		// skeleton.updateWorldTransform();
	}

	public void setPosition(float x, float y) {
		body.setTransform(x, y, body.getAngle());
	}

	public Skeleton getSkeleton() {
		return skeleton;
	}

	public float getX() {
		return skeleton.getX();
	}

	public float getY() {
		return skeleton.getY();
	}

	public float getCenterY() {
		return skeleton.getY() + bodySize.y;
	}

	public boolean alive() {
		return !dead;
	}
	
	public void addBox(PhysicsBoundingBoxAttachment box, int index) {
		if(index > -1) {
			boxes.insert(index, box);
		} else {
			boxes.add(box);
		}
	}

	public void addBox(PhysicsBoundingBoxAttachment box) {
		boxes.add(box);
	}

	public int removeBox(PhysicsBoundingBoxAttachment box) {
		for(int i = 0, n = boxes.size; i < n; i++) {
			if(boxes.get(i) == box) {
				boxes.removeIndex(i);
				return i;
			}
		}
		return -1;
	}

	public void equipWeapon(Weapon weapon, int equipSlot) {
		inventory.setWeaponSlot(equipSlot, weapon);
		if (equipSlot == weaponSlot) {
			equipWeapon(weapon);
		}
	}

	public void equipShield(Shield shield, int equipSlot) {
		inventory.setShieldSlot(equipSlot, shield);
		if (equipSlot == shieldSlot) {
			equipShield(shield);
		}
	}

	public void toggleWeapon() {
		if (!dead && !animLock) {
			Weapon weapon = getWeapon();
			boolean currTwoHand = (weapon != null && weapon.twoHand);
			int nextSlot = (weaponSlot + 1) % Inventory.WEAPON_SLOTS;
			weapon = inventory.getEquippedWeapon(nextSlot);
			if (weapon != null) {
				weaponSlot = nextSlot;
				equipWeapon(weapon);
				if (currTwoHand && !weapon.twoHand) {
					// bring shield back up if active
					Shield shield = inventory.getEquippedShield(shieldSlot);
					if (shield != null) {
						equipShield(shield);
					}
				}
			}
		}
	}

	public void toggleShield() {
		if (!dead && !animLock) {
			int nextSlot = (shieldSlot + 1) % Inventory.WEAPON_SLOTS;
			Shield shield = inventory.getEquippedShield(nextSlot);
			if (shield != null) {
				shieldSlot = nextSlot;
				equipShield(shield);
			}
		}
	}

	/*
	 * Each weapon skeleton has a root bone with slots that correspond to the
	 * attachments on the "weapon" bone
	 */
	public void equipWeapon(Weapon weapon) {
		unblock();
		if (weapon == null) {
			unequipWeapon();
			return;
		}
		Slot existingSlot = skeleton.findSlot("weapon-box");
		PhysicsBoundingBoxAttachment existingAtt = (PhysicsBoundingBoxAttachment) existingSlot
				.getAttachment();
		if (existingAtt != null) {
			existingAtt.destroyBody(world);
		}

		String personSlot = "weapon";
		String anim = "unsheath";
		inventory.setWeaponSlot(weaponSlot, weapon);
		Skin playerSkin = skeleton.getSkin();
		// Update player skin to use weapon attachments
		for (Slot slot : weapon.getSkeleton().getSlots()) {
			String slotName = slot.getData().getName();
			if (slotName.contains("-box")) {
				PhysicsBoundingBoxAttachment attachment = (PhysicsBoundingBoxAttachment) slot
						.getAttachment();
				attachment.weapon = weapon;
				attachment.setupBody(this, world);
				existingSlot.setAttachment(attachment);
			} else {
				playerSkin.addAttachment(
						skeleton.getData().findSlotIndex(personSlot), slotName,
						slot.getAttachment());
			}
		}
		skeleton.findSlot(personSlot).setAttachment(
				skeleton.getAttachment(personSlot, personSlot));
		if (weapon.twoHand) {
			anim += "2";
			unequipShield();
			secondaryState.setClearOnFinish(false);
			secondaryState.setAnimation(0, "heavy-2h", false);
			secondaryState.clearTrack(0);
		} else {
			secondaryState.clearTrack(0);
			Shield activeShield = inventory.shieldEquip[shieldSlot];
			if (activeShield != null) {
				equipShield(activeShield);
			}
		}
		animState.setAnimation(0, anim + (skeleton.getFlipX() ? "_l" : "_r"),
				false);
		setAnimationLock();

	}

	private void checkAnimTwoHand() {
		Weapon weapon = getWeapon();
		if (weapon != null && weapon.twoHand) {
			secondaryState.setAnimation(0, "heavy-2h", false);
		}
	}

	public void equipShield(Shield shield) {
		if (shield == null) {
			unequipShield();
			return;
		}
		Weapon right = getWeapon();
		if (right != null && right.twoHand) {
			return;
		}

		Slot existingSlot = skeleton.findSlot("shield-box");
		PhysicsBoundingBoxAttachment existingAtt = (PhysicsBoundingBoxAttachment) existingSlot
				.getAttachment();
		if (existingAtt != null) {
			existingAtt.destroyBody(world);
		}

		inventory.setShieldSlot(shieldSlot, shield);
		Skin playerSkin = skeleton.getSkin();
		// Update player skin to use weapon attachments
		for (Slot slot : shield.getSkeleton().getSlots()) {
			String slotName = slot.getData().getName();
			if (slotName.contains("-box")) {
				PhysicsBoundingBoxAttachment attachment = (PhysicsBoundingBoxAttachment) slot
						.getAttachment();
				attachment.shield = shield;
				attachment.setupBody(this, world);
				existingSlot.setAttachment(attachment);
			} else {
				playerSkin.addAttachment(
						skeleton.getData().findSlotIndex("shield"), slotName,
						slot.getAttachment());
			}
		}
		skeleton.findSlot("shield").setAttachment(
				skeleton.getAttachment("shield", "shield-back"));
	}

	/*
	 * Each armor piece is stored in its own skeleton file. It should contain
	 * one root bone with slots that correspond (in naming) to the slots in the
	 * main skeleton that will be overridden. The region attachment names should
	 * match the region attachment names from the main skeleton.
	 */
	public void equipArmor(Armor armor) {
		Skin playerSkin = skeleton.getSkin();
		if (armor.getType().equals(Armor.Type.HEAD)) {
			inventory.setHelm(armor);
		} else if (armor.getType().equals(Armor.Type.TORSO)) {
			inventory.setBody(armor);
		} else if (armor.getType().equals(Armor.Type.ARMS)) {
			inventory.setArms(armor);
		} else if (armor.getType().equals(Armor.Type.LEGS)) {
			inventory.setLegs(armor);
		}
		for (Slot slot : armor.getSkeleton().getSlots()) {
			Attachment attach = slot.getAttachment();
			String slotName = slot.getData().getName();
			if (attach instanceof RegionAttachment) {
				playerSkin.addAttachment(
						skeleton.getData().findSlotIndex(slotName),
						attach.getName(), attach);
				skeleton.findSlot(slotName).setAttachment(attach);
			} else if (attach instanceof PhysicsBoundingBoxAttachment) {
				// Add physics body
				PhysicsBoundingBoxAttachment box = (PhysicsBoundingBoxAttachment) attach;
				box.regionSlot = skeleton
						.findSlot(slotName.replace("-box", ""));
				box.armor = armor;
				Slot boxSlot = skeleton.findSlot(slotName);

				// Remove existing body (if any)
				PhysicsBoundingBoxAttachment existingAtt = (PhysicsBoundingBoxAttachment) boxSlot
						.getAttachment();
				if (existingAtt != null) {
					existingAtt.destroyBody(world);
				}

				box.setupBody(this, world);
				//box.setupPolygon();
				boxSlot.setAttachment(box);
				((PhysicsBoundingBoxAttachment) skeleton.findSlot(
						slotName.replace("-armor", "")).getAttachment()).armorChild = box;
			} else if (attach == null) {
				Array<Attachment> att = new Array<Attachment>();
				Skin armorSkin = armor.getSkeleton().getData().getSkins()
						.get(1);
				armorSkin.findAttachmentsForSlot(armor.getSkeleton()
						.findSlotIndex(slotName), att);
				int slotIndex = skeleton.getData().findSlotIndex(slotName);
				for (Attachment attachment : att) {
					playerSkin.addAttachment(slotIndex,
							attachment.getSkinName(), attachment);
				}
			}
		}
	}

	public void unequipWeapon(int slot) {
		if (slot == weaponSlot) {
			unequipWeapon();
		}
	}

	public void unequipShield(int slot) {
		if (slot == shieldSlot) {
			unequipShield();
		}
	}

	public void unequipWeapon() {
		Weapon right = getWeapon();
		if (right != null) {
			boolean twoHand = right.twoHand;
			// skeleton.findSlot("weapon").setAttachment(null);
			skeleton.getSkin().removeAttachmentsForSlot(
					skeleton.findSlotIndex("weapon"));
			Slot boxSlot = skeleton.findSlot("weapon-box");
			PhysicsBoundingBoxAttachment att = (PhysicsBoundingBoxAttachment) boxSlot
					.getAttachment();
			att.destroyBody(world);
			boxSlot.setAttachment(null);
			inventory.setWeaponSlot(weaponSlot, null);
			secondaryState.clearTrack(0);
			if (twoHand && getShield() != null) {
				equipShield(getShield());
			}
		}
	}

	public void unequipShield() {
		unblock();
		Skin playerSkin = skeleton.getSkin();
		playerSkin.removeAttachmentsForSlot(skeleton.findSlotIndex("shield"));
		Slot boxSlot = skeleton.findSlot("shield-box");
		PhysicsBoundingBoxAttachment att = (PhysicsBoundingBoxAttachment) boxSlot
				.getAttachment();
		if (att != null) {
			att.destroyBody(world);
			boxSlot.setAttachment(null);
		}
	}

	public void unequipArmor(Armor armor) {
		String[] bones = null;
		int val = inventory.unequipArmor(armor);
		if (val == 0) { // head
			bones = new String[] { "head" };
		} else if (val == 1) { // chest
			bones = new String[] { "chest", "back", "shoulder-left",
					"shoulder-right" };
		} else if (val == 2) { // arms
			bones = new String[] { "elbow-left", "hand-left", "elbow-right",
					"hand-right" };
		} else if (val == 3) { // legs
			bones = new String[] { "thigh-left", "shin-left", "foot-left",
					"thigh-right", "shin-right", "foot-right", "pelvis" };
		}
		for (String bone : bones) {
			int slotIndex = skeleton.findSlotIndex(bone + "-armor");
			skeleton.getSlots().get(slotIndex).setAttachment(null);
			skeleton.getSkin().removeAttachmentsForSlot(slotIndex);
			PhysicsBoundingBoxAttachment box = (PhysicsBoundingBoxAttachment) skeleton
					.findSlot(bone + "-armor-box").getAttachment();
			box.destroyBody(world);
			((PhysicsBoundingBoxAttachment) skeleton.findSlot(bone + "-box")
					.getAttachment()).armorChild = null;
		}
	}

	public void setWalk(boolean walk) {
		this.walk = walk;
		if (desiredMovement != 0) {
			setMovement(Math.signum(desiredMovement));
		}
	}

	/**
	 * Calculate and return this person's max item burden
	 * 
	 * @return 30 base + strength + (2 * endurance)
	 */
	public float getMaxBurden() {
		return (30 + str + (2 * end));
	}

	public void setMovement(float x) {
		setMovement(x, true);
	}

	protected void setMovement(float x, boolean overwriteStop) {
		if (dead)
			return;
		if (overwriteStop) {
			stopMovement = x;
		}
		float spd = speed;
		if (walk) {
			spd *= 0.5f;
		}
		String movAnim = spd > (speed * 0.5f) ? "run_r" : "walk_r";
		desiredMovement = x * spd;
		boolean oldDirection = skeleton.getFlipX();
		if (x != 0) {
			direction = (x > 0);
			if (!locked) {
				if (direction) {
					flipState.setAnimation(0, "flip_r", false);
					skeleton.setFlipX(false);
				} else {
					flipState.setAnimation(0, "flip_l", true);
					skeleton.setFlipX(true);
				}
				if (direction == oldDirection) {
					setBodyVerts = true;
				}
			} else if (skeleton.getFlipX() == direction) {
				movAnim = "backup";
			}
			if (footContacts > 0 && !animLock) {
				if ((desiredMovement > 0 && stuckRight)
						|| (desiredMovement < 0 && stuckLeft)) {
					stopMovement = desiredMovement;
					desiredMovement = 0;
					movAnim = "stand_r";
				}
				animState.setAnimation(0, movAnim, true);
				checkAnimTwoHand();
			}
			/*
			 * if (direction == RIGHT) { saw.setTransform(saw.getWorldCenter(),
			 * 0); } else { saw.setTransform(saw.getWorldCenter(), (float)
			 * Math.PI); }
			 */
		} else {
			if (footContacts > 0 && !animLock) {
				animState.setAnimation(0, "stand_r", false);
				checkAnimTwoHand();
			}
		}
	}

	public void stopMomentum() {
		if (!dead) {
			Vector2 vel = body.getLinearVelocity();
			Vector2 center = body.getWorldCenter();
			float impulse = body.getMass() * (0 - vel.x);
			body.applyLinearImpulse(impulse, 0, center.x, center.y, true);
		}
	}

	public void move() {
		if (!animLock) {
			Vector2 vel = body.getLinearVelocity();
			Vector2 center = body.getWorldCenter();
			float mov = desiredMovement;
			if (direction == skeleton.getFlipX() && !walk) {
				mov *= 0.65f;
			}
			float impulse = body.getMass() * (mov - vel.x);
			body.applyLinearImpulse(impulse, 0, center.x, center.y, true);
		}
	}

	public void parry() {
		int spCost = 5;
		if (sp < spCost) {
			return;
		}
		Weapon right = getWeapon();
		if (!dead && footContacts > 0 && !animLock) {
			blocking = 0;
			secondaryState.clearTrack(0);

			sp -= spCost;
			setAnimationLock();
			String anim;
			if (right != null && right.twoHand) {
				anim = "parry_2h";
				secondaryState.clearTrack(0);
			} else {
				anim = "parry";
			}
			anim = skeleton.getFlipX() ? anim + "_l" : anim + "_r";
			animState.setAnimation(0, anim, false);
		}
	}

	public void setParried() {
		animState.setAnimation(0, "parried_r", false);
		secondaryState.clearTrack(0);
		setAnimationLock();
		parried = true;
	}

	public void notifyFootTouch(int diff) {
		if (!dead) {
			if (footContacts < 1 && diff > 0) {
				jumped = false;
				setAnimationLock();
				noise = 3;
				checkAnimTwoHand();
				animState.setAnimation(0, "land_r", false);
			}
			if ((footContacts + diff) >= 0) {
				footContacts += diff;
			}
			if (footContacts < 1) {
				animState.setAnimation(0, "fall_r", false);
				checkAnimTwoHand();
			}
		}
	}

	public void jump() {
		if (footContacts > 0 && !animLock) {
			jumped = true;
			Vector2 center = body.getWorldCenter();
			body.applyLinearImpulse(0, bodySize.y * 200, center.x, center.y,
					true);
			animState.setAnimation(0, "jump_r", false);
			checkAnimTwoHand();
			// footContacts = 0;
		}
	}

	public void backstep() {
		int spCost = 5;
		if (sp < spCost) {
			return;
		}
		if (footContacts > 0 && !animLock) {
			sp -= spCost;
			unblock();
			setAnimationLock();
			animState.setAnimation(0, "backstep", false);
			checkAnimTwoHand();
		} else if (time > 2) {
			queuedAction = DsConstants.Actions.BACKSTEP;
			time = 0;
		}
	}

	public void guardbreak() {
		int spCost = 5;
		if (sp < spCost) {
			return;
		}
		if (footContacts > 0 && !animLock) {
			sp -= spCost;
			unblock();
			setAnimationLock();
			String anim = "bash";
			Weapon right = getWeapon();
			if (right != null && right.twoHand) {
				secondaryState.clearTrack(0);
				anim += "2h";
			}
			anim += (skeleton.getFlipX() ? "_l" : "_r");
			animState.setAnimation(0, anim, false);
			checkAnimTwoHand();
		} else if (time > 2) {
			queuedAction = DsConstants.Actions.GUARDBREAK;
			time = 0;
		}
	}

	public void crouch() {
		crouched = 1;
		if (footContacts > 0 && !animLock) {
			setAnimationLock();
			checkAnimTwoHand();
			animState.setAnimation(0, "crouch", false);
			crouched = 2;
		}
	}

	public void uncrouch() {
		if (crouched == 2) {
			checkAnimTwoHand();
			animState.setAnimation(0, "standup", false);
		}
		crouched = 0;
	}

	public void block() {
		Weapon right = getWeapon();
		if ((right == null || !right.twoHand) && getShield() != null) {
			blocking = 1;
			if (!animLock || crouched == 2) {
				String anim = skeleton.getFlipX() ? "block_l" : "block_r";
				secondaryState.setClearOnFinish(false);
				secondaryState.setAnimation(0, anim, false);
				blocking = 2;
			}
		}
	}

	public void unblock() {
		if (blocking == 2) {
			String anim = skeleton.getFlipX() ? "unblock_l" : "unblock_r";
			secondaryState.setClearOnFinish(true);
			secondaryState.setAnimation(0, anim, false);
			blocking = 1;
		}
	}

	public void stopBlocking() {
		unblock();
		blocking = 0;
	}

	public void stagger(boolean behind) {
		blocking = 0;
		setAnimationLock();
		String anim = behind ? "stagger2" : "stagger";
		secondaryState.clearTrack(0);
		animState.setAnimation(0, anim, false);
		staggered = 1;
		if (swingingWeapon > 0) {
			swingingWeapon = 0;
			victims.clear();
		}
	}

	private void setAnimationLock() {
		setAnimationLock(true);
	}

	public void notifyHit(PhysicsBoundingBoxAttachment att) {
		if (!hitBoxes.contains(att, true)) {
			hitBoxes.add(att);
		}
	}

	public void checkDamage(Person attacker) {
		boolean damage = true;
		if (dead) {
			for (PhysicsBoundingBoxAttachment att : hitBoxes) {
				if (att.body != null) {
					doBloodEffect(att.body.getPosition(),
							attacker.getX() < getX());
				}
			}
			hitBoxes.clear();
			return;
		}
		Weapon weap = attacker.getWeapon();
		Strike strike = weap.getCurrentStrike();
		float multiplier = 1;
		int armorCount = 0;
		float armorPhys = 0, armorFire = 0, armorLightning = 0;
		boolean shieldHit = false;
		boolean unArmoredHit = false;
		boolean parrySuccess = false;
		boolean didStagger = false;
		for (PhysicsBoundingBoxAttachment att : hitBoxes) {
			boolean isShield = att.getName().contains("shield-box");
			boolean isArm = att.getName().contains("shoulder-left");
			if (att.getName().contains("weapon-box")) {
				if (parrying > 0 && getWeapon().twoHand) {
					parrySuccess = true;
					break;
				} else if (hitBoxes.size == 1) {
					// weapon is only thing hit, no damage
					damage = false;
					break;
				}
				continue;
			}
			if (isShield || isArm) {
				if (parrying > 0) {
					parrySuccess = true;
					break;
				} else if (blocking == 2) {
					shieldHit = true;
					break;
				}
			} else if (att.isArmor()) {
				if (att.armor.armorClass == Armor.Class.HEAVY) {
					if (strike.type == Weapon.SLASH) {
						multiplier = 2;
					} else if (strike.type == Weapon.STAB) {
						multiplier = 0.75f;
					}
				} else if (att.armor.armorClass == Armor.Class.LIGHT) {
					if (strike.type == Weapon.SLASH) {
						multiplier = 0.75f;
					}
				} else {
					multiplier = 1;
				}
				armorPhys += att.armor.defense * multiplier;
				armorFire += att.armor.defFire;
				armorLightning += att.armor.defLightning;
				armorCount++;
				doSparkEffect(att.body.getPosition(), attacker.getX() < getX());
			} else {
				if (!isShield
						&& (att.armorChild == null || !hitBoxes.contains(
								att.armorChild, true))) {
					unArmoredHit = true;
					doBloodEffect(att.body.getPosition(),
							attacker.getX() < getX());
				}
			}
		}

		if (parrySuccess) {
			attacker.setParried();
			// queuedAction = DsConstants.Actions.ATTACK_HEAVY;
		} else if (damage) {
			if (unArmoredHit) {
				armorPhys = armorFire = armorLightning = 0;
			}

			float phys = weap.damage + (weap.scaleStr * attacker.str);
			phys += (weap.scaleDex * attacker.dex);
			if (parried) {
				phys *= 2;
			}

			Shield shield = getShield();
			if (shieldHit)
				phys -= (phys * shield.physReduction);

			if (armorCount > 0) {
				phys -= ((armorPhys / armorCount) * 0.25f);
			}
			phys -= (def * 2);
			if (phys < 0) {
				phys = 0;
			}

			float fire = weap.damageFire;
			fire += (weap.scaleInt * attacker.intl);
			fire -= armorFire;
			fire -= defFire * 2;
			if (fire < 0) {
				fire = 0;
			}
			if (shieldHit)
				fire -= (fire * shield.fireReduction);

			float lightning = weap.damageLightning;
			lightning += (weap.scaleInt * attacker.intl);
			lightning -= armorLightning;
			lightning -= defLightning * 2;
			if (lightning < 0) {
				lightning = 0;
			}
			if (shieldHit)
				lightning -= (lightning * shield.lightningReduction);

			float totDmg = phys + fire + lightning;
			if (totDmg > 0) {
				recentDamage += totDmg;
				lastDamageTime = 0;
				hp -= totDmg;
				if (hp < 1) {
					kill();

				} else {
					// Check for stagger
					boolean attackerBehind = false;
					float diff = getX() - attacker.getX();
					if (direction) {
						attackerBehind = (diff > 0);
					} else {
						attackerBehind = (diff < 0);
					}

					if (attackerBehind) {
						didStagger = true;
						stagger(true);
					} else if (totDmg > (maxHp / 4)) {
						didStagger = true;
						stagger(false);
					}
				}
			}
		} else if (!didStagger || kicking < 0) {
			stagger(!isFacingPerson(attacker));
		}

		hitBoxes.clear();
	}

	/**
	 * 
	 * @return -1 if facing left, 1 if facing right
	 */
	public int getFacing() {
		return (skeleton.getFlipX() ? -1 : 1);
	}

	public boolean isFacingPerson(Person p) {
		boolean posDir = p.getX() > getX();
		return (posDir ^ skeleton.getFlipX());
	}

	public boolean isCrouched() {
		return crouched == 2;
	}

	public Vector2 getScreenPosition(Vector2 vec) {
		tmpVec3.set(body.getWorldCenter(), 0);
		tmpVec3.y -= (bodySize.y);
		camera.project(tmpVec3);
		vec.set(tmpVec3.x, tmpVec3.y);
		return vec;
	}

	private void setAnimationLock(boolean stop) {
		animLock = true;
		if (stop)
			stopMomentum();
	}

	public int getRecentDamage() {
		return recentDamage;
	}

	public void addVictim(Person person) {
		if (!victims.contains(person, true)) {
			victims.add(person);
		}
	}

	public Weapon getWeapon() {
		return inventory.getEquippedWeapon(weaponSlot);
	}

	public Shield getShield() {
		return inventory.getEquippedShield(shieldSlot);
	}

	public void doBloodEffect(Vector2 point, boolean flipX) {
		PooledEffectBox2d effect = GameScreen.bloodPool.obtain();
		effect.setPosition(point.x, point.y + 0.5f);
		effect.setFlip(flipX, false);
		GameScreen.effects.add(effect);
	}

	public void doSparkEffect(Vector2 point, boolean flipX) {
		PooledEffectBox2d effect = GameScreen.sparkPool.obtain();
		effect.setPosition(point.x, point.y + 0.5f);
		effect.setFlip(flipX, false);
		GameScreen.effects.add(effect);
	}

	// //////////// ANIMATION STATE LISTENER HANDLERS ////////////////
	@Override
	public void event(int trackIndex, Event event) {
		String eventName = event.getData().getName();
		if (eventName.equals("ready")) {
			comboReady = true;
			stopMomentum();
			if (queuedAction == DsConstants.Actions.ATTACK_HEAVY) {
				heavyAttack();
				queuedAction = -1;
			} else if (queuedAction == DsConstants.Actions.ATTACK_LIGHT) {
				lightAttack();
				queuedAction = -1;
			}
		} else if (eventName.equals("step")) {
			int dir = event.getInt();
			if (dir == 0) {
				stopMomentum();
			} else {
				float spd = skeleton.getFlipX() ? -speed : speed;
				spd *= 0.5f * dir;
				Vector2 vel = body.getLinearVelocity();
				Vector2 center = body.getWorldCenter();
				float impulse = body.getMass() * (spd - vel.x);
				body.applyLinearImpulse(impulse, 0, center.x, center.y, true);
			}
		} else if (eventName.equals("attack")) {
			// Do attack checking
			Strike currStrike = getWeapon().getCurrentStrike();
			int state = event.getInt();
			if (state == 0) {
				attacking = 3;
				torsoLock = true;
				strike(currStrike.type);
				swingingWeapon = 0;
			} else if (state == 1) {
				attacking = 2;
				swingingWeapon = (short) currStrike.type;
			}
		} else if (eventName.equals("parry")) {
			int state = event.getInt();
			parrying = (byte) state;
		} else if (eventName.equals("kick")) {
			kicking = (byte) event.getInt();
		}
	}

	@Override
	public void complete(int trackIndex, int loopCount) {
		/*
		 * right.resetStrikes(); comboReady = false;
		 */
	}

	@Override
	public void start(int trackIndex) {
		// TODO Auto-generated method stub

	}

	@Override
	public void end(int trackIndex) {
		torsoLock = false;
		parried = false;
		attacking = 0;
		staggered = 0;
	}
	// //////////END ANIMATION STATE LISTENER HANDLERS /////////////
}
