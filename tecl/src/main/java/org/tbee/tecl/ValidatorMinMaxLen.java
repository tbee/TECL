package org.tbee.tecl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.tbee.tecl.TECLSchema.ValidationException;
import org.tbee.tecl.TECLSchema.Validator;

public class ValidatorMinMaxLen implements Validator {

	private static final String TYPE = "type";
	private static final String MAX_LEN = "maxLen";
	private static final String MIN_LEN = "minLen";

	public void validate(TECL tecl, TECL schemaTECL, int schemaPropertyIdx, String schemaPropertyId, TECLSchema teclSchema) {

		// Is either column present?
		if (schemaTECL.str(schemaPropertyIdx, MIN_LEN) == null && schemaTECL.str(schemaPropertyIdx, MAX_LEN) == null) {
			return;
		}

		// Get the type		
		String schemaType = schemaTECL.str(schemaPropertyIdx, TYPE);
		if (schemaType == null) {
			throw new ValidationException("You cannot define min/maxLen without type on " + schemaTECL.createFullPathToKey(schemaPropertyIdx, schemaPropertyId));
		}
		Class<?> typeClass = teclSchema.typeToClass.get(schemaType);
		if (typeClass == null) {
			return;
		}
		
		// Find length method.
		Method method = schemaTypeToMethod.get(schemaType); // cache
		if (method == null) {
			
			try {
				method = typeClass.getDeclaredMethod("length", new Class<?>[0]);
			} 
			catch (NoSuchMethodException e1) {
				throw new ValidationException("Type '" + schemaType +"' does not have a length() method on " + schemaTECL.createFullPathToKey(schemaPropertyIdx, schemaPropertyId));
			} 
			catch (SecurityException e1) {
				throw new ValidationException("Error", e1);
			}
			if (!"int".equals(method.getReturnType().getName())) {
				throw new ValidationException("Type '" + schemaType +"' does not have a length() method returning int on " + schemaTECL.createFullPathToKey(schemaPropertyIdx, schemaPropertyId));
			}
			
			// remember
			schemaTypeToMethod.put(schemaType, method);
		}
		
		// Get the actual values
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
			int schemaMinLen = schemaTECL.integer(schemaPropertyIdx, MIN_LEN, 0);
			if (length < schemaMinLen) {
				throw new ValidationException("'" + schemaPropertyId + "' should be at least of length " + schemaMinLen + " at " + tecl.createFullPathToKey(idx, schemaPropertyId));
			}
			
			// check maxValues
			int schemaMaxLen = schemaTECL.integer(schemaPropertyIdx, MAX_LEN, Integer.MAX_VALUE);
			if (length > schemaMaxLen) {
				throw new ValidationException("'" + schemaPropertyId + "' should be no longer than " + schemaMaxLen + " at " + tecl.createFullPathToKey(idx, schemaPropertyId));
			}
		}
	}
	
	private Map<String, Method> schemaTypeToMethod = new HashMap<>();
}
