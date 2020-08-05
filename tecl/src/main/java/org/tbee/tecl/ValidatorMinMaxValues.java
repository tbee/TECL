package org.tbee.tecl;

import org.tbee.tecl.TECLSchema.ValidationException;
import org.tbee.tecl.TECLSchema.Validator;

public class ValidatorMinMaxValues implements Validator {

	public void validate(TECL tecl, TECL schemaTECL, int schemaPropertyIdx, String schemaPropertyId, TECLSchema teclSchema) {

		// Count number of times the value is present
		int cntValues = tecl.count(schemaPropertyId);
		
		// check minValues
		int schemaMinValues = schemaTECL.integer(schemaPropertyIdx, "minValues", 0);
		if (cntValues < schemaMinValues) {
			throw new ValidationException("'" + schemaPropertyId + "' should occur at least " + schemaMinValues + " times at " + tecl.createFullPathToKey(schemaMinValues, schemaPropertyId));
		}
		
		// check maxValues
		int schemaMaxValues = schemaTECL.integer(schemaPropertyIdx, "maxValues", Integer.MAX_VALUE);
		if (cntValues > schemaMaxValues) {
			throw new ValidationException("'" + schemaPropertyId + "' should occur at most " + schemaMaxValues + " times at " + tecl.createFullPathToKey(schemaMaxValues, schemaPropertyId));
		}
	}
}
