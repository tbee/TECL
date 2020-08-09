package org.tbee.tecl;

import java.util.List;
import java.util.function.BiFunction;

import org.tbee.tecl.TECLSchema.ValidationException;
import org.tbee.tecl.TECLSchema.Validator;

public class ValidatorListType implements Validator {

	private static final String TYPE = "type";
	private static final String SUBTYPE = "subtype";
	private static final String ENUM = "enum";

	public void validate(TECL tecl, TECL schemaTECL, int schemaPropertyIdx, String schemaPropertyId, TECLSchema teclSchema) {

		// If a enum is specified, fetch the enum values
		String schemaEnum = schemaTECL.str(schemaPropertyIdx, ENUM);
		List<String> enumValues = null; 
		if (schemaEnum != null) {
			enumValues = schemaTECL.strs(schemaEnum);
		}
		
		// type must be 'list'
		String schemaType = schemaTECL.str(schemaPropertyIdx, TYPE);
		if (!"list".equals(schemaType)) {
			return;
		}
		
		// Subtype has the actual type
		String schemaSubtype = schemaTECL.str(schemaPropertyIdx, SUBTYPE);
		if (schemaSubtype == null) {
			throw new ValidationException("Type list requires a subtype for " + schemaTECL.createFullPathToKey(schemaPropertyIdx, schemaPropertyId));
		}
		
		// Determine the class for the type
		Class<?> typeClass = teclSchema.typeToClass.get(schemaSubtype);
		if (typeClass == null) {
			throw new ValidationException("Unknown type '" + schemaSubtype + "' for " + schemaTECL.createFullPathToKey(schemaPropertyIdx, schemaPropertyId));
		}
		
		// Determine the converter function for the class
		BiFunction<String, ?, ?> convertFunction = tecl.convertFunction(typeClass);

		// process all values for the specified id
		int valuesCnt = tecl.count(schemaPropertyId);
		for (int valueIdx = 0; valueIdx < valuesCnt; valueIdx++) {
			String value = tecl.str(valueIdx, schemaPropertyId);
			try {
				Object result = convertFunction.apply(value, null);
			}
			catch (Exception e) {
				throw new ValidationException("Error validating value against type for " + tecl.createFullPathToKey(valueIdx, schemaPropertyId), e);
			}
			
			// validate against enum
			if (enumValues != null && !enumValues.contains(value)) {
				throw new ValidationException("Value '" + value + "' does not occur in the enum '"  + schemaEnum + "' for " + tecl.createFullPathToKey(valueIdx, schemaPropertyId));
			}
		}
	}
}
