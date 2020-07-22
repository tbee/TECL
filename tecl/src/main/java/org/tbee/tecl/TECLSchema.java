package org.tbee.tecl;

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
		
		// scan all properties in the schema
		int numberOfProperties = schemaTECL.count("id");
		for (int propertyIdx = 0; propertyIdx < numberOfProperties; propertyIdx++) {
			
			// get data
			String id = schemaTECL.str(propertyIdx, "id");
			
			// type
			String type = schemaTECL.str(propertyIdx, "type");
			if (type != null) {
				validateType(tecl, propertyIdx, id, type);
			}
			
			// min&maxValues
			int cntValues = tecl.count(id);
			int minValues = schemaTECL.integer(propertyIdx, "minValues", 0);
			if (cntValues < minValues) {
				throw new ValidationException("'" + id + "' should occur at least " + minValues + " times at " + tecl.createFullPathToKey(minValues, id));
			}
			int maxValues = schemaTECL.integer(propertyIdx, "maxValues", Integer.MAX_VALUE);
			if (cntValues > maxValues) {
				throw new ValidationException("'" + id + "' should occur at most " + maxValues + " times at " + tecl.createFullPathToKey(maxValues, id));
			}
		}
	}

	/*
	 * 
	 */
	private void validateType(TECL tecl, int idx, String id, String type) {
		
		// Determine the class for the type
		Class<?> typeClass = typeToClass.get(type);
		if (typeClass == null) {
			throw new ValidationException("Unknown type '" + type + "' for " + schemaTECL.createFullPathToKey(idx, id));
		}
		
		// Determine the converter function for the class
		BiFunction<String, ?, ?> convertFunction = tecl.convertFunction(typeClass);

		// process all values for the specified id
		List<String> values = tecl.strs(id);
		for (String value : values) {
			try {
				convertFunction.apply(value, null);
			}
			catch (Exception e) {
				throw new ValidationException("Error validating value for " + schemaTECL.createFullPathToKey(idx, id), e);
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
