package org.transdroid.daemon;

public enum OS {

	Windows {
		@Override public String getPathSeperator() { return "\\"; }
	},
	Mac {
		@Override public String getPathSeperator() { return "/"; }
	},
	Linux {
		@Override public String getPathSeperator() { return "/"; }
	};
	
	public static OS fromCode(String osCode) {
		if (osCode == null) {
			return null;
		}
		if (osCode.equals("type_windows")) {
			return Windows;
		}
		if (osCode.equals("type_mac")) {
			return Mac;
		}
		if (osCode.equals("type_linux")) {
			return Linux;
		}
		return null;
	}

	public abstract String getPathSeperator();

}
