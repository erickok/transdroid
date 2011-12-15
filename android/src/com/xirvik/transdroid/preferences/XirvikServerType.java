package com.xirvik.transdroid.preferences;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum XirvikServerType {

	Dedicated (1),
	SemiDedicated (2),
	Shared (3);

	private int code;
    private static final Map<Integer,XirvikServerType> lookup  = new HashMap<Integer,XirvikServerType>();

	static {
	    for(XirvikServerType s : EnumSet.allOf(XirvikServerType.class))
	         lookup.put(s.getCode(), s);
	}

	XirvikServerType(int code) {
		this.code = code;
	}
	
	public int getCode() {
		return code;
	}
	
	public static XirvikServerType getStatus(int code) {
		return lookup.get(code);
	}

	/**
	 * Returns the type of xirvik server
	 * @param code A string with the code, similar to that used in arrays.xml 
	 * @return The xirvik server type; or null if the code was null or empty
	 */
	public static XirvikServerType fromCode(String code) {
		if (code == null) {
			return null;
		}
		if (code.equals("type_dedicated")) {
			return Dedicated;
		}
		if (code.equals("type_semi")) {
			return SemiDedicated;
		}
		if (code.equals("type_shared")) {
			return Shared;
		}
		return null;
	}
}
