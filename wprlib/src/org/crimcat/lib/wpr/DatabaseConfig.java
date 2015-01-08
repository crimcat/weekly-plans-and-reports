/*
    (java) library for Weekly Plans and Reminder

    Copyright (C) 2011  Stas Torgashov

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

/* File:    DatabaseConfig.java
 * Author:  Stas Torgashov aka Crimson Cat (crimcat@yandex.ru)
 * Created: 2011, March 25
 */

package org.crimcat.lib.wpr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.zip.CRC32;

/**
 * Application configuration manager.
 * Prepare access and check summing of the weekly database files.
 * By default, todo databases are loaded from .wpr directory from the user
 * home directory. This should definitely work for windows and linux (at
 * least, it was tested on these OSes).
 */
public final class DatabaseConfig {

    /**
     * Default directory name for the database files.
     */
    private static final String APP_DEFAULT_DIR_NAME = ".wpr";
    /**
     * Default application config options file name
     */
    private static final String APP_CONFIG_FILE_NAME = ".global_config";

    /**
     * Get instance of the configuration object. It's a singletone.
     * @return database config object reference
     */
    public static DatabaseConfig instance() {
        if(null == dbconfig) {
            dbconfig = new DatabaseConfig();
        }
        return dbconfig;
    }
    // Singletone database object
    private static DatabaseConfig dbconfig = null;

    /**
     * Get default database path in user home directory.
     * @return string with default database path
     */
    public String getDefaultDatabasePath() {
        String home = System.getProperty("user.home");
        String sep = System.getProperty("file.separator");
        if(null == home) {
            throw new RuntimeException();
        }
        return home + sep + APP_DEFAULT_DIR_NAME;
    }

    /**
     * Get current database path.
     * @return string with the current database path
     */
    public String getCurrentDatabasePath() {
        return currentPath.toAbsolutePath().toString();
    }

    /**
     * Check if the current database path valid. This check is nominal because
     * the check is done while setting application database path.
     * @return true if application database path exists and valid
     */
    public boolean isCurrentDatabasePathValid() {
        return (currentPath != null) && isPathValid(currentPath);
    }

    /**
     * Set some other application database path if needed.
     * @param newPath string with new database path
     * @return true if database path is successfully set and verified
     */
    public boolean setDatabasePath(String newPath) {
        Path path = Paths.get(newPath);
        if(isPathValid(path)) {
            currentPath = path;
            return true;
        }
        return false;
    }

    /**
     * Internal database bundle structure. Manages the set of weekly
     * database files: todo list, memo text and check sum.
     * Basename is taken from the Monday date in YYYY-MM-DD format.
     */
    class DatabaseFilesBundle {
        // predefined file extentions for weekly database
        private static final String EXT_TODOLIST = ".todolist";
        private static final String EXT_MEMO = ".memo";
        private static final String EXT_CHECKSUM = ".checksum";

        /**
         * Get database todo list file object. The file may not exist at all.
         * @return todo list file object
         */
        Path getTodoListPath() {
            return todoListPath;
        }

        /**
         * Get database memo text file object. The file may not exist at all.
         * @return memo text file object
         */
        Path getMemoPath() {
            return memoPath;
        }

        /**
         * Internal function: check check sum of weekly database files.
         * @return true if check summing is ok
         * @throws IOException
         */
        boolean verifyChecksum() throws IOException {
            if(Files.exists(checksumFilePath) && Files.isReadable(checksumFilePath)) {
                CRC32 crc32 = new CRC32();
                updateCRC32(crc32, getMemoPath());
                updateCRC32(crc32, getTodoListPath());
                BufferedReader br = Files.newBufferedReader(checksumFilePath);
                String cksumStr = br.readLine();
                br.close();
                return crc32.getValue() == Long.parseLong(cksumStr);
            }
            return true; // we do not check if no checksum file found
        }

        /**
         * Internal function: recalculate weekly database check sum for
         * todo list and memo text files and write it to the check sum file.
         * @throws IOException
         */
        void updateChecksum() throws IOException {
            CRC32 crc32 = new CRC32();
            updateCRC32(crc32, getMemoPath());
            updateCRC32(crc32, getTodoListPath());
            try (BufferedWriter bw = Files.newBufferedWriter(checksumFilePath)) {
                bw.write(Long.toUnsignedString(crc32.getValue()));
            }
        }

