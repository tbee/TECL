package org.tbee.tecl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * TODO:
 * - allowAdditionalProperties (default false): allow more properties than what is defined in the schema
 * - type group
 */
public class TECLSchema {
	
	private TECL schemaTECL;

	/**
	 * 
	 * @param tesd
	 */
	public TECLSchema(String tesd) {
		schemaTECL = TECL.parser().parse(tesd);
	}

	/**
	 * Throws ValidationException when errors are found
	 * @param tecl
	 */
	public void validate(TECL tecl) {
		
		// construct the convertFunctions mapping
		for (Class<?> clazz : TECL.buildinConvertFunctions.keySet()) {
			typeToClass.put(clazz.getSimpleName(), clazz);
		}
		for (Class<?> clazz : tecl.convertFunctions.keySet()) {
			if (!typeToClass.values().contains(clazz)) {
				typeToClass.put(clazz.getSimpleName(), clazz);
			}
		}
		
		// now validate
		validate(tecl, schemaTECL);
	}
	private Map<String, Class<?>> typeToClass = new HashMap<>();


	/*
	 * 
	 */
	private void validate(TECL tecl, TECL schemaTECL) {
		List<String> processedKeys = new ArrayList<String>();
		
		// scan all properties in the schema
		int schemaNumberOfProperties = schemaTECL.count("id");
		for (int schemaPropertyIdx = 0; schemaPropertyIdx < schemaNumberOfProperties; schemaPropertyIdx++) {
			
			// get data
			String schemaId = schemaTECL.str(schemaPropertyIdx, "id");
			processedKeys.add(schemaId);
			
			// min&maxValues
			int cntValues = tecl.count(schemaId);
			int schemaMinValues = schemaTECL.integer(schemaPropertyIdx, "minValues", 0);
			if (cntValues < schemaMinValues) {
				throw new ValidationException("'" + schemaId + "' should occur at least " + schemaMinValues + " times at " + tecl.createFullPathToKey(schemaMinValues, schemaId));
			}
			int schemaMaxValues = schemaTECL.integer(schemaPropertyIdx, "maxValues", Integer.MAX_VALUE);
			if (cntValues > schemaMaxValues) {
				throw new ValidationException("'" + schemaId + "' should occur at most " + schemaMaxValues + " times at " + tecl.createFullPathToKey(schemaMaxValues, schemaId));
			}
			
			// type
			String schemaType = schemaTECL.str(schemaPropertyIdx, "type");
			String schemaSubtype = schemaTECL.str(schemaPropertyIdx, "subtype");
			if (schemaType != null) {
				if ("group".contentEquals(schemaType)) {
					validateGroup(tecl, schemaPropertyIdx, schemaId, schemaSubtype);
				}
				else {
					validatePropertyType(tecl, schemaPropertyIdx, schemaId, schemaType);
				}
			}
		}
		
		// Check for undefined keys
		List<String> undefinedPropertyKeys = tecl.properties.getKeys();
		undefinedPropertyKeys.addAll(tecl.groups.getKeys());
		undefinedPropertyKeys.removeAll(processedKeys);
		if (!undefinedPropertyKeys.isEmpty()) {
			String propertyKey = undefinedPropertyKeys.get(0);
			throw new ValidationException("'" + propertyKey + "' is not defined in the schema at " + tecl.createFullPathToKey(0, propertyKey));
		}
	}

	private void validateGroup(TECL tecl, int schemaPropertyIdx, String schemaGroupId, String schemaGroupName) {
		
		// get schema for group
		TECL groupSchemaTECL = schemaTECL.grp(schemaGroupName);
		if (groupSchemaTECL == null) {
			throw new ValidationException("Group '" + schemaGroupName + "' is not defined in schema " + schemaTECL.createFullPathToKey(schemaPropertyIdx, schemaGroupId));			
		}
		
		// Scan the groups
		List<TECL> groups = tecl.grps(schemaGroupId);
		for (TECL group : groups) {
			validate(group, groupSchemaTECL);
		}
	}

	/*
	 * 
	 */
	private void validatePropertyType(TECL tecl, int schemaPropertyIdx, String schemaPropertyId, String schemaType) {

		// Determine the class for the type
		Class<?> typeClass = typeToClass.get(schemaType);
		if (typeClass == null) {
			throw new ValidationException("Unknown type '" + schemaType + "' for " + schemaTECL.createFullPathToKey(schemaPropertyIdx, schemaPropertyId));
		}
		
		// Determine the converter function for the class
		BiFunction<String, ?, ?> convertFunction = tecl.convertFunction(typeClass);

		// process all values for the specified id
		int valuesCnt = tecl.count(schemaPropertyId);
		for (int valueIdx = 0; valueIdx < valuesCnt; valueIdx++) {
			String value = tecl.str(valueIdx, schemaPropertyId);
			try {
				convertFunction.apply(value, null);
			}
			catch (Exception e) {
				throw new ValidationException("Error validating value for " + tecl.createFullPathToKey(valueIdx, schemaPropertyId), e);
			}
		}
	}

	/**
	 * 
	 */
	public static class ValidationException extends RuntimeException {
		
		public ValidationException(String message, Exception e) {
			super(message, e);
		}

		public ValidationException(String message) {
			super(message);
		}
	}
}
