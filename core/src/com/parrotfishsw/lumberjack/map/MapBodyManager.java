package com.parrotfishsw.lumberjack.map;

import java.util.HashMap;
import java.util.Iterator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.Map;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.CircleMapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;
import com.parrotfishsw.dismemberment.DsConstants;
import com.parrotfishsw.lumberjack.entities.CuttableObject;

/**
 * @author David Saltares avid.saltares at gmail.com
 * @brief Populates box2D world with static bodies using data from a map object
 * 
 *        It uses an XML formatted materials file to assign properties to the
 *        static bodies it creates. To assign a material to a shape add a
 *        "material" custom property to the shape in question using your editor
 *        of choice (Tiled, Gleed, Tide...). Such file uses the following
 *        structure:
 * @code <materials> <material name="ice" density="1.0" restitution="0.0"
 *       friction="0.1" /> <material name="elastic" density="1.0"
 *       restitution="0.8" friction="0.8" /> </materials>
 * @endcode
 * 
 *          In case no material property is found, it'll get a default one.
 * 
 *          Map files should have a layer for destructibles named "cuttables."
 *          Cuttable objects can be linked together using "id" and "parent"
 *          attributes. Any object that specifies a "parent" must also specify
 *          an id.
 * 
 */
public class MapBodyManager {
	private Logger m_logger;
	private World m_world;
	private float m_units;
	private Array<Body> m_bodies = new Array<Body>();
	private ObjectMap<String, FixtureDef> m_materials = new ObjectMap<String, FixtureDef>();
	private HashMap<Byte, CuttableObject> objMap = new HashMap<Byte, CuttableObject>();
	public final Array<Vector2> enemyPositions = new Array<Vector2>(10);

	/**
	 * @param world
	 *            box2D world to work with.
	 * @param unitsPerPixel
	 *            conversion ratio from pixel units to box2D metres.
	 * @param materialsFile
	 *            xml file with specific physics properties to be assigned to
	 *            newly created bodies.
	 * @param loggingLevel
	 *            verbosity of the embedded logger.
	 */
	public MapBodyManager(World world, String materialsFile, int loggingLevel,
			float units) {
		m_logger = new Logger("MapBodyManager", loggingLevel);
		m_logger.info("initialising");

		m_world = world;
		m_units = units;

		FixtureDef defaultFixture = new FixtureDef();
		defaultFixture.density = 25.0f;
		defaultFixture.friction = 0.2f;
		defaultFixture.restitution = 0.0f;

		m_materials.put("default", defaultFixture);

		if (materialsFile != null) {
			loadMaterialsFile(materialsFile);
		}
	}

	/**
	 * @param map
	 *            will use the "physics" layer of this map to look for shapes in
	 *            order to create the static bodies.
	 */
	public void createPhysics(Map map) {
		createPhysics(map, "physics");
		createDestructibles(map);
		// Setup child-parent relationships
		for (CuttableObject o : objMap.values()) {
			if (o.parentId > -1) {
				CuttableObject p = objMap.get(o.parentId);
				o.parent = p;
				o.body.setType(BodyType.DynamicBody);
				o.weldToParent(m_world);
				p.children.add(o);
			}
		}
	}

	/**
	 * @param map
	 *            map to be used to create the static bodies.
	 * @param layerName
	 *            name of the layer that contains the shapes.
	 */
	public void createPhysics(Map map, String layerName) {
		MapLayer layer = map.getLayers().get(layerName);

		if (layer == null) {
			m_logger.error("layer " + layerName + " does not exist");
			return;
		}

		MapObjects objects = layer.getObjects();
		Iterator<MapObject> objectIt = objects.iterator();

		while (objectIt.hasNext()) {
			MapObject object = objectIt.next();

			if (object instanceof TextureMapObject) {
				continue;
			}

			Shape shape;

			if (object instanceof RectangleMapObject) {
				shape = getRectangle((RectangleMapObject) object);
			} else if (object instanceof PolygonMapObject) {
				shape = getPolygon((PolygonMapObject) object);
			} else if (object instanceof PolylineMapObject) {
				shape = getPolyline((PolylineMapObject) object);
			} else if (object instanceof CircleMapObject) {
				shape = getCircle((CircleMapObject) object);
			} else {
				m_logger.error("non suported shape " + object);
				continue;
			}

			MapProperties properties = object.getProperties();
			String material = (String) properties.get("material");
			if (material == null) {
				material = "default";
			}
			FixtureDef fixtureDef = m_materials.get(material);

			if (fixtureDef == null) {
				m_logger.error("material does not exist " + material
						+ " using default");
				fixtureDef = m_materials.get("default");
			}

			fixtureDef.shape = shape;
			fixtureDef.filter.categoryBits = DsConstants.Categories.MAP;

			BodyDef bodyDef = new BodyDef();
			bodyDef.type = BodyDef.BodyType.StaticBody;
			// bodyDef.position.y += 14.1f;

			Body body = m_world.createBody(bodyDef);
			body.createFixture(fixtureDef);

			m_bodies.add(body);

			fixtureDef.shape = null;
			shape.dispose();
		}
	}

