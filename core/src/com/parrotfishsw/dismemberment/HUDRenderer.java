package com.parrotfishsw.dismemberment;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.parrotfishsw.dismemberment.item.Armor;
import com.parrotfishsw.dismemberment.item.Equipment;
import com.parrotfishsw.dismemberment.item.Inventory;
import com.parrotfishsw.dismemberment.item.Shield;
import com.parrotfishsw.dismemberment.item.Weapon;
import com.parrotfishsw.dismemberment.ui.EquipmentButton;
import com.parrotfishsw.dismemberment.ui.EquipmentTable;
import com.parrotfishsw.lumberjack.entities.Enemy;
import com.parrotfishsw.lumberjack.entities.Player;

public class HUDRenderer {
	I18NBundle invBundle = null;
	Stage inventoryStage;
	Skin skin;
	Window invWindow;
	Window contextWindow;

	Vector2 tmpVec = new Vector2();

	Table statusDisplay;
	Label nameLabel;

	public Array<Array<EquipmentButton>> inputHierarchy = new Array<Array<EquipmentButton>>();
	public Array<Button> modalButtons = new Array<Button>(3);
	public int modalFocus;
	public int focusedRow;
	public EquipmentButton focusedButton;
	public EquipmentButton selectedButton;

	// Virtual w/h for ui viewport
	Vector2 resolution = new Vector2(1920, 1080);
	Vector2 position;
	Player player;
	Array<Enemy> enemies;
	SpriteBatch batch;
	TextureAtlas uiAtlas;
	NinePatchDrawable barBorderPatch;
	NinePatchDrawable barPatch;
	NinePatchDrawable windowPatch;
	BitmapFont font;
	float enemyBarWidth;
	public boolean menuOpen;
	public boolean contextMenuOpen = false;
	public boolean refreshMouse = false;

	public HUDRenderer(Vector2 resolution, Player player, Array<Enemy> enemies,
			TextureAtlas atlas, SpriteBatch batch) {
		invBundle = I18NBundle.createBundle(Gdx.files
				.internal("strings/inventory"));
		this.player = player;
		this.enemies = enemies;
		uiAtlas = atlas;
		skin = new Skin(Gdx.files.internal("data/uiskin.json"));
		windowPatch = new NinePatchDrawable(new NinePatch(
				atlas.findRegion("ui"), 42, 42, 42, 42));
		barBorderPatch = new NinePatchDrawable(new NinePatch(
				atlas.findRegion("bar"), 12, 12, 12, 12));
		barPatch = new NinePatchDrawable(new NinePatch(
				atlas.findRegion("bar-fill"), 12, 12, 12, 12));
		this.batch = new SpriteBatch();
		// this.batch.getProjectionMatrix().setToOrtho2D(0, 0,
		// Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		font = new BitmapFont();
		position = new Vector2();
		enemyBarWidth = resolution.x * 0.2f;
		menuOpen = false;
		focusedButton = null;

		inventoryStage = new Stage(new ScalingViewport(Scaling.fit,
				this.resolution.x, this.resolution.y, new OrthographicCamera(
						this.resolution.x, this.resolution.y)), batch);
		statusDisplay = new Table();
		setupStatusDisplay();
	}

