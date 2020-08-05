package org.tbee.tecl;

import org.tbee.tecl.TECLSchema.ValidationException;
import org.tbee.tecl.TECLSchema.Validator;

public class ValidatorMinMaxValues implements Validator {

	private static final String MAX_VALUES = "maxValues";
	private static final String MIN_VALUES = "minValues";

	public void validate(TECL tecl, TECL schemaTECL, int schemaPropertyIdx, String schemaPropertyId, TECLSchema teclSchema) {

		// Count number of times the value is present
		int cntValues = tecl.count(schemaPropertyId);
		
		// check minValues
		int schemaMinValues = schemaTECL.integer(schemaPropertyIdx, MIN_VALUES, 0);
		if (cntValues < schemaMinValues) {
			throw new ValidationException("'" + schemaPropertyId + "' should occur at least " + schemaMinValues + " times at " + tecl.createFullPathToKey(schemaMinValues, schemaPropertyId));
		}
		
		// check maxValues
		int schemaMaxValues = schemaTECL.integer(schemaPropertyIdx, MAX_VALUES, Integer.MAX_VALUE);
		if (cntValues > schemaMaxValues) {
			throw new ValidationException("'" + schemaPropertyId + "' should occur at most " + schemaMaxValues + " times at " + tecl.createFullPathToKey(schemaMaxValues, schemaPropertyId));
		}
	}
}
