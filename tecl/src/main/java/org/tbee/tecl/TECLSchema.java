package org.tbee.tecl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO:
 * - allowAdditionalProperties (default false): allow more properties than what is defined in the schema
 * - type group
 */
public class TECLSchema {
	
	private TECL schemaTECL;
	private List<Validator> validators = new ArrayList<>();

	
	/**
	 * 
	 * @param tesd
	 */
	public TECLSchema(String tesd) {
		validators.add(new ValidatorMinMaxValues());
		validators.add(new ValidatorPropertyType());
		schemaTECL = TECL.parser().parse(tesd);
	}

	/**
	 * Add a validator
	 * @param validator
	 */
	TECLSchema addValidator(Validator validator) {
		validators.add(validator);
		return this;
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
	public Map<String, Class<?>> typeToClass = new HashMap<>();


	/*
	 * 
	 */
	private void validate(TECL tecl, TECL schemaTECL) {
		List<String> processedKeys = new ArrayList<String>();
		
		// scan all properties in the schema
		int schemaNumberOfProperties = schemaTECL.count("id");
		for (int schemaPropertyIdx = 0; schemaPropertyIdx < schemaNumberOfProperties; schemaPropertyIdx++) {
			
			// get data
			String schemaPropertyId = schemaTECL.str(schemaPropertyIdx, "id");
			processedKeys.add(schemaPropertyId);
			
			for (Validator validator : validators) {
				validator.validate(tecl, schemaTECL, schemaPropertyIdx, schemaPropertyId, this);
			}
			
			// type
			String schemaType = schemaTECL.str(schemaPropertyIdx, "type");
			String schemaSubtype = schemaTECL.str(schemaPropertyIdx, "subtype");
			if ("group".equals(schemaType)) {
				validateGroup(tecl, schemaPropertyIdx, schemaPropertyId, schemaSubtype);
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
	
	public static interface Validator {
		
		/**
		 * 
		 * @param tecl the TECL that is validated
		 * @param schemaTECL the TECL holding the schema information for the validation; over its properties the next two parameters are interating
		 * @param schemaPropertyIdx the index of the property in the schemaTECL that is currently being validated
		 * @param schemaPropertyId the id of the property (at the index) in the schemaTECL that is currently being validated
		 * @param teclSchema the TECLSchema instance doing the validation
		 */
		void validate(TECL tecl, TECL schemaTECL, int schemaPropertyIdx, String schemaPropertyId, TECLSchema teclSchema);
	}
}
