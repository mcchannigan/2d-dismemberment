package com.parrotfishsw.dismemberment;

import com.badlogic.gdx.Input.Keys;
import com.parrotfishsw.dismemberment.input.Xbox360Pad;

/**
 * Stores and handles interactions with game configuration and user settings
 * @author Kyle
 *
 */
public class GameConfig {
	public static class KeyControls {
		public static int LEFT = Keys.A;
		public static int RIGHT = Keys.D;
		public static int AIM_UP = Keys.I;
		public static int AIM_DOWN = Keys.K;
		public static int UP = Keys.W;
		public static int CROUCH = Keys.S;
		public static int BLOCK = Keys.Q;
		public static int PARRY = Keys.E;
		public static int JUMP = Keys.SPACE;
		public static int BACKSTEP = Keys.C;
		public static int SLICE = Keys.TAB;
		public static int WALK = Keys.SHIFT_LEFT;
		public static int LIGHT_ATTACK = Keys.U;
		public static int HEAVY_ATTACK = Keys.O;
		
		public static int MENU_UP = Keys.UP;
		public static int MENU_DOWN = Keys.DOWN;
		public static int MENU_LEFT = Keys.LEFT;
		public static int MENU_RIGHT = Keys.RIGHT;
		public static int MENU_CONFIRM = Keys.ENTER;
		public static int MENU_CANCEL = Keys.BACKSPACE;
		
		public static int TOGGLE_WEAPON = Keys.G;
		public static int TOGGLE_SHIELD = Keys.F;
		
		public static int PAUSE = Keys.ESCAPE;
		public static int MENU = Keys.GRAVE;
		
		public static float SENSITIVITY = 1;
		public static int Y_AXIS = -1;
	}
	
	public static class GamepadControls {
		public static int MOVESTICK = Xbox360Pad.AXIS_LEFT_X;
		public static int AIMSTICK = Xbox360Pad.AXIS_RIGHT_Y;
		public static int BLOCK = Xbox360Pad.BUTTON_LB;//virtualButton(Xbox360Pad.AXIS_LEFT_TRIGGER);
		public static int JUMP = Xbox360Pad.BUTTON_A;
		public static int BACKSTEP = Xbox360Pad.BUTTON_B;
		public static int CROUCH = Xbox360Pad.BUTTON_L3;
		public static int LOCK = Xbox360Pad.BUTTON_R3;
		public static int LIGHT_ATTACK = Xbox360Pad.BUTTON_RB;
		public static int HEAVY_ATTACK = -virtualButton(Xbox360Pad.AXIS_RIGHT_TRIGGER);
		
		public static int PAUSE = Xbox360Pad.BUTTON_START;
		public static int MENU = Xbox360Pad.BUTTON_BACK;
		
		public static int virtualButton(int axis) {
			return 100 + axis;
		}


	}
	
	public static class Video {
		public static int width = 1280;
		public static int height = 720;
		public static boolean fullscreen = false;
		public static boolean debug = true;
	}
}
