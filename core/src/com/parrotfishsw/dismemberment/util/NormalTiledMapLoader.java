package com.parrotfishsw.dismemberment.util;

import java.io.IOException;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.ImageResolver;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.XmlReader.Element;

public class NormalTiledMapLoader extends TmxMapLoader {
	public NormalTiledMapLoader() {
		this(new InternalFileHandleResolver());
	}
	
	public NormalTiledMapLoader (FileHandleResolver resolver) {
		super(resolver);
	}

	@Override
	protected Array<AssetDescriptor> getDependencyAssetDescriptors (FileHandle tmxFile,
																	TextureLoader.TextureParameter textureParameter) {
		Array<AssetDescriptor> descriptors = new Array<AssetDescriptor>();

		final Array<FileHandle> fileHandles = getDependencyFileHandles(tmxFile);
		for (FileHandle handle : fileHandles) {
			descriptors.add(new AssetDescriptor(handle, Texture.class, textureParameter));
			String normalPath = handle.name().replace(".png", "-normals.png");
			FileHandle normalHandle = handle.sibling(normalPath);
			descriptors.add(new AssetDescriptor(normalHandle, Texture.class, textureParameter));
		}

		return descriptors;
	}

	protected void loadTileSet (TiledMap map, Element element, FileHandle tmxFile, ImageResolver imageResolver) {
		if (element.getName().equals("tileset")) {
			String name = element.get("name", null);
			int firstgid = element.getIntAttribute("firstgid", 1);
			int tilewidth = element.getIntAttribute("tilewidth", 0);
			int tileheight = element.getIntAttribute("tileheight", 0);
			int spacing = element.getIntAttribute("spacing", 0);
			int margin = element.getIntAttribute("margin", 0);
			String source = element.getAttribute("source", null);

			String imageSource = "";
			String normalImageSource = "";
			int imageWidth = 0, imageHeight = 0;

			FileHandle image = null;
			FileHandle normalImage = null;
			if (source != null) {
				FileHandle tsx = getRelativeFileHandle(tmxFile, source);
				//try {
					element = xml.parse(tsx);
					name = element.get("name", null);
					tilewidth = element.getIntAttribute("tilewidth", 0);
					tileheight = element.getIntAttribute("tileheight", 0);
					spacing = element.getIntAttribute("spacing", 0);
					margin = element.getIntAttribute("margin", 0);
					imageSource = element.getChildByName("image").getAttribute("source");
					imageWidth = element.getChildByName("image").getIntAttribute("width", 0);
					imageHeight = element.getChildByName("image").getIntAttribute("height", 0);
					image = getRelativeFileHandle(tsx, imageSource);
				/*} catch (IOException e) {
					throw new GdxRuntimeException("Error parsing external tileset.");
				}*/
			} else {
				imageSource = element.getChildByName("image").getAttribute("source");
				imageWidth = element.getChildByName("image").getIntAttribute("width", 0);
				imageHeight = element.getChildByName("image").getIntAttribute("height", 0);
				image = getRelativeFileHandle(tmxFile, imageSource);
			}
			normalImageSource = imageSource.replace(".png", "-normals.png");
			normalImage = getRelativeFileHandle(tmxFile, normalImageSource);

			TextureRegion texture = imageResolver.getImage(image.path());
			TextureRegion normal = imageResolver.getImage(normalImage.path());

			TiledMapTileSet tileset = new TiledMapTileSet();
			MapProperties props = tileset.getProperties();
			tileset.setName(name);
			props.put("firstgid", firstgid);
			props.put("imagesource", imageSource);
			props.put("imagewidth", imageWidth);
			props.put("imageheight", imageHeight);
			props.put("tilewidth", tilewidth);
			props.put("tileheight", tileheight);
			props.put("margin", margin);
			props.put("spacing", spacing);

			int stopWidth = texture.getRegionWidth() - tilewidth;
			int stopHeight = texture.getRegionHeight() - tileheight;

			int id = firstgid;

			for (int y = margin; y <= stopHeight; y += tileheight + spacing) {
				for (int x = margin; x <= stopWidth; x += tilewidth + spacing) {
					TextureRegion tileRegion = new TextureRegion(texture, x, y, tilewidth, tileheight);
					TextureRegion normalRegion = new TextureRegion(normal, x, y, tilewidth, tileheight);
					/*if (!yUp) {
						tileRegion.flip(false, true);
					}*/
					TiledMapTile tile = new NormalTiledMapTile(tileRegion, normalRegion);
					tile.setId(id);
					tileset.putTile(id++, tile);
				}
			}

			Array<Element> tileElements = element.getChildrenByName("tile");

			for (Element tileElement : tileElements) {
				int localtid = tileElement.getIntAttribute("id", 0);
				TiledMapTile tile = tileset.getTile(firstgid + localtid);
				if (tile != null) {
					String terrain = tileElement.getAttribute("terrain", null);
					if (terrain != null) {
						tile.getProperties().put("terrain", terrain);
					}
					String probability = tileElement.getAttribute("probability", null);
					if (probability != null) {
						tile.getProperties().put("probability", probability);
					}
					Element properties = tileElement.getChildByName("properties");
					if (properties != null) {
						loadProperties(tile.getProperties(), properties);
					}
				}
			}

			Element properties = element.getChildByName("properties");
			if (properties != null) {
				loadProperties(tileset.getProperties(), properties);
			}
			map.getTileSets().addTileSet(tileset);
		}
	}

