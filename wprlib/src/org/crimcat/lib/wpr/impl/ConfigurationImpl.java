/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.crimcat.lib.wpr.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.crimcat.lib.wpr.AppDatabase;

/**
 *
 * @author Stas
 */
public class ConfigurationImpl implements AppDatabase.Configuration {
    /**
     * Default application config options file name
     */
    private static final String APP_CONFIG_FILE_NAME = ".global_config";
    
    /**
     * Config option for automatic copy todo list from previous week on Mondays
     */
    private static final String APP_OPTION_AUTO_COPY_FROM_THE_PAST = "auto-copy-from-the-past";
    /**
     * Config option for verbose output on commands execution
     */
    private static final String APP_OPTION_VERBOSE_OUTPUT = "verbose-output";
    
    public ConfigurationImpl() {
        try {
            Path configPath = Paths.get(AppDatabase.getDefaultAppDatabasePath() +
                System.getProperty("file.separator") + APP_CONFIG_FILE_NAME);
            if(Files.exists(configPath) && Files.isReadable(configPath)) {
                config.load(Files.newInputStream(configPath));
            } else {
                config.put(APP_OPTION_AUTO_COPY_FROM_THE_PAST, "false");
                config.put(APP_OPTION_VERBOSE_OUTPUT, "false");
                config.store(Files.newOutputStream(configPath), "");
            }
        } catch(IOException ex) {
            throw new RuntimeException(ex.toString());
        }
    }
    
    @Override
    public boolean doVerboseOuput() {
        String prop = config.getProperty(APP_OPTION_VERBOSE_OUTPUT);
        return "true".equalsIgnoreCase(prop);
    }

    @Override
    public boolean doCopyFromThePastOnMondays() {
        String prop = config.getProperty(APP_OPTION_AUTO_COPY_FROM_THE_PAST);
        return "true".equalsIgnoreCase(prop);
    }
    
    Properties config = new Properties();
}