	/**
	 * Load destructibles and create as dynamic bodies in the map
	 */
	public void createDestructibles(Map map) {
		MapLayer layer = map.getLayers().get("cuttables");

		if (layer == null) {
			m_logger.error("No cuttables layer found!");
			return;
		}

		MapObjects objects = layer.getObjects();
		Iterator<MapObject> objectIt = objects.iterator();

		while (objectIt.hasNext()) {
			MapObject object = objectIt.next();

			if (object instanceof TextureMapObject) {
				continue;
			}

			Shape shape;
			// float diff = 0;

			if (object instanceof RectangleMapObject) {
				shape = getRectangle((RectangleMapObject) object);
			} else if (object instanceof PolygonMapObject) {
				shape = getPolygon((PolygonMapObject) object);
			} else if (object instanceof PolylineMapObject) {
				shape = getPolyline((PolylineMapObject) object);
			} else if (object instanceof CircleMapObject) {
				shape = getCircle((CircleMapObject) object);
			} else {
				m_logger.error("non suported shape " + object);
				continue;
			}

			MapProperties properties = object.getProperties();
			String material = (String) properties.get("material");
			String anchor = (String) properties.get("anchor");
			String parent = (String) properties.get("parent");
			String id = (String) properties.get("id");
			if (material == null) {
				material = "default";
			}
			FixtureDef fixtureDef = m_materials.get(material);

			if (fixtureDef == null) {
				m_logger.error("material does not exist " + material
						+ " using default");
				fixtureDef = m_materials.get("default");
			}

			fixtureDef.shape = shape;
			fixtureDef.filter.categoryBits = DsConstants.Categories.DESTRUCTIBLE;

			BodyDef bodyDef = new BodyDef();
			bodyDef.type = BodyDef.BodyType.StaticBody;
			// bodyDef.position.y += diff;

			Body body = m_world.createBody(bodyDef);
			body.createFixture(fixtureDef);
			CuttableObject gObj = new CuttableObject(body);
			if (anchor != null) {
				gObj.setAnchor(anchor);
			}
			if (id != null) {
				gObj.id = Byte.valueOf(id);
				objMap.put(gObj.id, gObj);
			}
			if (parent != null) {
				gObj.parentId = Byte.valueOf(parent);
			}
			body.setUserData(gObj);

			// m_bodies.add(body);

			fixtureDef.shape = null;
			shape.dispose();

		}

		// ////////////////////////
		objectIt = map.getLayers().get("logic").getObjects().iterator();
		while (objectIt.hasNext()) {
			MapObject object = objectIt.next();
			if (object instanceof TextureMapObject) {
				continue;
			}
			if (object.getName().equals("player")) {
				RectangleMapObject rect = (RectangleMapObject) object;
				Rectangle rectangle = rect.getRectangle();
				map.getProperties().put("px",
						(rectangle.x + rectangle.width * 0.5f) * m_units);
				map.getProperties().put("py",
						(rectangle.y + rectangle.height * 0.5f) * m_units);
			} else if (object.getName().contains("enemy")) {
				// TODO retrieve enemy properties
				RectangleMapObject rect = (RectangleMapObject) object;
				Rectangle rectangle = rect.getRectangle();
				Vector2 pos = new Vector2(
						(rectangle.x + rectangle.width * 0.5f) * m_units,
						(rectangle.y + rectangle.height * 0.5f) * m_units);
				enemyPositions.add(pos);
			} else if (object.getName().equals("goal")) {
				Shape shape = getRectangle((RectangleMapObject) object);
				FixtureDef fd = new FixtureDef();
				fd.shape = shape;
				fd.isSensor = true;
				fd.filter.categoryBits = DsConstants.Categories.GOAL;
				BodyDef bodyDef = new BodyDef();
				bodyDef.type = BodyDef.BodyType.StaticBody;

				Body body = m_world.createBody(bodyDef);
				body.createFixture(fd).setUserData(DsConstants.Id.GOAL_ID);
				fd.shape = null;
				shape.dispose();
			}
		}
	}

