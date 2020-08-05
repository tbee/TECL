package org.tbee.tecl;

import java.util.function.BiFunction;

import org.tbee.tecl.TECLSchema.ValidationException;
import org.tbee.tecl.TECLSchema.Validator;

public class ValidatorPropertyType implements Validator {

	public void validate(TECL tecl, TECL schemaTECL, int schemaPropertyIdx, String schemaPropertyId, TECLSchema teclSchema) {

		// type
		String schemaType = schemaTECL.str(schemaPropertyIdx, "type");
		if (schemaType == null || "group".equals(schemaType)) {
			return;
		}
		
		// Determine the class for the type
		Class<?> typeClass = teclSchema.typeToClass.get(schemaType);
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
				Object result = convertFunction.apply(value, null);
			}
			catch (Exception e) {
				throw new ValidationException("Error validating value for " + tecl.createFullPathToKey(valueIdx, schemaPropertyId), e);
			}
		}
	}
}
