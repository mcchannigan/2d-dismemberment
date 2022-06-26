package com.parrotfishsw.dismemberment.ui;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton.ImageButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.SplitPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.parrotfishsw.dismemberment.HUDRenderer;
import com.parrotfishsw.dismemberment.item.Equipment;

public class EquipmentTable<K extends Equipment> extends Table {
	private static final int rowHt = 112;
	
	public Array<EquipmentButton> row = new Array<EquipmentButton>();
	public EquipmentTable(HUDRenderer r, K[] equipSlots, int equipCount, Array<K> equip, TextureAtlas uiAtlas, NinePatchDrawable barBorderPatch, float contentWidth, Skin skin) {
		super();
		float splitAmt = 0.10f;
		float padding = 5f;
		row = new Array<EquipmentButton>();
		Table table = new Table();
		Table equipTable = new Table();
		
		equipTable.left();
		equipTable.top();
		
		SplitPane weaponSplitPane = new SplitPane(equipTable, table, false, skin);
		weaponSplitPane.setSplitAmount(splitAmt);
		weaponSplitPane.setTouchable(Touchable.childrenOnly);
		table.top();
		table.left();
		ButtonStyle style = new ButtonStyle(barBorderPatch, barBorderPatch,
				barBorderPatch);
		ImageButtonStyle iStyle = new ImageButtonStyle(style);
	
		int rowNum = 0;
		int rows = 1;
		for(int i = 0; i < equipCount; i++) {
			if(rowNum >= 2) {
				rowNum = 0;
				rows++;
				equipTable.row();
			}
			Equipment w = equipSlots[i];
			TextureRegionDrawable drawable = new TextureRegionDrawable(
					uiAtlas.findRegion(w == null ? "blank-thumb" : w.getUiImage()));
			EquipmentButton imgBtn = new EquipmentButton(r, drawable, equipSlots, true, w);
			imgBtn.setStyle(iStyle);
			imgBtn.pad(padding);
			equipTable.add(imgBtn);
			row.add(imgBtn);
			rowNum++;
		}
		
		for(K w : equip) {
			EquipmentButton imgBtn = new EquipmentButton(r, new TextureRegionDrawable(
					uiAtlas.findRegion(w.getUiImage())), equipSlots, false, w);
			imgBtn.setStyle(iStyle);
			imgBtn.pad(padding);
			table.add(imgBtn);
			row.add(imgBtn);
		}
		
		table.setHeight(rows * rowHt);
		equipTable.setHeight(table.getHeight());
		equipTable.padTop(12f);
		table.setBackground(barBorderPatch);
		//equipTable.setBackground(barBorderPatch);
		this.add(weaponSplitPane).width(contentWidth).height(table.getHeight());
	}
	
	private float calcBtnWidth(Button btn, float ht) {
		float ratio = btn.getWidth() / btn.getHeight();
		return ht * ratio;
	}
}