	@Override
	protected void addStaticTiles (FileHandle tmxFile, ImageResolver imageResolver, TiledMapTileSet tileSet, Element element,
								   Array<Element> tileElements, String name, int firstgid, int tilewidth, int tileheight, int spacing, int margin,
								   String source, int offsetX, int offsetY, String imageSource, int imageWidth, int imageHeight, FileHandle image) {

		MapProperties props = tileSet.getProperties();
		if (image != null) {
			// One image for the whole tileSet
			TextureRegion texture = imageResolver.getImage(image.path());
			TextureRegion normalTex = imageResolver.getImage(image.path().replace(".png", "-normals.png"));

			props.put("imagesource", imageSource);
			props.put("imagewidth", imageWidth);
			props.put("imageheight", imageHeight);
			props.put("tilewidth", tilewidth);
			props.put("tileheight", tileheight);
			props.put("margin", margin);
			props.put("spacing", spacing);

			int stopWidth = texture.getRegionWidth() - tilewidth;
			int stopHeight = texture.getRegionHeight() - tileheight;

			int id = firstgid;

			for (int y = margin; y <= stopHeight; y += tileheight + spacing) {
				for (int x = margin; x <= stopWidth; x += tilewidth + spacing) {
					TextureRegion tileRegion = new TextureRegion(texture, x, y, tilewidth, tileheight);
					TextureRegion normalRegion = new TextureRegion(normalTex, x, y, tilewidth, tileheight);
					int tileId = id++;
					addStaticNormalMapTile(tileSet, tileRegion, normalRegion, tileId, offsetX, offsetY);
				}
			}
		} else {
			// Every tile has its own image source
			for (Element tileElement : tileElements) {
				Element imageElement = tileElement.getChildByName("image");
				if (imageElement != null) {
					imageSource = imageElement.getAttribute("source");

					if (source != null) {
						image = getRelativeFileHandle(getRelativeFileHandle(tmxFile, source), imageSource);
					} else {
						image = getRelativeFileHandle(tmxFile, imageSource);
					}
				}
				TextureRegion texture = imageResolver.getImage(image.path());
				int tileId = firstgid + tileElement.getIntAttribute("id");
				addStaticTiledMapTile(tileSet, texture, tileId, offsetX, offsetY);
			}
		}
	}

	protected void addStaticNormalMapTile (TiledMapTileSet tileSet, TextureRegion textureRegion, TextureRegion normalRegion, int tileId, float offsetX,
										  float offsetY) {
		NormalTiledMapTile tile = new NormalTiledMapTile(textureRegion, normalRegion);
		tile.setId(tileId);
		tile.setOffsetX(offsetX);
		tile.setOffsetY(flipY ? -offsetY : offsetY);
		tileSet.putTile(tileId, tile);
	}
}
