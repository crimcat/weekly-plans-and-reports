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

/* File:    TodoTask.java
 * Author:  Stas Torgashov aka Crimson Cat (crimcat@yandex.ru)
 * Created: 2011, March 25
 */

package org.crimcat.lib.wpr;

/**
 * Todo task object. It's an aggregation of the following date:
 * creation date, task title and completion flag. Tasks are simple and
 * can be in only two states: active and completed. Tasks cannot be modified
 * in a way other than marking an active task as completed one. Now other
 * modifications are provided.
 */
public class TodoTask {

    /**
     * Create new task with the given title (description). The date is taken
     * as today.
     * @param descr string with task description
     */
    public TodoTask(String descr) {
        if((null == descr) || (0 == descr.length())) {
            throw new RuntimeException();
        }
        this.description = descr;
        this.originated = new TaskDate();
    }

    /**
     * Get task title (description).
     * @return task title
     */
    public String title() {
        return description;
    }

    /**
     * Get task creation date.
     * @return task date object
     */
    public TaskDate originatedOn() {
        return originated;
    }

    /**
     * Check if the task is already completed
     * @return true if the task is completed, false if it's active
     */
    public boolean isCompleted() {
        return fCompleted;
    }

    /**
     * Get string representation, the format is following:
     * DATE:[A|C]:TITLE, ':' - is the delimiter.
     * @return task as string
     */
    @Override
    public String toString() {
        return originated.toString() + ":" +
            (fCompleted ? "C" : "A") + ":" +
            description;
    }

    /**
     * Mark the task completed. This action cannot be revoked.
     */
    void markCompleted() {
        fCompleted = true;
    }

    /**
     * Read task from the string, @see toString. The expected format is
     * DATE:[A|C]:TITLE
     * If parsing is failed the contents of the current object is not changed.
     * @param str string to parse
     * @return true if read, false if string is not recognized
     */
    public boolean fromString(String str) {
        String[] parts = str.split(":");
        if(parts.length >= 3) {
            TaskDate _td = new TaskDate();
            if(_td.fromString(parts[0])) {
                if("A".equals(parts[1]) || "C".equals(parts[1])) {
                    originated = _td;
                    fCompleted = "C".equals(parts[1]);
                    description = parts[2];
                    for(int i = 3; i < parts.length; ++i) {
                        description += ":" + parts[i];
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private TaskDate originated = null;
    private String description = null;
    private boolean fCompleted = false;
}
