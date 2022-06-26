package com.parrotfishsw.dismemberment.input;

import java.awt.Button;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;
import com.parrotfishsw.dismemberment.DsConstants;
import com.parrotfishsw.dismemberment.GameConfig;
import com.parrotfishsw.dismemberment.GameScreen;
import com.parrotfishsw.dismemberment.GameConfig.KeyControls;
import com.parrotfishsw.lumberjack.entities.Player;

public class GameInputHandler implements InputProcessor {
	GameScreen gameScreen = null;

	public GameInputHandler(GameScreen scr) {
		gameScreen = scr;
	}

	@Override
	public boolean keyDown(int keycode) {
		Player player = gameScreen.player;
		if (!gameScreen.isPaused()) {
			float camDiff = 0.1f;
			if (keycode == GameConfig.KeyControls.LEFT) {
				player.setMovement(-1);
			} else if (keycode == GameConfig.KeyControls.RIGHT) {
				player.setMovement(1);
			} else if (keycode == GameConfig.KeyControls.JUMP) {
				player.jump();
			} else if (keycode == GameConfig.KeyControls.CROUCH) {
				player.crouch();
			} else if (keycode == GameConfig.KeyControls.BLOCK) {
				player.block();
			} else if (keycode == GameConfig.KeyControls.PARRY) {
				player.parry();
			} else if (keycode == GameConfig.KeyControls.BACKSTEP) {
				player.backstep();
			} else if (keycode == GameConfig.KeyControls.AIM_UP) {
				player.addUpperBodyAngle(0.1f);
			} else if (keycode == GameConfig.KeyControls.AIM_DOWN) {
				player.addUpperBodyAngle(-0.1f);
			} else if (keycode == GameConfig.KeyControls.PAUSE) {
				gameScreen.pause();
			} else if (keycode == GameConfig.KeyControls.MENU) {
				gameScreen.toggleMenu();
			} else if (keycode == GameConfig.KeyControls.WALK) {
				player.setWalk(true);
			} else if (keycode == GameConfig.KeyControls.LIGHT_ATTACK) {
				player.lightAttack();
			} else if (keycode == GameConfig.KeyControls.HEAVY_ATTACK) {
				player.heavyAttack();
			} else if (keycode == GameConfig.KeyControls.TOGGLE_WEAPON) {
				player.toggleWeapon();
			} else if (keycode == GameConfig.KeyControls.TOGGLE_SHIELD) {
				player.toggleShield();
			} else if (keycode == Keys.UP) {
				gameScreen.lightDiff.y = camDiff;
			} else if (keycode == Keys.DOWN) {
				gameScreen.lightDiff.y = -camDiff;
			} else if (keycode == Keys.LEFT) {
				gameScreen.lightDiff.x = -camDiff;
			} else if (keycode == Keys.RIGHT) {
				gameScreen.lightDiff.x = camDiff;
			} else if (keycode == Keys.PLUS) {
				gameScreen.lightDiff.z = (camDiff * 0.1f);
			} else if (keycode == Keys.MINUS) {
				gameScreen.lightDiff.z = -(camDiff * 0.1f);
			} else if (keycode == Keys.F1) {
				GameConfig.Video.debug = !GameConfig.Video.debug;
			}
		} else {
			if (keycode == GameConfig.KeyControls.PAUSE) {
				gameScreen.resume();
			}
		}
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		if (!gameScreen.isPaused()) {
			Player player = gameScreen.player;
			if (keycode == GameConfig.KeyControls.LEFT) {
				player.setMovement(0);
			} else if (keycode == GameConfig.KeyControls.RIGHT) {
				player.setMovement(0);
			} else if (keycode == GameConfig.KeyControls.CROUCH) {
				player.uncrouch();
			} else if (keycode == GameConfig.KeyControls.BLOCK) {
				player.stopBlocking();
			} else if (keycode == GameConfig.KeyControls.AIM_UP
					|| keycode == GameConfig.KeyControls.AIM_DOWN) {
				// player.rotateSaw(0);
			} else if (keycode == GameConfig.KeyControls.WALK) {
				player.setWalk(false);
			} else if (keycode == Keys.UP) {
				gameScreen.lightDiff.y = 0;
			} else if (keycode == Keys.DOWN) {
				gameScreen.lightDiff.y = 0;
			} else if (keycode == Keys.LEFT) {
				gameScreen.lightDiff.x = 0;
			} else if (keycode == Keys.RIGHT) {
				gameScreen.lightDiff.x = 0;
			} else if (keycode == Keys.PLUS) {
				gameScreen.lightDiff.z = 0f;
			} else if (keycode == Keys.MINUS) {
				gameScreen.lightDiff.z = 0;
			}
		}
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
		if (Gdx.input.isCursorCatched()) {
			if (button == Buttons.LEFT) {
				gameScreen.player.lightAttack();
			} else if (button == Buttons.RIGHT) {
				gameScreen.player.heavyAttack();
			} else if (button == Buttons.MIDDLE) {
				gameScreen.player.locked = !gameScreen.player.locked;
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		int dy = screenY - ((int) gameScreen.center.y);
		if (Gdx.input.isCursorCatched()) {
			gameScreen.player.addUpperBodyAngle(dy
					* DsConstants.DEG_TO_RAD * GameConfig.KeyControls.Y_AXIS);
			Gdx.input.setCursorPosition((int) gameScreen.center.x, (int) gameScreen.center.y);
			return true;
		}
		return false;
	}

	@Override
	public boolean scrolled(float amountX, float amountY) {
		return false;
	}

}
