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
 *
 * @author Stas
 */
public class AppDatabase {
    public interface Configuration {
        boolean doVerboseOuput();
        boolean doCopyFromThePastOnMondays();
    }

    public interface FilesBundle {
        Path getRootDatabasePath();
        Path getTodoListFilePath();
        Path getMemoFilePath();
        
        boolean checkConsistency();
        void updateChecksum();
    }
    
    public static Configuration getAppConfig() {
        return new ConfigurationImpl();
    }
    
    public static FilesBundle getFilesBundle(TaskDate td) {
        return new FilesBundleImpl(td);
    }
    
    public static FilesBundle getFilesBundle(TaskDate td, String groupName) {
        return new FilesBundleImpl(td, groupName);
    }
    
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
    
    public static boolean setDefaultAppDatabasePath(String path) {
        Path newPath = Paths.get(path);
        if(Files.exists(newPath) && Files.isDirectory(newPath) && Files.isReadable(newPath) && Files.isWritable(newPath)) {
            appDatabasePath = newPath;
            return true;
        }
        return false;
    }
    
    private AppDatabase() {
    }
    
    private static Path appDatabasePath = null;
    
    /**
     * Default directory name for application database files
     */
    private static final String APP_DEFAULT_DIR_NAME = ".wpr";
}