	public void render(boolean paused) {
		batch.begin();
		float sw = Gdx.graphics.getWidth();
		float sh = Gdx.graphics.getHeight();
		float ht = sh * 0.05f;
		float wd = sw * 0.5f;

		boolean outerBreak = false;
		if (refreshMouse) {
			for (int i = 0; i < inputHierarchy.size; i++) {
				Array<EquipmentButton> row = inputHierarchy.get(i);
				for (EquipmentButton btn : row) {
					if (btn.isOver()) {
						if (!btn.isDisabled()) {
							focusedRow = i;
							updateFocusedButton(btn);
						}
						outerBreak = true;
						break;
					}
				}
				if (outerBreak) {
					break;
				}
			}
			refreshMouse = false;
		}

		barPatch.getPatch().setColor(Color.RED);
		if (player.hp > 0) {
			barPatch.draw(batch, 7, sh - (ht + 5) + 2, (wd - 4)
					* (player.hp / player.maxHp), (ht - 8));
		}
		barBorderPatch.draw(batch, 5, sh - (ht + 5), wd, ht);
		barPatch.getPatch().setColor(Color.GREEN);
		wd = sw * 0.25f;
		ht *= 0.75f;
		if (player.sp > 0) {
			barPatch.draw(batch, 7, sh - (2.667f * ht) + 2, (wd - 4)
					* (player.sp / player.maxSp), ht - 8);
		}
		barBorderPatch.draw(batch, 5, sh - (2.667f * ht), wd, ht);

		for (Enemy enemy : enemies) {
			if (enemy.alive() && enemy.hp < enemy.maxHp) {
				enemy.getScreenPosition(position);
				position.x -= (enemyBarWidth * 0.5f);
				position.y -= (enemyBarWidth * 0.25f);
				barPatch.getPatch().setColor(Color.DARK_GRAY);
				barPatch.draw(batch, position.x, position.y, enemyBarWidth,
						enemyBarWidth * 0.05f);
				barPatch.getPatch().setColor(Color.RED);
				barPatch.draw(batch, position.x, position.y, enemyBarWidth
						* ((float) enemy.hp / (float) enemy.maxHp),
						enemyBarWidth * 0.05f);
				if (enemy.getRecentDamage() > 0) {
					font.draw(batch, String.valueOf(enemy.getRecentDamage()),
							position.x, position.y);
				}
			}
		}
		batch.end();
		if (menuOpen) {
			inventoryStage.act(Gdx.graphics.getDeltaTime());
			inventoryStage.draw();
		}
	}

	public void setMenuOpen(boolean val) {
		menuOpen = val;
		if (val) {
			// float btnPad = invWindow.getPadLeft() * 0.1f;
			invWindow.clear();
			inputHierarchy.clear();
			float contentWidth = invWindow.getWidth()
					- (invWindow.getPadLeft() + invWindow.getPadRight());

			EquipmentTable<Weapon> weaponTable = new EquipmentTable<Weapon>(
					this, player.inventory.weaponEquip, Inventory.WEAPON_SLOTS,
					player.inventory.getWeapons(), uiAtlas, barBorderPatch,
					contentWidth, skin);

			// focusedButton = (Button) ((Table) ((Group)
			// weaponTable.getChildren().first()).getChildren().first()).getChildren().first();

			EquipmentTable<Shield> shieldTable = new EquipmentTable<Shield>(
					this, player.inventory.shieldEquip, Inventory.WEAPON_SLOTS,
					player.inventory.getShields(), uiAtlas, barBorderPatch,
					contentWidth, skin);

			EquipmentTable<Armor> armorTable = new EquipmentTable<Armor>(this,
					player.inventory.helm, 1, player.inventory.getHelms(),
					uiAtlas, barBorderPatch, contentWidth * 0.5f, skin);
			EquipmentTable<Armor> torsoTable = new EquipmentTable<Armor>(this,
					player.inventory.armor, 1, player.inventory.getArmors(),
					uiAtlas, barBorderPatch, contentWidth * 0.5f, skin);
			EquipmentTable<Armor> armTable = new EquipmentTable<Armor>(this,
					player.inventory.arms, 1, player.inventory.getGauntlets(),
					uiAtlas, barBorderPatch, contentWidth * 0.5f, skin);
			EquipmentTable<Armor> legTable = new EquipmentTable<Armor>(this,
					player.inventory.legs, 1, player.inventory.getLeggings(),
					uiAtlas, barBorderPatch, contentWidth * 0.5f, skin);
			inputHierarchy.add(weaponTable.row);
			inputHierarchy.add(legTable.row);
			inputHierarchy.add(armTable.row);
			inputHierarchy.add(torsoTable.row);
			inputHierarchy.add(armorTable.row);
			inputHierarchy.add(shieldTable.row);

			focusedRow = 0;
			updateFocusedButton((EquipmentButton) inputHierarchy.first()
					.first());

			/*
			 * Table consumablesTable = new Table(); consumablesTable.left();
			 * consumablesTable.setWidth(invWindow.getWidth()); for (Consumable
			 * w : player.inventory.getConsumables()) { consumablesTable.add(new
			 * Label(w.getName(), skin)); }
			 * consumablesTable.setBackground(barBorderPatch);
			 */

			ScrollPane weaponPane = new ScrollPane(weaponTable);
			ScrollPane armorPane = new ScrollPane(armorTable);
			armorTable.left();
			ScrollPane torsoPane = new ScrollPane(torsoTable);
			torsoTable.left();
			ScrollPane armPane = new ScrollPane(armTable);
			armTable.left();
			ScrollPane legPane = new ScrollPane(legTable);
			legTable.left();
			ScrollPane shieldPane = new ScrollPane(shieldTable);
			// ScrollPane consumablesPane = new ScrollPane(consumablesTable);
			invWindow.add(weaponPane).width(contentWidth);
			invWindow.row();
			invWindow.add(shieldPane).width(contentWidth);
			invWindow.row();
			invWindow.add(armorPane).width(contentWidth);
			invWindow.row();
			invWindow.add(torsoPane).width(contentWidth);
			invWindow.row();
			invWindow.add(armPane).width(contentWidth);
			invWindow.row();
			invWindow.add(legPane).width(contentWidth);
			/*
			 * invWindow.row();
			 * invWindow.add(consumablesPane).width(contentWidth);
			 */
			invWindow.row();
			invWindow.add(statusDisplay).width(contentWidth);
		} else {
			closeContextMenu();
		}
	}

