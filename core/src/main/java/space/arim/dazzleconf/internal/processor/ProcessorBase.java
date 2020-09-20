/* 
 * DazzleConf-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * DazzleConf-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DazzleConf-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with DazzleConf-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Lesser General Public License.
 */
package space.arim.dazzleconf.internal.processor;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import space.arim.dazzleconf.ConfigurationOptions;
import space.arim.dazzleconf.annote.CollectionSize;
import space.arim.dazzleconf.annote.IntegerRange;
import space.arim.dazzleconf.annote.NumericRange;
import space.arim.dazzleconf.error.BadValueException;
import space.arim.dazzleconf.error.IllDefinedConfigException;
import space.arim.dazzleconf.error.ImproperEntryException;
import space.arim.dazzleconf.error.InvalidConfigException;
import space.arim.dazzleconf.error.MissingKeyException;
import space.arim.dazzleconf.error.MissingValueException;
import space.arim.dazzleconf.internal.ConfEntry;
import space.arim.dazzleconf.internal.ConfigurationDefinition;
import space.arim.dazzleconf.internal.DefaultMethodUtil;
import space.arim.dazzleconf.internal.ImmutableCollections;
import space.arim.dazzleconf.internal.NestedConfEntry;
import space.arim.dazzleconf.internal.SingleConfEntry;
import space.arim.dazzleconf.serialiser.ValueSerialiser;
import space.arim.dazzleconf.validator.ValueValidator;

public abstract class ProcessorBase {

	private final ConfigurationOptions options;
	private final List<ConfEntry> entries;
	
	private String key;
	
	private final Map<String, Object> result = new HashMap<>();
	
	/**
	 * Creates from options and entries
	 * 
	 * @param options the config options
	 * @param mainEntry the config entries
	 */
	ProcessorBase(ConfigurationOptions options, List<ConfEntry> entries) {
		this.options = options;
		this.entries = entries;
	}
	
	abstract ProcessorBase continueNested(ConfigurationOptions options, NestedConfEntry<?> childEntry) throws ImproperEntryException;
	
	/**
	 * Creates a configuration using the specified processor
	 * 
	 * @param <C> the type of the config class
	 * @param definition the config definition
	 * @param processor the processor
	 * @return an instance of the config class
	 * @throws InvalidConfigException if the input to the processor is invalid for the configuration
	 */
	public static <C> C createConfig(ConfigurationDefinition<C> definition, ProcessorBase processor) throws InvalidConfigException {
		processor.process();

		ConfigInvocationHandler handler;
		if (definition.hasDefaultMethods()) {
			handler = new DefaultMethodConfigInvocationHandler(processor.result);
		} else {
			handler = new ConfigInvocationHandler(processor.result);
		}
		Class<C> configClass = definition.getConfigClass();
		Object proxy = Proxy.newProxyInstance(configClass.getClassLoader(), new Class<?>[] {configClass}, handler);
		return configClass.cast(proxy);
	}
	
	private void process() throws InvalidConfigException {
		for (ConfEntry entry : entries) {
			Method method = entry.getMethod();
			if (DefaultMethodUtil.isDefault(method)) {
				continue;
			}
			key = entry.getKey();
			String methodName = method.getName();
			Object value = processEntry(entry);
			Object formerValue = result.put(methodName, value);
			if (formerValue != null) {
				throw new IllDefinedConfigException("Duplicate method name " + methodName);
			}
		}
	}
	
	private Object processEntry(ConfEntry entry) throws InvalidConfigException {
		if (entry instanceof NestedConfEntry) {
			NestedConfEntry<?> nestedEntry = (NestedConfEntry<?>) entry;
			return createConfig(nestedEntry.getDefinition(), continueNested(options, nestedEntry));
		}
		SingleConfEntry singleEntry = (SingleConfEntry) entry;
		Object preValue = getValueFromSources(entry);
		if (preValue == null) {
			throw MissingValueException.forKey(key);
		}
		Object value = processObjectAtEntryWithGoal(singleEntry, entry.getMethod().getReturnType(), preValue);

		ValueValidator validator = singleEntry.getValidator();
		if (validator == null) {
			validator = options.getValidators().get(key);
		}
		if (validator != null) {
			validator.validate(key, value);
		}
		return value;
	}
	
	private <G> Object processObjectAtEntryWithGoal(SingleConfEntry entry, Class<G> goal, Object preValue)
			throws InvalidConfigException {

		// @ConfSerialiser override
		@SuppressWarnings("unchecked")
		ValueSerialiser<G> confSerialiser = (ValueSerialiser<G>) entry.getSerialiser();
		if (confSerialiser != null) {
			return fromSerialiser(preValue, goal, confSerialiser);
		}

		Method method = entry.getMethod();
		// Collections
		if (goal == List.class || goal == Set.class || goal == Collection.class) {
			Class<?> elementType = entry.getCollectionElementType();
			return getAsCollection(entry, goal == List.class, preValue, method, elementType);
		}

		// boolean and String
		if (goal == boolean.class || goal == Boolean.class) {
			return getAsBoolean(preValue);
		} else if (goal == String.class) {
			return getAsString(preValue);
		}

		// Integer types
		if (goal == int.class || goal == Integer.class) {
			return getAsNumber(method, preValue).intValue();
		} else if (goal == long.class || goal == Long.class) {
			return getAsNumber(method, preValue).longValue();
		} else if (goal == short.class || goal == Short.class) {
			return getAsNumber(method, preValue).shortValue();
		} else if (goal == byte.class || goal == Byte.class) {
			return getAsNumber(method, preValue).byteValue();
		}

		// Floating point types
		if (goal == double.class || goal == Double.class) {
			return getAsNumber(method, preValue).doubleValue();
		} else if (goal == float.class || goal == Float.class) {
			return getAsNumber(method, preValue).floatValue();
		}

		// char
		if (goal == char.class || goal == Character.class) {
			String string = getAsString(preValue);
			if (string.length() != 1) {
				throw new BadValueException.Builder().key(key).message("value " + preValue + " is not a single character").build();
			}
			return string.charAt(0);
		}

		// Enums
		if (goal.isEnum()) {
			String parsable = getAsString(preValue);
			if (!options.strictParseEnums()) {
				parsable = parsable.toUpperCase(Locale.ROOT);
			}
			return enumValueOf(goal, parsable);
		}

		// All other types
		ValueSerialiser<G> serialiser  = options.getSerialisers().getSerialiser(goal);
		if (serialiser == null) {
			throw new IllDefinedConfigException("No ValueSerialiser for " + key);
		}
		return fromSerialiser(preValue, goal, serialiser);
	}
	
