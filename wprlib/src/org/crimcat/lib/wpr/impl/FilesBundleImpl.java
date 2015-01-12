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

/* File:    FilesBundleImpl.java
 * Author:  Stas Torgashov aka Crimson Cat (crimcat@yandex.ru)
 * Created: 2015, January 11
 */

package org.crimcat.lib.wpr.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.CRC32;
import org.crimcat.lib.wpr.AppDatabase;
import org.crimcat.lib.wpr.TaskDate;

/**
 * Implementation of files bundle class. 
 * @author Stas Torgashov
 */
public class FilesBundleImpl implements AppDatabase.FilesBundle {
    
    /**
     * Extention for todos list file.
     */
    private static final String EXT_TODOLIST = ".todolist";
    /**
     * Extention for memo file.
     */
    private static final String EXT_MEMO = ".memo";
    /**
     * Extention for checksum file.
     */
    private static final String EXT_CHECKSUM = ".checksum";
        
    public FilesBundleImpl(TaskDate td) {
        rootDatabasePath = AppDatabase.getDefaultAppDatabasePath();
        basename = rootDatabasePath.toString() +
            System.getProperty("file.separator") +
            td.toString();
    }
    
    public FilesBundleImpl(TaskDate td, String groupName) {
        rootDatabasePath = AppDatabase.getDefaultAppDatabasePath();
        basename = rootDatabasePath.toString() +
            System.getProperty("file.separator") + groupName +
            System.getProperty("file.separator") + td.toString();
    }
    
    @Override
    public Path getRootDatabasePath() {
        return rootDatabasePath;
    }

    @Override
    public Path getTodoListFilePath() {
        Path todoFile = Paths.get(basename + EXT_TODOLIST);
        if(!Files.exists(todoFile)) try {
            Files.createFile(todoFile);
        } catch(IOException ex) {
            throw new RuntimeException(ex.toString());
        }
        return todoFile;
    }

    @Override
    public Path getMemoFilePath() {
        return Paths.get(basename + EXT_MEMO);
    }

    @Override
    public boolean checkConsistency() {
        final Path checksumFilePath = getChecksumFilePath();
        if(Files.exists(checksumFilePath) && Files.isReadable(checksumFilePath)) try {
            CRC32 crc32 = new CRC32();
            updateCRC32(crc32, getMemoFilePath());
            updateCRC32(crc32, getTodoListFilePath());
            String cksumStr;
            try (BufferedReader br = Files.newBufferedReader(checksumFilePath)) {
                cksumStr = br.readLine();
            }
            return crc32.getValue() == Long.parseLong(cksumStr);
        } catch(IOException ex) {
            throw new RuntimeException(ex.toString());
        }
        return true; // we do not check if no checksum file found or is not accessible
    }

    @Override
    public void updateChecksum() {
        try {
            CRC32 crc32 = new CRC32();
            updateCRC32(crc32, getMemoFilePath());
            updateCRC32(crc32, getTodoListFilePath());
            try (BufferedWriter bw = Files.newBufferedWriter(getChecksumFilePath())) {
                bw.write(Long.toUnsignedString(crc32.getValue()));
            }
        } catch(IOException ex) {
            throw new RuntimeException(ex.toString());
        }
    }
    
    /**
     * Internal method to obtain a path to the checksum file.
     * @return path to checksum file object
     */
    private Path getChecksumFilePath() {
        return Paths.get(basename + EXT_CHECKSUM);
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
    
    /**
     * Path to root database folder.
     */
    private final Path rootDatabasePath;
    
    /**
     * Base name to construct bundle elements names.
     */
    private final String basename;
}
