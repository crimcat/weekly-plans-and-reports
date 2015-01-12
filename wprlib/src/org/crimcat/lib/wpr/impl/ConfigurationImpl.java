/*
    (Java) Weekly Plans and Reports - simple and handy todo planning tool.

    Copyright (C) 2011-2014  Stas Torgashov

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/* File:    ConfigurationImpl.java
 * Author:  Stas Torgashov aka Crimson Cat (crimcat@yandex.ru)
 * Created: 2015, January 11
 */

package org.crimcat.lib.wpr.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.crimcat.lib.wpr.AppDatabase;

/**
 * Implementation of application configuration.
 * @author Stas Torgashov
 */
public class ConfigurationImpl implements AppDatabase.Configuration {
    /**
     * Default application config options file name.
     */
    private static final String APP_CONFIG_FILE_NAME = ".global_config";
    
    /**
     * Config option for automatic copy todo list from previous week on Mondays.
     */
    private static final String APP_OPTION_AUTO_COPY_FROM_THE_PAST = "auto-copy-from-the-past";
    /**
     * Config option for verbose output on commands execution.
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
    
    /**
     * Properties read from configuration file.
     */
    Properties config = new Properties();
}
