package org.wso2.apimgt.gateway.codegen.config;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import org.wso2.apimgt.gateway.codegen.exception.ConfigParserException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TOMLConfigParser {

    /**
     * Parses the given YAML configuration file and de-serialize the content into given bean type.
     *
     * @param configFilePath path to toml file
     * @param type           class of the bean to be used when de-serializing
     * @param <T>            type of the bean class to be used when de-serializing
     * @return returns the populated bean instance
     * @throws ConfigParserException if cannot read or parse the content of the specified YAML file
     */
    public static <T> T parse(String configFilePath, Class<T> type) throws ConfigParserException {
        Path configurationFile = Paths.get(configFilePath);
        if (!Files.exists(configurationFile)) {
            throw new ConfigParserException("Mandatory configuration file '" + configurationFile + "' does not exists.");
        }

        T loadedBean;
        String content = null;
        try {
            content = new String(Files.readAllBytes(configurationFile), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ConfigParserException("Cannot read the content of configuration file '" + configurationFile + "'.",
                    e);
        }
        loadedBean = parseString(content, type);
        return loadedBean;
    }

    public static void write(String configFilePath, Object content) throws ConfigParserException {
        Path configurationFile = Paths.get(configFilePath);
        if (!Files.exists(configurationFile)) {
            throw new ConfigParserException("Mandatory configuration file '" + configurationFile + "' does not exists.");
        }
        TomlWriter tomlWriter = new TomlWriter();
        try {
            tomlWriter.write(content, new File(configFilePath));
        } catch (IOException e) {
            throw new ConfigParserException("Cannot write the content to configuration file '" + configurationFile + "'.",
                    e);
        }
    }

    static <T> T parseString(String configFileContent, Class<T> type) {
        return new Toml().read(configFileContent).to(type);
    }
}
