package org.tbee.tecl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.BiFunction;

import org.tbee.tecl.TECLSchema.ValidationException;
import org.tbee.tecl.TECLSchema.Validator;

public class ValidatorMinMaxLen implements Validator {

	public void validate(TECL tecl, TECL schemaTECL, int schemaPropertyIdx, String schemaPropertyId, TECLSchema teclSchema) {

		// check if its type has a length() method		
		String schemaType = schemaTECL.str(schemaPropertyIdx, "type");
		Class<?> typeClass = teclSchema.typeToClass.get(schemaType);
		if (typeClass == null) {
			return;
		}
		
		// Find length method. TODO: caching
		Method method;
		try {
			method = typeClass.getDeclaredMethod("length", new Class<?>[0]);
		} 
		catch (NoSuchMethodException e1) {
			return; // no action on purpose
		} 
		catch (SecurityException e1) {
			return; // no action on purpose
		}
		if (!"int".equals(method.getReturnType().getName())) {
			return;
		}
		
		// Get the actual value
		BiFunction<String, ?, ?> convertFunction = tecl.convertFunction(typeClass);
		List<String> values = tecl.strs(schemaPropertyId);
		for (int idx = 0; idx < values.size(); idx++) {
			String valueStr = values.get(idx);
			
			// Get value in type that has the length method
			Object value = convertFunction.apply(valueStr, null);
	
			// Invoke: call the length method to determine the length
			int length;
			try {
				length = (Integer)method.invoke(value, new Object[0]);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new ValidationException("Could not invoke length() method on " + tecl.createFullPathToKey(idx, schemaPropertyId));
			}
			
			// check minValues
			int schemaMinLen = schemaTECL.integer(schemaPropertyIdx, "minLen", 0);
			if (length < schemaMinLen) {
				throw new ValidationException("'" + schemaPropertyId + "' should be at least of length " + schemaMinLen + " at " + tecl.createFullPathToKey(idx, schemaPropertyId));
			}
			
			// check maxValues
			int schemaMaxLen = schemaTECL.integer(schemaPropertyIdx, "maxLen", Integer.MAX_VALUE);
			if (length > schemaMaxLen) {
				throw new ValidationException("'" + schemaPropertyId + "' should be no longer than " + schemaMaxLen + " at " + tecl.createFullPathToKey(idx, schemaPropertyId));
			}
		}
	}
}
