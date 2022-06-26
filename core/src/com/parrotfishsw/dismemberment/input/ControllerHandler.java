package com.parrotfishsw.dismemberment.input;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.parrotfishsw.dismemberment.DsConstants;
import com.parrotfishsw.dismemberment.GameConfig;
import com.parrotfishsw.dismemberment.GameScreen;
import com.parrotfishsw.lumberjack.entities.Player;

public class ControllerHandler implements ControllerListener {
	Controller control1 = null;
	Controller control2 = null;

	GameScreen screen;

	Map<Integer, Boolean> axisDown = new HashMap<Integer, Boolean>();

	public ControllerHandler(GameScreen gs) {
		screen = gs;
		Array<Controller> controllers = Controllers.getControllers();
		if (controllers.size > 0) {
			control1 = controllers.get(0);
			if (controllers.size > 1) {
				control2 = controllers.get(1);
			}
		}
	}

	@Override
	public boolean axisMoved(Controller ctrl, int axis, float value) {
		Player player;
		if (ctrl == control1) {
			player = screen.player;
			if (!screen.isPaused()) {
				if (MathUtils.isZero(value, 0.1f)) {
					value = 0;
				}
				if (axis == GameConfig.GamepadControls.MOVESTICK) {
					player.setMovement(value);
				} else if (axis == GameConfig.GamepadControls.AIMSTICK) {
					player.torsoAngleToAdd = (value * DsConstants.DEG_TO_RAD * GameConfig.KeyControls.Y_AXIS);
				} else {
					// TODO virtual button press for triggers
					int virtVal = GameConfig.GamepadControls
							.virtualButton(axis);
					if (value != 0) {
						Boolean down = axisDown.get(virtVal);
						if (down == null || !down) {
							axisDown.put(virtVal, true);
							buttonDown(ctrl, (value > 0 ? 1 : -1) * virtVal);
						}
					} else {
						axisDown.put(virtVal, false);
						axisDown.put(-virtVal, false);
						buttonUp(ctrl, virtVal);
						buttonUp(ctrl, -virtVal);
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean buttonDown(Controller ctrl, int button) {
		Player player = screen.player;
		if (button == GameConfig.GamepadControls.JUMP) {
			player.jump();
		} else if (button == GameConfig.GamepadControls.CROUCH) {
			if (player.isCrouched()) {
				player.uncrouch();
			} else {
				player.crouch();
			}
		} else if (button == GameConfig.GamepadControls.LOCK) {
			player.locked = !player.locked;
		} else if (button == GameConfig.GamepadControls.BACKSTEP) {
			player.backstep();
		} else if (button == GameConfig.GamepadControls.PAUSE) {
			screen.pause();
		} else if (button == GameConfig.GamepadControls.MENU) {
			screen.toggleMenu();
		} else if (button == GameConfig.GamepadControls.LIGHT_ATTACK) {
			player.lightAttack();
		} else if (button == GameConfig.GamepadControls.HEAVY_ATTACK) {
			player.heavyAttack();
		} else if (button == GameConfig.GamepadControls.BLOCK) {
			player.block();
		}
		return false;
	}

	@Override
	public boolean buttonUp(Controller ctrl, int button) {
		Player player = screen.player;
		if (button == GameConfig.GamepadControls.BLOCK) {
			player.stopBlocking();
		}
		return false;
	}

	@Override
	public void connected(Controller ctrl) {
		if (control1 == null || control1.equals(ctrl)) {
			control1 = ctrl;
		} else if (control2 == null || control2.equals(ctrl)) {
			control2 = ctrl;
		} else {
			// ignore controller
		}
	}

	@Override
	public void disconnected(Controller ctrl) {
		if (ctrl == control1) {
			// display reconnect notice
		}
	}

}