        /**
         * Local ctor: create database bundle description for the given date
         * in the given user database path.
         * @param date date to use, should point to the Monday of the given week
         * @param rootDir file object with the database root directory
         * @throws IOException
         */
        private DatabaseFilesBundle(TaskDate date, Path rootDir) throws IOException {
            String basename = rootDir + System.getProperty("file.separator") + date.toString();

            todoListPath = Paths.get(basename + EXT_TODOLIST);
            memoPath = Paths.get(basename + EXT_MEMO);
            checksumFilePath = Paths.get(basename + EXT_CHECKSUM);
            
            if(!Files.exists(todoListPath)) {
                Files.createFile(todoListPath);
            }
        }

        // database file objects paths
        private Path todoListPath = null;
        private Path memoPath = null;
        private Path checksumFilePath = null;
    }

    /**
     * Internal function: get database bundle for the given date. It's a
     * singleton for the clients of this class.
     * @param date Monday date object
     * @return initialized database bundle for the given date
     * @throws IOException
     */
    DatabaseFilesBundle getDatabaseBundleForDate(TaskDate date) throws IOException {
        if(isCurrentDatabasePathValid()) {
            if(!Files.exists(currentPath)) {
                Files.createDirectory(currentPath);
            }
            return new DatabaseFilesBundle(date, currentPath);
        }
        return null;
    }
    
    /**
     * Global application options.
     * Integrated into application database config due to common storage, setup
     * and access policies.
     */
    public interface IGlobalOptions {
        boolean doCopyFromThePastOnMondays();
        boolean isVerboseOutput();
    }
    
    /**
     * Load and get current application global options set as interface.
     * @return reference to global options object
     */
    public IGlobalOptions getGlobalOptions() {
        if(null == globalConfig) {
            return new IGlobalOptions() {
                @Override
                public boolean doCopyFromThePastOnMondays() {
                    return false;
                }

                @Override
                public boolean isVerboseOutput() {
                    return false;
                }
            };
        }
        
        return new IGlobalOptions() {
            @Override
            public boolean doCopyFromThePastOnMondays() {
                String prop = globalConfig.getProperty(APP_OPTION_AUTO_COPY_FROM_THE_PAST);
                return "true".equalsIgnoreCase(prop);
            }

            @Override
            public boolean isVerboseOutput() {
                String prop = globalConfig.getProperty(APP_OPTION_VERBOSE_OUTPUT);
                return "true".equalsIgnoreCase(prop);
            }
        };
    }

    /**
     * Ctor: private creation of the object. It just initializes the user
     * database path.
     */
    private DatabaseConfig() {
        try {
            currentPath = Paths.get(getDefaultDatabasePath());
            if (!Files.exists(currentPath)) {
                Files.createDirectory(currentPath);
            }
            globalConfig = new Properties();
            Path configPath = Paths.get(currentPath + System.getProperty("file.separator") + APP_CONFIG_FILE_NAME);
            if(Files.exists(configPath) && Files.isReadable(configPath)) {
                globalConfig.load(Files.newInputStream(configPath));
            } else {
                globalConfig.put(APP_OPTION_AUTO_COPY_FROM_THE_PAST, "false");
                globalConfig.put(APP_OPTION_VERBOSE_OUTPUT, "false");
                globalConfig.store(Files.newOutputStream(configPath), "");
            }
        } catch(FileNotFoundException ffex) {
        } catch(IOException ioex) {
        }
    }

    /**
     * Private method: check if the given database path valid.
     * @param dir file object representing the database directory
     * @return true if the database directory is valid and accessible
     */
    private static boolean isPathValid(Path dir) {
        return Files.exists(dir) && Files.isDirectory(dir) && Files.isReadable(dir) && Files.isWritable(dir);
    }

    /**
     * Private method: accumulative calculation of CRC32 for the given file.
     * Needed to find CRC32 for all database files for the give date.
     * @param crc32 CRC32 accumulator object (cannot be null)
     * @param file file to append its CRC32 to the result
     * @throws IOException
     */
    private static void updateCRC32(CRC32 crc32, Path path) throws IOException {
        if(Files.exists(path, LinkOption.NOFOLLOW_LINKS) && Files.isReadable(path)) {
            try(InputStream is = Files.newInputStream(path)) {
                while(is.available() > 0) {
                    crc32.update(is.read());
                }
            }
        }
    }
    
    private static final String APP_OPTION_AUTO_COPY_FROM_THE_PAST = "auto-copy-from-the-past";
    private static final String APP_OPTION_VERBOSE_OUTPUT = "verbose-output";
    
    // use database path
    private Path currentPath = null;
    private Properties globalConfig = null;
}