	public void toggleMenu() {
		setMenuOpen(!menuOpen);
		Gdx.input.setCursorCatched(!menuOpen);
	}

	public void resize(int width, int height) {
		// inventoryStage.getViewport().update(width, height, true);
		float padding = resolution.y * 0.1f;
		// Gdx.input.setInputProcessor(stage);

		inventoryStage.clear();
		invWindow = new Window(invBundle.get("inventory.title"), skin);
		Window window = invWindow;
		window.setBackground(windowPatch);
		window.setWidth(resolution.x - (padding * 2));
		window.setHeight(resolution.y - (padding * 2));
		window.setPosition(padding, padding);
		window.pad(padding * 0.5f);
		inventoryStage.addActor(window);
		inventoryStage.addActor(contextWindow);
	}

	public void dispose() {
		inventoryStage.dispose();
	}

	public void updateFocusedButton(EquipmentButton btn) {
		if (focusedButton != null) {
			focusedButton.setFocus(false);
		}
		focusedButton = btn;
		focusedButton.setFocus(true);

		String labelText = btn.equipment != null ? btn.equipment.getName()
				: " ";
		nameLabel.setText(labelText);
	}

	public void openContextMenu(EquipmentButton button) {
		float x = button.getRight();
		float y = button.getY();
		tmpVec.set(0, 0);
		button.getParent().localToAscendantCoordinates(null, tmpVec);
		tmpVec.add(x, y);
		contextWindow.setPosition(tmpVec.x, tmpVec.y);
		contextWindow.setVisible(true);
		contextWindow.setModal(true);

		if (button.equipment.isUsable()) {
			modalButtons.get(0).setDisabled(false);
			modalFocus = 0;
		} else {
			modalButtons.get(0).setDisabled(true);
			modalFocus = 1;
		}
		contextMenuOpen = true;
	}

	public void closeContextMenu() {
		contextWindow.setVisible(false);
		contextMenuOpen = false;
	}

	public void setupInput(InputMultiplexer inpt) {
		inpt.addProcessor(inventoryStage);
	}
	
	public void lockFocusToCurrentRow(int equipSize, boolean equipBoxes) {
		for(int i = 0, n = inputHierarchy.size; i < n; i++) {
			boolean activeRow = (i == focusedRow);
			Array<EquipmentButton> row = inputHierarchy.get(i);
			for(int j = 0, m = row.size; j < m; j++) {
				EquipmentButton btn = row.get(j);
				boolean activeBox = equipBoxes ? (j < equipSize) : (j >= equipSize);
				btn.setDisabled(!activeRow || !activeBox);
			}
		}
	}
	
	public void clearFocus() {
		
	}
	