	/**
	 * Destroys every static body that has been created using the manager.
	 */
	public void destroyPhysics() {
		for (Body body : m_bodies) {
			m_world.destroyBody(body);
		}

		m_bodies.clear();
	}

	private void loadMaterialsFile(String materialsFile) {
		m_logger.info("adding default material");

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.density = 1.0f;
		fixtureDef.friction = 1.0f;
		fixtureDef.restitution = 0.0f;
		m_materials.put("default", fixtureDef);

		m_logger.info("loading materials file");

		try {
			XmlReader reader = new XmlReader();
			Element root = reader.parse(Gdx.files.internal(materialsFile));

			Array<Element> materials = root.getChildrenByName("materials");

			for (Element material : materials) {
				String name = material.getAttribute("name");

				if (name == null) {
					m_logger.error("material without name");
					continue;
				}

				fixtureDef = new FixtureDef();
				fixtureDef.density = Float.parseFloat(material.getAttribute(
						"density", "1.0"));
				fixtureDef.friction = Float.parseFloat(material.getAttribute(
						"friction", "1.0"));
				fixtureDef.restitution = Float.parseFloat(material
						.getAttribute("restitution", "1.0"));
				m_logger.info("adding material " + name);
				m_materials.put(name, fixtureDef);
			}

		} catch (Exception e) {
			m_logger.error("error loading " + materialsFile + " "
					+ e.getMessage());
		}
	}

	private Shape getRectangle(RectangleMapObject rectangleObject) {
		Rectangle rectangle = rectangleObject.getRectangle();
		PolygonShape polygon = new PolygonShape();
		Vector2 size = new Vector2((rectangle.x + rectangle.width * 0.5f)
				* m_units, (rectangle.y + rectangle.height * 0.5f) * m_units);
		polygon.setAsBox(rectangle.width * 0.5f * m_units, rectangle.height
				* 0.5f * m_units, size, 0.0f);
		return polygon;
	}

	private Shape getCircle(CircleMapObject circleObject) {
		Circle circle = circleObject.getCircle();
		CircleShape circleShape = new CircleShape();
		circleShape.setRadius(circle.radius * m_units);
		circleShape.setPosition(new Vector2(circle.x * m_units, circle.y
				* m_units));
		return circleShape;
	}

	private Shape getPolygon(PolygonMapObject polygonObject) {
		Polygon poly = polygonObject.getPolygon();
		PolygonShape polygon = new PolygonShape();
		float[] vertices = poly.getTransformedVertices();
		float[] worldVertices = new float[vertices.length];

		for (int i = 0; i < vertices.length; ++i) {
			worldVertices[i] = vertices[i] * m_units;
		}

		polygon.set(worldVertices);
		return polygon;
	}

	private Shape getPolyline(PolylineMapObject polylineObject) {
		float[] vertices = polylineObject.getPolyline()
				.getTransformedVertices();
		Vector2[] worldVertices = new Vector2[vertices.length / 2];

		for (int i = 0; i < vertices.length / 2; ++i) {
			worldVertices[i] = new Vector2();
			worldVertices[i].x = vertices[i * 2] * m_units;
			worldVertices[i].y = vertices[i * 2 + 1] * m_units;
		}

		ChainShape chain = new ChainShape();
		chain.createChain(worldVertices);
		return chain;
	}
}