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

/* File:    Weekly.java
 * Author:  Stas Torgashov aka Crimson Cat (crimcat@yandex.ru)
 * Created: 2011, March 25
 */

package org.crimcat.lib.wpr;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import org.crimcat.lib.wpr.DatabaseConfig.DatabaseFilesBundle;

/**
 * Weekly database representation.
 */
public class Weekly {

    /**
     * Weekly todo task entry editor interface.
     */
    public interface IEditor {
        /**
         * Set new memo text for this week.
         * @param memoText string with memo text
         */
        void setMemo(String memoText);

        /**
         * Add new task to this weekly
         * @param description task title (description)
         */
        void addTask(String description);

        /**
         * Mark the given task completed.
         * @param task todo task reference
         */
        void markTaskCompleted(TodoTask task);
    }

    /**
     * Ctor: create and load weekly database for today date.
     * @throws IOException
     */
    public Weekly() throws IOException {
        monday = new TaskDate().shiftToWeekDay(TaskDate.WeekDay.MONDAY);
        load();
    }

    /**
     * Ctor: create and load weekly database for the given date.
     * @param forDate date object reference
     * @throws IOException
     */
    public Weekly(TaskDate forDate) throws IOException {
        monday = forDate.shiftToWeekDay(TaskDate.WeekDay.MONDAY);
        load();
    }

    /**
     * Get number of tasks for this weekly.
     * @return integer number of tasks (both completed and active)
     */
    public int size() {
        return tasks.size();
    }

    /**
     * Get reference to the task by its number in the list.
     * @param idx task object index, must be less than @see size result.
     * @return reference to the task with the given index
     */
    public TodoTask taskAt(int idx) {
        if((idx < 0) || (idx >= size())) {
            throw new RuntimeException();
        }
        return tasks.get(idx);
    }

    /**
     * Get Monday date for this week.
     * @return Monday date object
     */
    public TaskDate startedOn() {
        return monday;
    }

    /**
     * Get memo string for this weekly.
     * @return string with memo text
     */
    public String memo() {
        return memoText == null ? "" : memoText;
    }

    /**
     * Check if the given weekly object editable. The object is editable if
     * and only if today is inside this weekly.
     * @return true if this weekly is current and can be edited
     */
    public boolean isEditable() {
        TaskDate td = new TaskDate().shiftToWeekDay(TaskDate.WeekDay.MONDAY);
        return td.equals(monday);
    }

    /**
     * Get editor interface for this weekly. If the weekly cannot be edited
     * this method returns null, otherwise it returns a valid editor object,
     * @return editor object reference or null
     */
    public IEditor getEditor() {
        if(isEditable()) {
            return new Weekly.IEditor() {
                @Override
                public void setMemo(String txt) {
                    memoText = txt;
                    wasChanged = true;
                }
                @Override
                public void addTask(String description) {
                    tasks.add(new TodoTask(description));
                    wasChanged = true;
                }
                @Override
                public void markTaskCompleted(TodoTask task) {
                    if((null != task) && tasks.contains(task)) {
                        task.markCompleted();
                        wasChanged = true;
                    }
                }
            };
        }
        return null;
    }

    /**
     * Save weekly changes if any.
     * @throws IOException
     */
    public void sync() throws IOException {
        if(wasChanged) {
            save();
            wasChanged = false;
        }
    }

    /**
     * Load weekly from the database.
     * @throws IOException
     */
    private void load() throws IOException {
        dbbundle = DatabaseConfig.instance().getDatabaseBundleForDate(monday);
        // verify checksum
        if(!dbbundle.verifyChecksum()) {
            throw new ChecksumException();
        }

        // load memo
        if(dbbundle.getMemoFile().exists()) {
            BufferedReader memobr = new BufferedReader(new FileReader(dbbundle.getMemoFile()));
            StringBuilder memoBuf = new StringBuilder();
            while(memobr.ready()) {
                String nextLine = memobr.readLine();
                if(nextLine.length() > 0) {
                    memoBuf.append(nextLine);
                    memoBuf.append('\n');
                }
            }
            memobr.close();
            memoText = memoBuf.toString();
        }

        // load todos
        BufferedReader todobr = new BufferedReader(new FileReader(dbbundle.getTodoListFile()));
        while(todobr.ready()) {
            String nextLine = todobr.readLine();
            if(nextLine.length() > 0) {
                TodoTask tt = new TodoTask("x"); // fake parameters
                if(!tt.fromString(nextLine)) {
                    throw new RuntimeException("Cannot parse todo record: " + nextLine);
                }
                tasks.add(tt);
            }
        }
        todobr.close();

        wasChanged = false;
    }

    /**
     * Save weekly to the database.
     * @throws IOException
     */
    private void save() throws IOException {
        // save memo
        if(0 == memo().length()) {
            // delete memo file if no memo found
            dbbundle.getMemoFile().delete();
        } else {
            PrintStream memofos = new PrintStream(dbbundle.getMemoFile());
            memofos.print(memo());
            memofos.flush();
            memofos.close();
        }
        // save todos
        PrintStream todofos = new PrintStream(dbbundle.getTodoListFile());
        for(int i = 0; i < size(); ++i) {
            todofos.println(taskAt(i));
        }
        todofos.flush();
        todofos.close();
        // update checksum
        dbbundle.updateChecksum();

        wasChanged = true;
    }

    // Monday date object
    private TaskDate monday = null;
    // List of todo tasks
    private ArrayList<TodoTask> tasks = new ArrayList<TodoTask>();
    // String with memo text
    private String memoText = "";
    // Weekly changes flag
    private boolean wasChanged = false;
    // Database manager object reference
    private DatabaseFilesBundle dbbundle = null;
}
