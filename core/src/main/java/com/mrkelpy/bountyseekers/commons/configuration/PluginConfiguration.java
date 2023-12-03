package com.mrkelpy.bountyseekers.commons.configuration;

import com.mrkelpy.bountyseekers.commons.utils.FileUtils;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * This class implements a configuration for the general plugin settings.
 */
public class PluginConfiguration {

    public static final PluginConfiguration INSTANCE = new PluginConfiguration();
    private final File configFile = new File(FileUtils.makeDirectory("."), "config.yml");
    private final YamlConfiguration config = YamlConfiguration.loadConfiguration(this.configFile);

    /**
     * Main constructor for the PluginConfiguration class.
     * Adds the default values to the config and saves it.
     */
    public PluginConfiguration() {
        this.addDefaults();
        this.save();
    }

    /**
     * Returns the configuration object to interact with.
     * @return The configuration
     */
    public YamlConfiguration getConfig() {
        return this.config;
    }

    /**
     * Saves the config into memory.
     */
    public void save() {
        try {
            this.config.save(this.configFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves the default data values to the file.
     */
    private void addDefaults() {

        if (!this.config.contains("general.commands.truly-silent-raise"))
            this.config.set("general.commands.truly-silent-raise", false);

    }
}
