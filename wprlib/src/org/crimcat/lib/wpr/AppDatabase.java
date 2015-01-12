/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.crimcat.lib.wpr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.crimcat.lib.wpr.impl.ConfigurationImpl;
import org.crimcat.lib.wpr.impl.FilesBundleImpl;

/**
 * Application database object: gate to application configuration and
 * database files bundles.
 * @author Stas Torgashov
 */
public class AppDatabase {
    /**
     * Interface to provide application configuration, it's read from database.
     */
    public interface Configuration {
        /**
         * If verbose output needed.
         * @return true if verbose setting is on
         */
        boolean doVerboseOuput();
        
        /**
         * If automatic copy from past option is needed
         * @return true if automatic copy on mondays is on
         */
        boolean doCopyFromThePastOnMondays();
    }

    /**
     * Interface to provide files bundle abstraction to operate with
     * application data.
     */
    public interface FilesBundle {
        /**
         * Get path to root folder of application database files.
         * Should not return null.
         * @return path object reference to root path
         */
        Path getRootDatabasePath();
        
        /**
         * Get full path to ToDo list file.
         * @return path object reference to file with list of todos
         */
        Path getTodoListFilePath();
        
        /**
         * Get full path to Memo file.
         * File may be not present at all.
         * @return path object reference to file with memo text
         */
        Path getMemoFilePath();
        
        /**
         * Check files consistency.
         * @return true if all data is ok
         */
        boolean checkConsistency();
        
        /**
         * Force updating checksum on files bundle contents.
         */
        void updateChecksum();
    }
    
    /**
     * Get application configuration.
     * @return configuration instance with current application settings
     */
    public static Configuration getAppConfig() {
        return new ConfigurationImpl();
    }
    
    /**
     * Get database files bundle for the given date.
     * @param td task date for which files bundle is requested
     * @return files bundle object
     */
    public static FilesBundle getFilesBundle(TaskDate td) {
        return new FilesBundleImpl(td);
    }
    
    /**
     * Get database files bundle for the given date and given group.
     * @param td task date for which files bundle is requested
     * @param groupName group name
     * @return files bundle object
     */
    public static FilesBundle getFilesBundle(TaskDate td, String groupName) {
        return new FilesBundleImpl(td, groupName);
    }
    
    /**
     * Get default application database path.
     * This is the place where we expect to find application settings and all
     * file bundles.
     * @return path with full path to the root directory
     */
    public static Path getDefaultAppDatabasePath() {
        if(null == appDatabasePath) {
            String home = System.getProperty("user.home");
            if(null == home) {
                throw new RuntimeException();
            }
            String sep = System.getProperty("file.separator");
            appDatabasePath = Paths.get(home + sep + APP_DEFAULT_DIR_NAME);
            if(!Files.exists(appDatabasePath)) try {
                Files.createDirectory(appDatabasePath);
            } catch(IOException ex) {
                throw new RuntimeException(ex.toString());
            }
        }
        return appDatabasePath;
    }
    
    /**
     * Set default application database path.
     * @param path string with full path of root application database folder
     * @return true if path exists and can be successfully set
     */
    public static boolean setDefaultAppDatabasePath(String path) {
        Path newPath = Paths.get(path);
        if(Files.exists(newPath) && Files.isDirectory(newPath) && Files.isReadable(newPath) && Files.isWritable(newPath)) {
            appDatabasePath = newPath;
            return true;
        }
        return false;
    }
    
    /**
     * No class instances.
     */
    private AppDatabase() {
    }
    
    /**
     * Current application database path.
     */
    private static Path appDatabasePath = null;
    
    /**
     * Default directory name for application database files.
     */
    private static final String APP_DEFAULT_DIR_NAME = ".wpr";
}