	private <G> G fromSerialiser(Object preValue, Class<G> goal, ValueSerialiser<G> serialiser) throws BadValueException {
		String toParse = getAsString(preValue);
		G deserialised = serialiser.deserialise(key, toParse);
		if (deserialised == null) {
			throw new IllDefinedConfigException(
					"At key " + key + ", ValueSerialiser#deserialise for " + serialiser + " returned null");
		}
		return goal.cast(deserialised);
	}
	
	/*
	 * 
	 * Type conversion methods
	 * 
	 */
	
	private <E> Collection<E> getAsCollection(SingleConfEntry entry, boolean ordered, Object preValue,
			Method method, Class<E> elementClass) throws InvalidConfigException {

		Collection<E> result = (ordered) ? new ArrayList<>() : new HashSet<>();
		if (!(preValue instanceof List)) {
			throw new BadValueException.Builder().key(key).message("value " + preValue + " is not a List").build();
		}
		List<?> asList = (List<?>) preValue;
		CollectionSize sizing = method.getAnnotation(CollectionSize.class);
		if (sizing != null) {
			int size = asList.size();
			if (size < sizing.min()) {
				throw new BadValueException.Builder().key(key)
						.message("value's size " + size + " is less than minimum size " + sizing.min()).build();
			}
			if (size > sizing.max()) {
				throw new BadValueException.Builder().key(key).message(
						"value's size " + size + " is more than maximum size " + sizing.max()).build();
			}
		}
		for (Object element : asList) {
			result.add(elementClass.cast(processObjectAtEntryWithGoal(entry, elementClass, element)));
		}
		return (ordered) ? ImmutableCollections.listOf(result) : ImmutableCollections.setOf(result);
	}
	
	private String getAsString(Object value) {
		return value.toString();
	}
	
	private Number getAsNumber(Method method, Object value) throws BadValueException {
		Number number = getAsNumber0(value);

		NumericRange numericRange = method.getAnnotation(NumericRange.class);
		if (numericRange != null) {
			double asDouble = number.doubleValue();
			if (asDouble < numericRange.min()) {
				throw new BadValueException.Builder().key(key)
						.message("value's size " + asDouble + " is less than minimum size " + numericRange.min()).build();
			}
			if (asDouble > numericRange.max()) {
				throw new BadValueException.Builder().key(key)
						.message("value's size " + asDouble + " is more than maximum size " + numericRange.max()).build();
			}
		}
		IntegerRange intRange = method.getAnnotation(IntegerRange.class);
		if (intRange != null) {
			long asLong = number.longValue();
			if (asLong < intRange.min()) {
				throw new BadValueException.Builder().key(key)
						.message("value's size " + asLong + " is less than minimum size " + intRange.min()).build();
			}
			if (asLong > intRange.max()) {
				throw new BadValueException.Builder().key(key)
						.message("value's size " + asLong + " is more than maximum size " + intRange.max()).build();
			}
		}
		return number;
	}
	
	private Number getAsNumber0(Object value) throws BadValueException {
		if (value instanceof Number) {
			return (Number) value;
		}
		if (value instanceof String) {
			Number parsed;
			try {
				parsed = NumberFormat.getInstance().parse((String) value);
			} catch (ParseException ex) {
				throw new BadValueException.Builder().key(key).message(
						"value " + value + " is not a Number and cannot be converted to one").cause(ex).build();
			}
			return parsed;
		}
		throw new BadValueException.Builder().key(key).message("value " + value + " is not a Number").build();
	}
	
	private Boolean getAsBoolean(Object value) throws BadValueException {
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		if (value instanceof String) {
			String parsable = (String) value;
			if (parsable.equalsIgnoreCase("true") || parsable.equalsIgnoreCase("yes")) {
				return Boolean.TRUE;
			}
			if (parsable.equalsIgnoreCase("false") || parsable.equalsIgnoreCase("false")) {
				return Boolean.FALSE;
			}
		}
		throw new BadValueException.Builder().key(key).message("value " + value + " is not a boolean").build();
	}
	
	@SuppressWarnings("unchecked")
	private <T, E extends Enum<E>> Enum<E> enumValueOf(Class<T> enumClass, String parsable) throws BadValueException {
		try {
			return Enum.valueOf((Class<E>) enumClass, parsable);
		} catch (IllegalArgumentException ex) {
			throw new BadValueException.Builder().key(key)
					.message("value " + parsable + " is not a " + enumClass.getName()).build();
		}
	}
	
	/**
	 * Retrieves the pre processing value for a config entry
	 * 
	 * @param entry the config entry
	 * @return the pre processed value
	 * @throws MissingKeyException if the key is not defined
	 */
	abstract Object getValueFromSources(ConfEntry entry) throws MissingKeyException;
	
}
