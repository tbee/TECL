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
