package org.tbee.tecl;

import java.math.BigDecimal;
import java.util.List;

import org.tbee.tecl.TECLSchema.ValidationException;
import org.tbee.tecl.TECLSchema.Validator;

public class ValidatorMinMax implements Validator {

	private static final String MAX = "max";
	private static final String MIN = "min";

	public void validate(TECL tecl, TECL schemaTECL, int schemaPropertyIdx, String schemaPropertyId, TECLSchema teclSchema) {

		// Is either column present?
		String schemaMinStr = schemaTECL.str(schemaPropertyIdx, MIN);
		String schemaMaxStr = schemaTECL.str(schemaPropertyIdx, MAX);
		if (schemaMinStr == null && schemaMaxStr == null) {
			return;
		}
		BigDecimal schemaMin = (schemaMinStr == null ? null : new BigDecimal(schemaMinStr));
		BigDecimal schemaMax = (schemaMaxStr == null ? null : new BigDecimal(schemaMaxStr));

		// Get the actual values
		List<String> values = tecl.strs(schemaPropertyId);
		for (int idx = 0; idx < values.size(); idx++) {
			String valueStr = values.get(idx);
			
			// convert value
			BigDecimal value = new BigDecimal(valueStr);
			
			// check min
			if (schemaMin != null && schemaMin.compareTo(value) > 0) {
				throw new ValidationException("'" + schemaPropertyId + "' should be equal or greater than " + schemaMin + " at " + tecl.createFullPathToKey(idx, schemaPropertyId));
			}
			
			// check max
			if (schemaMax != null && schemaMax.compareTo(value) < 0) {
				throw new ValidationException("'" + schemaPropertyId + "' should be equal or less than " + schemaMax + " at " + tecl.createFullPathToKey(idx, schemaPropertyId));
			}
		}
	}
}
