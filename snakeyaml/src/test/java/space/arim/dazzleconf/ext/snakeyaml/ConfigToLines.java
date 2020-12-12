/* 
 * DazzleConf-snakeyaml
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * DazzleConf-snakeyaml is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DazzleConf-snakeyaml is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with DazzleConf-snakeyaml. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Lesser General Public License.
 */
package space.arim.dazzleconf.ext.snakeyaml;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import space.arim.dazzleconf.ConfigurationFactory;

public class ConfigToLines<C> {

	private final ConfigurationFactory<C> factory;

	public ConfigToLines(ConfigurationFactory<C> factory) {
		this.factory = factory;
	}

	public Stream<String> writeLines(C config) {
		var baos = new ByteArrayOutputStream();
		try {
			factory.write(config, baos);
		} catch (IOException ex) {
			fail(ex);
		}
		return baos.toString(StandardCharsets.UTF_8).lines();
	}

}
