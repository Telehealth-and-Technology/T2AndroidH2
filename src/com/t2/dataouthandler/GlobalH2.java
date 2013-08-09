package com.t2.dataouthandler;

public class GlobalH2 {

	public static final int CACHE_ERROR = -1;	
	public static final int CACHE_IDLE = 0;	
	public static final int CACHE_SENDING = 1;	
	public static final int CACHE_DELETING = 2;	
	public static final int CACHE_SENT = 3;	

	
	public static final String[] VALID_DATA_TYPES = new String[] {
		DataOutHandlerTags.STRUCTURE_TYPE_SENSOR_DATA, 
		DataOutHandlerTags.STRUCTURE_TYPE_HABIT, 
		DataOutHandlerTags.STRUCTURE_TYPE_CHECKIN};
	
	
	
}


