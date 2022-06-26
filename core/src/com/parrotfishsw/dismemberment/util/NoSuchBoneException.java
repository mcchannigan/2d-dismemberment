package com.parrotfishsw.dismemberment.util;

public class NoSuchBoneException extends Exception {
	private static final long serialVersionUID = -1416643438469333003L;

	public NoSuchBoneException(String name) {
		super("No bone with name found in skeleton: " + name);
	}
}
