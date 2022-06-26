package com.parrotfishsw.dismemberment.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.parrotfishsw.dismemberment.DsConstants;
import com.parrotfishsw.dismemberment.HUDRenderer;
import com.parrotfishsw.dismemberment.item.Armor;
import com.parrotfishsw.dismemberment.item.Equipment;

public class EquipmentButton extends ImageButton {
	public Equipment equipment;
	public HUDRenderer renderer;
	/** will be null on buttons for equipped items */ 
	public Equipment[] equipSlots;
	boolean focused = false;
	boolean lockedFocus = false;
	public boolean equipSlot = false;

	public EquipmentButton(HUDRenderer r, Drawable imageUp, Equipment[] check, final boolean equipSlot,
			Equipment equipment) {
		super(imageUp);
		this.equipment = equipment;
		this.equipSlot = equipSlot;
		renderer = r;
		if (check != null) {
			equipSlots = check;
			for (Equipment e : check) {
				if (equipment == e) {
					//setChecked(true);
				}
			}
		}
		addListener(new ClickListener(1) {
			public void clicked (InputEvent event, float x, float y) {
				EquipmentButton button = (EquipmentButton) event.getListenerActor();
				if(equipSlot) {
					renderer.unequip(button);
				}
			}
		});
		addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				EquipmentButton button = (EquipmentButton) actor;
				// event.setBubbles(false);
				if (button.equipSlot) {
					// selected item from equipment list
					if (renderer.selectedButton == null) {
						button.setLockedFocus(true);
						renderer.lockFocusToCurrentRow(equipSlots.length, false);
						renderer.selectedButton = button;
					} else {
						// equip selected item to this slot
						renderer.equip(renderer.selectedButton, button);
					}
				} else {
					// selected item from inventory list
					if(renderer.selectedButton == null) {
						renderer.openContextMenu(button);
					} else {
						// equip this item to selected slot
						renderer.equip(button, renderer.selectedButton);
					}
				}
			}
		});
	}

	public void setFocus(boolean f) {
		if (!lockedFocus) {
			focused = f;
			if (f) {
				setColor(DsConstants.Colors.FOCUS);
			} else {
				setColor(Color.WHITE);
			}
		}
	}

	public void setDisabled(boolean isDisabled) {
		super.setDisabled(isDisabled);
		if (isDisabled) {
			getImage().setColor(DsConstants.Colors.GRAY);
		} else {
			getImage().setColor(Color.WHITE);
		}
	}

	public void setLockedFocus(boolean l) {
		lockedFocus = l;
	}
	
	public void setEquipment(Equipment e, TextureAtlas uiAtlas) {
		equipment = e;
		TextureRegionDrawable drawable = new TextureRegionDrawable(
				uiAtlas.findRegion(e == null ? "blank-thumb" : e.getUiImage()));
		this.getImage().setDrawable(drawable);
	}
}
