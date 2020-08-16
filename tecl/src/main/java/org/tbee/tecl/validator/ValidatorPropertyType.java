package org.tbee.tecl.validator;

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

import java.util.List;
import java.util.function.BiFunction;

import org.tbee.tecl.TECL;
import org.tbee.tecl.TECLSchema;
import org.tbee.tecl.TECLSchema.ValidationException;
import org.tbee.tecl.TECLSchema.Validator;

public class ValidatorPropertyType implements Validator {

	private static final String TYPE = "type";
	private static final String ENUM = "enum";

	public void validate(TECL tecl, TECL schemaTECL, int schemaPropertyIdx, String schemaPropertyId, TECLSchema teclSchema) {
		
		// type
		String schemaType = schemaTECL.str(schemaPropertyIdx, TYPE);
		if (schemaType == null || "group".equals(schemaType) || "list".equals(schemaType)) { // TODO: better meta type detection?
			return;
		}
		
		// If a enum is specified, fetch the enum values
		String schemaEnum = schemaTECL.str(schemaPropertyIdx, ENUM);
		List<String> enumValues = null; 
		if (schemaEnum != null && !schemaEnum.isBlank()) {
			enumValues = schemaTECL.strs(schemaEnum);
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
				throw new ValidationException("Error validating value against type for " + tecl.createFullPathToKey(valueIdx, schemaPropertyId), e);
			}
			
			// validate against enum
			if (enumValues != null && !enumValues.contains(value)) {
				throw new ValidationException("Value '" + value + "' does not occur in the enum '"  + schemaEnum + "' for " + tecl.createFullPathToKey(valueIdx, schemaPropertyId));
			}
		}
	}
}