	public void enableAll() {
		for (Array<EquipmentButton> row : inputHierarchy) {
			for(EquipmentButton btn : row) {
				btn.setDisabled(false);
				btn.setLockedFocus(false);
				btn.setFocus(false);
			}
		}
		focusedButton.setFocus(true);
	}
	
	public void unequip(EquipmentButton target) {
		int equipIndex = 0;
		// Get index of equip change
		Array<EquipmentButton> focusRow = inputHierarchy.get(focusedRow);
		for(int i = 0; i < focusRow.size; i++) {
			if(focusRow.get(i) == target) {
				equipIndex = i;
			}
		}
		
		// if this is the active item, we need to unequip it on the player
		if(target.equipment instanceof Armor) {
			player.unequipArmor((Armor) target.equipment);
		} else if(target.equipment instanceof Weapon) {
			player.unequipWeapon(equipIndex);
		} else if(target.equipment instanceof Shield) {
			player.unequipShield(equipIndex);
		}
		
		target.setEquipment(null, uiAtlas);
		target.equipSlots[equipIndex] = null;
	}
	
	public void equip(EquipmentButton source, EquipmentButton target) {
		// target should be an equipment slot, so its equipSlots will be null
		// we assume focus is already locked to correct row
		if(source == target) return;
		int equipIndex = 0;
		// Get index of equip change
		Array<EquipmentButton> focusRow = inputHierarchy.get(focusedRow);
		for(int i = 0; i < source.equipSlots.length; i++) {
			EquipmentButton btn = focusRow.get(i);
			if(btn == target) {
				equipIndex = i;
			} else if(btn.equipment == source.equipment) {
				// item equipped in another slot, so remove it
				btn.setEquipment(null, uiAtlas);
			}
			
			Equipment equip = source.equipSlots[i];
			if(equip == source.equipment) {
				source.equipSlots[i] = null;
			}
		}
		
		target.setEquipment(source.equipment, uiAtlas);
		source.equipSlots[equipIndex] = source.equipment;
		//source.setChecked(true);
		enableAll();
		selectedButton = null;
		
		if(source.equipment instanceof Armor) {
			player.equipArmor((Armor) source.equipment); 
		} else if(source.equipment instanceof Weapon) {
			player.equipWeapon((Weapon) source.equipment, equipIndex);
		} else if(source.equipment instanceof Shield) {
			player.equipShield((Shield) source.equipment, equipIndex);
		}
	}
	
	public void cancel() {
		if(contextMenuOpen) {
			closeContextMenu();
		} else if(selectedButton != null) {
			enableAll();
			selectedButton = null;
		}
	}

	private void setupStatusDisplay() {
		nameLabel = new Label("", skin);
		statusDisplay.setBackground(barBorderPatch);
		statusDisplay.left();
		statusDisplay.add(nameLabel);

		contextWindow = new Window(invBundle.get("inventory.modal.title"), skin);
		contextWindow.setBackground(windowPatch);
		contextWindow.setWidth(200);
		contextWindow.setHeight(200);
		contextWindow.setVisible(false);
		contextWindow.setMovable(false);

		Button useButton = new TextButton(invBundle.get("inventory.use"), skin);
		useButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				// TODO Auto-generated method stub

			}
		});
		Button equipButton = new TextButton(invBundle.get("inventory.equip"),
				skin);
		equipButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				focusedButton.setLockedFocus(true);
				selectedButton = focusedButton;
				lockFocusToCurrentRow(player.inventory.getEquipSize(focusedButton.equipment), true);
				closeContextMenu();
			}
		});

		Button dropButton = new TextButton(invBundle.get("inventory.drop"),
				skin);
		dropButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				// TODO Auto-generated method stub

			}
		});

		contextWindow.add(useButton).width(contextWindow.getWidth());
		contextWindow.row();
		contextWindow.add(equipButton).width(contextWindow.getWidth());
		contextWindow.row();
		contextWindow.add(dropButton).width(contextWindow.getWidth());

		modalButtons.add(useButton);
		modalButtons.add(equipButton);
		modalButtons.add(dropButton);
		modalFocus = 0;
	}
}
