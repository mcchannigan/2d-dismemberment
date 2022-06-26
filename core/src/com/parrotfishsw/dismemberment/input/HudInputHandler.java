package com.parrotfishsw.dismemberment.input;

import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;
import com.parrotfishsw.dismemberment.GameConfig;
import com.parrotfishsw.dismemberment.GameScreen;
import com.parrotfishsw.dismemberment.HUDRenderer;
import com.parrotfishsw.dismemberment.ui.EquipmentButton;

/**
 * Processes input events on the HUD during gameplay. Meant to be used in
 * conjunction with a GameInputHandler as part of a multiplexer.
 * 
 * @author Kyle
 * 
 */
public class HudInputHandler implements InputProcessor {

	HUDRenderer renderer;
	GameScreen screen;

	public HudInputHandler(HUDRenderer ren, GameScreen screen) {
		renderer = ren;
		this.screen = screen;
	}

	@Override
	public boolean keyDown(int keycode) {
		if (renderer.menuOpen) {
			Array<EquipmentButton> row = renderer.inputHierarchy
					.get(renderer.focusedRow);
			int rows = renderer.inputHierarchy.size;
			if (keycode == GameConfig.KeyControls.MENU_RIGHT) {
				if (renderer.contextMenuOpen) {

				} else {
					for (int i = 0, n = row.size; i < n; i++) {
						Button btn = row.get(i);
						if (btn == renderer.focusedButton) {
							int j = 1;
							boolean nextFound = false;
							do {
								EquipmentButton next = row.get((i + j) % n);
								if (next == renderer.focusedButton) {
									// Looped all the way around so just break
									nextFound = true;
								} else if (!next.isDisabled()) {
									renderer.updateFocusedButton(next);
									nextFound = true;
								} else {
									// Button is disabled so continue looping
								}
								j++;
							} while (!nextFound);
							break;
						}
					}
				}
			} else if (keycode == GameConfig.KeyControls.MENU_LEFT) {
				if (renderer.contextMenuOpen) {

				} else {
					for (int i = 0, n = row.size; i < n; i++) {
						Button btn = row.get(i);
						if (btn == renderer.focusedButton) {
							int j = 1;
							boolean nextFound = false;
							do {
								EquipmentButton next = row.get(((i - j) + n)
										% n);
								if (next == renderer.focusedButton) {
									// Looped all the way around so just break
									nextFound = true;
								} else if (!next.isDisabled()) {
									renderer.updateFocusedButton(next);
									nextFound = true;
								} else {
									// Button is disabled so continue looping
								}
								j++;
							} while (!nextFound);
							break;
						}
					}
				}
			} else if (keycode == GameConfig.KeyControls.MENU_DOWN) {
				if (renderer.contextMenuOpen) {

				} else {
					boolean nextFound = false;
					int j = 1;
					do {
						int nextRow = ((renderer.focusedRow - j) + rows) % rows;
						if (nextRow == renderer.focusedRow) {
							// looped around, so do nothing
							nextFound = true;
						} else {
							for (EquipmentButton btn : renderer.inputHierarchy
									.get(nextRow)) {
								if (!btn.isDisabled()) {
									renderer.focusedRow = nextRow;
									renderer.updateFocusedButton(btn);
									nextFound = true;
									break;
								}
							}
						}
						j++;
					} while (!nextFound);
				}
			} else if (keycode == GameConfig.KeyControls.MENU_UP) {
				if (renderer.contextMenuOpen) {

				} else {
					boolean nextFound = false;
					int j = 1;
					do {
						int nextRow = ((renderer.focusedRow + j)) % rows;
						if (nextRow == renderer.focusedRow) {
							// looped around, so do nothing
							nextFound = true;
						} else {
							for (EquipmentButton btn : renderer.inputHierarchy
									.get(nextRow)) {
								if (!btn.isDisabled()) {
									renderer.focusedRow = nextRow;
									renderer.updateFocusedButton(btn);
									nextFound = true;
									break;
								}
							}
						}
						j++;
					} while (!nextFound);
				}
			} else if (keycode == GameConfig.KeyControls.MENU_CONFIRM) {
				if (renderer.contextMenuOpen) {

				} else {
					renderer.focusedButton
							.fire(Pools.obtain(ChangeEvent.class));
				}
			} else if (keycode == GameConfig.KeyControls.MENU_CANCEL) {
				renderer.cancel();
			}
		}
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		renderer.refreshMouse = true;
		return false;
	}

	@Override
	public boolean scrolled(float amountX, float amountY) {
		// TODO Auto-generated method stub
		return false;
	}

}
