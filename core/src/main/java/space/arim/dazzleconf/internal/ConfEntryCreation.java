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
package space.arim.dazzleconf.internal;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import space.arim.dazzleconf.annote.ConfValidator;
import space.arim.dazzleconf.annote.SubSection;
import space.arim.dazzleconf.error.IllDefinedConfigException;
import space.arim.dazzleconf.internal.util.MethodUtil;
import space.arim.dazzleconf.validator.ValueValidator;

class ConfEntryCreation {

	private final DefinitionReader<?> reader;
	private final Method method;

	ConfEntryCreation(DefinitionReader<?> reader, Method method) {
		this.reader = reader;
		this.method = method;
	}

	String getQualifiedMethodName() {
		return MethodUtil.getQualifiedName(method);
	}

	ConfEntry create() {
		if (method.getParameterCount() > 0) {
			throw new IllDefinedConfigException(getQualifiedMethodName() + " should not have parameters");
		}
		Class<?> returnType = method.getReturnType();
		if (!Modifier.isPublic(returnType.getModifiers())) {
			throw new IllDefinedConfigException(
					getQualifiedMethodName() + " has non-public return type " + returnType.getName());
		}
		return create0();
	}

	private ConfEntry create0() {
		if (method.getAnnotation(SubSection.class) != null) {
			Class<?> configClass = method.getReturnType();
			if (!configClass.isInterface()) {
				throw new IllDefinedConfigException(configClass.getName() + " is not an interface");
			}
			DefinitionReader<?> nestedReader = reader.createNestedReader(configClass);
			return new NestedConfEntry<>(method, nestedReader.read());
		}
		ValueValidator validator = getValidator();
		return new SingleConfEntry(method, validator);
	}

	private ValueValidator getValidator() {
		ConfValidator chosenValidator = method.getAnnotation(ConfValidator.class);
		return (chosenValidator == null) ? null : reader.instantiate(ValueValidator.class, chosenValidator.value());
	}

}
