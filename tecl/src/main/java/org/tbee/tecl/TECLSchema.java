package org.tbee.tecl;

/*-
 * #%L
 * TECL
 * %%
 * Copyright (C) 2020 Tom Eugelink
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.tbee.tecl.validator.ValidatorListType;
import org.tbee.tecl.validator.ValidatorMinMax;
import org.tbee.tecl.validator.ValidatorMinMaxLen;
import org.tbee.tecl.validator.ValidatorMinMaxValues;
import org.tbee.tecl.validator.ValidatorPropertyType;

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
		validators.add(new ValidatorListType());
		validators.add(new ValidatorMinMaxLen());
		validators.add(new ValidatorMinMax());
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
			
			// attributes
			String schemaAttributetype = schemaTECL.str(schemaPropertyIdx, "attr");
			TECL schemaAttrTECL = schemaTECL.grp(schemaAttributetype);
			TECL attrTECL = tecl.attr(schemaPropertyId);
			// if the key has attributes, then there must be an attribute schema
			if (!attrTECL.isEmpty() && schemaAttributetype == null) {
				throw new ValidationException("Attributes exist, but no schema for the attributes at " + tecl.createFullPathToKey(0, schemaPropertyId));				
			}
			try {
				validate(attrTECL, schemaAttrTECL);
			}
			catch (ValidationException e) {
				throw new ValidationException("Attributes fail to validate at " + tecl.createFullPathToKey(0, schemaPropertyId), e);
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
