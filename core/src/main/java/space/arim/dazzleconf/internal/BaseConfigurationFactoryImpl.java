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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Objects;

import space.arim.dazzleconf.ConfigurationFactory;
import space.arim.dazzleconf.ConfigurationOptions;
import space.arim.dazzleconf.error.InvalidConfigException;

public abstract class BaseConfigurationFactoryImpl<C> implements ConfigurationFactory<C> {

	private final Class<C> configClass;
	private final ConfigurationOptions options;
	
	protected BaseConfigurationFactoryImpl(Class<C> configClass, ConfigurationOptions options) {
		Objects.requireNonNull(configClass, "configClazz");
		if (!configClass.isInterface()) {
			throw new IllegalArgumentException(configClass.getName() + " is not an interface");
		}
		this.configClass = configClass;
		this.options = Objects.requireNonNull(options, "options");
	}
	
	@Override
	public Class<C> getConfigClass() {
		return configClass;
	}
	
	@Override
	public ConfigurationOptions getOptions() {
		return options;
	}
	
	protected abstract Charset charset();
	
	// Reading
	
	protected abstract C loadFromReader(Reader reader) throws IOException, InvalidConfigException;
	
	protected abstract C loadFromReader(Reader reader, C auxiliaryEntries) throws IOException, InvalidConfigException;
	
	private C loadConfig(Reader reader) throws IOException, InvalidConfigException {
		try (Reader reader0 = reader; BufferedReader buffReader = new BufferedReader(reader)) {

			return loadFromReader(buffReader);
		}
	}
	
	private C loadConfig(Reader reader, C auxiliaryEntries) throws IOException, InvalidConfigException {
		try (Reader reader0 = reader; BufferedReader buffReader = new BufferedReader(reader)) {

			return loadFromReader(buffReader, auxiliaryEntries);
		}
	}
	
	private Reader toReader(ReadableByteChannel readChannel) {
		return Channels.newReader(readChannel, charset().newDecoder(), -1); // Channels.newReader performs null check
	}
	
	private Reader toReader(InputStream inputStream) {
		return new InputStreamReader(inputStream, charset()); // InputStreamReader performs null check
	}

	@Override
	public C load(ReadableByteChannel readChannel) throws IOException, InvalidConfigException {
		return loadConfig(toReader(readChannel));
	}

	@Override
	public C load(InputStream inputStream) throws IOException, InvalidConfigException {
		return loadConfig(toReader(inputStream));
	}
	
	@Override
	public C load(ReadableByteChannel readChannel, C auxiliaryEntries) throws IOException, InvalidConfigException {
		configClass.cast(Objects.requireNonNull(auxiliaryEntries, "auxiliaryEntries"));
		return loadConfig(toReader(readChannel), auxiliaryEntries);
	}

	@Override
	public C load(InputStream inputStream, C auxiliaryEntries) throws IOException, InvalidConfigException {
		configClass.cast(Objects.requireNonNull(auxiliaryEntries, "auxiliaryEntries"));
		return loadConfig(toReader(inputStream), auxiliaryEntries);
	}
	
	// Writing
	
	protected abstract void writeToWriter(C configData, Writer writer) throws IOException;
	
	private void writeConfig(C configData, Writer writer) throws IOException {
		try (Writer writer0 = writer; BufferedWriter buffWriter = new BufferedWriter(writer)) {

			writeToWriter(configData, buffWriter);
		}
	}

	@Override
	public void write(C configData, WritableByteChannel writableChannel) throws IOException {
		configClass.cast(Objects.requireNonNull(configData, "configData"));
		writeConfig(configData, Channels.newWriter(writableChannel, charset().newEncoder(), -1)); // Channels.newWriter performs null check
	}

	@Override
	public void write(C configData, OutputStream outputStream) throws IOException {
		configClass.cast(Objects.requireNonNull(configData, "configData"));
		writeConfig(configData, new OutputStreamWriter(outputStream, charset())); // OutputStreamWriter performs null check
	}
	
}
