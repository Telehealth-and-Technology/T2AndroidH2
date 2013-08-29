package com.t2.drupalsdk;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

public class DrupalUtils {

	public static void putDrupalFieldNode(String tag, String value, ObjectNode node) {
		String newTag = "field_" + tag.toLowerCase();
		ObjectNode valueNode = JsonNodeFactory.instance.objectNode();		
		ObjectNode undNode = JsonNodeFactory.instance.objectNode();		
		ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();		
		valueNode.put("value", value);
		arrayNode.add(valueNode);	
		undNode.put("und", arrayNode);			
		node.put(newTag, undNode);
	}		
	
	/**
	 * Writes Drupal formatted node to specified node (Long)
	 * 
	 * @param tag Data Tag
	 * @param value Data Value
	 * @param node Node to write to 
	 */
	public static void putDrupaFieldlNode(String tag, long value, ObjectNode node) {
		String newTag = "field_" + tag.toLowerCase();
		ObjectNode valueNode = JsonNodeFactory.instance.objectNode();		
		ObjectNode undNode = JsonNodeFactory.instance.objectNode();		
		ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();		
		valueNode.put("value", value);
		arrayNode.add(valueNode);	
		undNode.put("und", arrayNode);			
		node.put(newTag, undNode);
	}
	
	/**
	 * Writes Drupal formatted node to specified node (Int)
	 * 
	 * @param tag Data Tag
	 * @param value Data Value
	 * @param node Node to write to 
	 */
	public static void putDrupalFieldNode(String tag, int value, ObjectNode node) {
		String newTag = "field_" + tag.toLowerCase();
		ObjectNode valueNode = JsonNodeFactory.instance.objectNode();		
		ObjectNode undNode = JsonNodeFactory.instance.objectNode();		
		ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();		
		valueNode.put("value", value);
		arrayNode.add(valueNode);	
		undNode.put("und", arrayNode);			
		node.put(newTag, undNode);
	}
	
	/**
	 * Writes Drupal formatted node to specified node (Double)
	 * 
	 * @param tag Data Tag
	 * @param value Data Value
	 * @param node Node to write to 
	 */
	public static void putDrupalFieldNode(String tag, double value, ObjectNode node) {
		String newTag = "field_" + tag.toLowerCase();
		ObjectNode valueNode = JsonNodeFactory.instance.objectNode();		
		ObjectNode undNode = JsonNodeFactory.instance.objectNode();		
		ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();		
		valueNode.put("value", value);
		arrayNode.add(valueNode);	
		undNode.put("und", arrayNode);			
		node.put(newTag, undNode);
	}	
	
	
	public static void putDrupalCheckinFieldNode(String tag, String value, ObjectNode node) {
		String newTag = "field_" + tag.toLowerCase();
		ObjectNode dateNode = JsonNodeFactory.instance.objectNode();		
		ObjectNode valueNode = JsonNodeFactory.instance.objectNode();		
		ObjectNode undNode = JsonNodeFactory.instance.objectNode();		
		ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();		

		dateNode.put("date",  value);
		valueNode.put("value", dateNode);
		arrayNode.add(valueNode);	
		undNode.put("und", arrayNode);			

		node.put(newTag, undNode);
	}		

}
