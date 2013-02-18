/*
    (Java) Weekly Plans and Reports - simple and handy todo planning tool.

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

/* File:    WPRConsoleMain.java
 * Author:  Stas Torgashov aka Crimson Cat (crimcat@yandex.ru)
 * Created: 2011, March 25
 */

package org.crimcat.util.wpr;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.crimcat.lib.wpr.ChecksumException;
import org.crimcat.lib.wpr.DatabaseConfig;
import org.crimcat.lib.wpr.TaskDate;
import org.crimcat.lib.wpr.TodoTask;
import org.crimcat.lib.wpr.Weekly;
import org.crimcat.lib.wpr.TaskDate.WeekDay;

/**
 * Main utility functionality.
 * Reads command line and performs actions.
 */
public class WPRConsoleMain {

    /**
     * main function.
     * @param args command line arguments array
     */
    public static void main(String[] args) {
        boolean dbdir_option_set = false;
        boolean groups_option_set = false;
        boolean date_option_set = false;
        if(0 == args.length) {
            System.err.println("No commands specified.");
            System.err.println("Run with help command to see help information.");
        } else {
            // process options
            int argIdx;
            String curArgument = null;
            TaskDate theDate = new TaskDate(); // current date
            // read options first if any
            for(argIdx = 0; argIdx < args.length; ) {
                curArgument = args[argIdx++];
                if(!curArgument.startsWith("-")) {
                    break;
                }
                if(OPT_SETDATE.equals(curArgument)) {
                    if(date_option_set) {
                        System.err.println("Error: date selected multiple times.");
                        return;
                    }
                    if(args.length <= argIdx) {
                        System.err.println("Error: missing " + OPT_SETDATE + " data and command.");
                        return;
                    }
                    if(!theDate.fromString(args[argIdx])) {
                        System.err.println("Error: cannot understand the date - " + args[argIdx]);
                        return;
                    }
                    ++argIdx;
                    curArgument = null;
                    date_option_set = true;
                } else if(OPT_PREVIOUS_WEEK.equals(curArgument)) {
                    if(date_option_set) {
                        System.err.println("Error: date selected multiple times.");
                        return;
                    }
                    curArgument = null;
                    // move back to the previous Sunday
                    theDate = theDate.shiftToWeekDay(WeekDay.MONDAY).shift(-1);
                    date_option_set = true;
                } else if(OPT_DBDIR.equals(curArgument)) {
                    if(groups_option_set) {
                        System.err.println("Error: cannot use -b and -g options at the same time.");
                        return;
                    }
                    if(args.length <= argIdx) {
                        System.err.println("Error: missing " + OPT_DBDIR + " directory argument.");
                        return;
                    }
                    if(!DatabaseConfig.instance().setDatabasePath(args[argIdx])) {
                        System.err.println("Error: supplied path " + args[argIdx] + " is not valid.");
                        return;
                    }
                    ++argIdx;
                    curArgument = null;
                    dbdir_option_set = true;
                } else if(OPT_VERBOSE.equals(curArgument)) {
                    curArgument = null;
                    verbose = true;
                } else if(OPT_GROUP_SEL.equals(curArgument)) {
                    if(dbdir_option_set) {
                        System.err.println("Error: cannot use -b and -g options at the same time.");
                        return;
                    }
                    if(args.length <= argIdx) {
                        System.err.println("Error: missing " + OPT_GROUP_SEL + " group name argument.");
                        return;
                    }
                    String groupDir = DatabaseConfig.instance().getDefaultDatabasePath() +
                    System.getProperty("file.separator") +
                    args[argIdx++];
                    // check group directory
                    File groupDirFile = new File(groupDir);
                    if(!groupDirFile.exists()) {
                        if(!groupDirFile.mkdir()) {
                            System.err.println("Error: cannot create directory for the group - " +
                                groupDir);
                            return;
                        }
                        info("New group " + groupDirFile.getName() + " created.");
                    }
                    if(!DatabaseConfig.instance().setDatabasePath(groupDir)) {
                        System.err.println("Error: supplied path " + args[argIdx] + " is not valid.");
                        return;
                    }
                    groups_option_set = true;
                    curArgument = null;
                } else {
                    System.err.println("Unknow option: " + curArgument + ". Exitting.");
                    return;
                }
            }
            if(null == curArgument) {
                System.err.println("Error: missing command.");
                return;
            }
            if(CMD_HELP.equals(curArgument)) {
                processCmdHelp();
                return;
            }
            // main switch below: identify command
            try {
                // process commands
                if(CMD_TODAY.equals(curArgument)) {
                    processCmdToday(new Weekly(theDate), theDate);
                } else if(CMD_DAILY.equals(curArgument)) {
                    processCmdDaily(new Weekly(theDate), theDate);
                } else if(CMD_WEEKLY.equals(curArgument)) {
                    processCmdWeekly(new Weekly(theDate));
                } else if(CMD_ADD.equals(curArgument)) {
                    processCmdAdd(new Weekly(theDate), args, argIdx);
                } else if(CMD_COMPLETE.equals(curArgument)) {
                    processCmdComplete(new Weekly(theDate), args, argIdx);
                } else if(CMD_SUMMARY.equals(curArgument)) {
                    processCmdSummary(new Weekly(theDate));
                } else if(CMD_MEMO.equals(curArgument)) {
                    processCmdMemo(new Weekly(theDate));
                } else if(CMD_SETMEMO.equals(curArgument)) {
                    processCmdSetmemo(new Weekly(theDate), args, argIdx);
                } else if(CMD_GROUPS.equals(curArgument)) {
                    processCmdGroups();
                } else if(CMD_COPY_FROM_THE_PAST.equals(curArgument)) {
                    processCmdCopyFromThePast();
                } else {
                    System.err.println("Error: unknown command specified - " + curArgument + ".");
                }
            } catch(IOException ex) {
                System.err.println("Error: database read/write unrecoverable error.");
            } catch(ChecksumException cex) {
                System.err.println("Error: database checksum verification failed.");
                System.err.println("Try to check manually .todolist file, or remove" +
                    " .checksum or .memo file for the date to let the application" +
                    " fix this issue by itself.");
            }
        }
    }

    /**
     * Print week header: week number, Monday date and Sunday date.
     * @param w weekly object
     */
    private static void printWeekHeader(Weekly w) {
        System.out.println("Week " + w.startedOn().weekNumber() + " - " +
            w.startedOn().toString() + "..." +
            w.startedOn().shiftToWeekDay(TaskDate.WeekDay.SUNDAY).toString() + ":");
    }

    /**
     * Print todo task: its number, originated date, status (done or in work)
     * and its title.
     * @param itemNo number of todo task
     * @param tt todo task object
     */
    private static void printTodoTaskItem(int itemNo, TodoTask tt) {
        System.out.print(itemNo + ". " + tt.originatedOn() + "|");
        System.out.print(tt.isCompleted() ? "DONE" : "WORK");
        System.out.println(": " + tt.title());
    }

    /**
     * Print todo task headline: only task title with prompt '-'
     * @param tt todo task object
     */
    private static void printTodoTaskHeadline(TodoTask tt) {
        System.out.println("- " + tt.title());
    }

    /**
     * Print todo task item as @see printTodoTaskItem does, but without
     * task status: number, originated date and task title.
     * @param itemNo number of task
     * @param tt todo task object
     */
    private static void printTodoTaskItemNoStatus(int itemNo, TodoTask tt) {
        System.out.println(itemNo + ". " + tt.originatedOn() + ": " + tt.title());
    }

    /**
     * Predefined commands.
     */
    private static final String CMD_HELP = "help";
    private static final String CMD_TODAY = "today";
    private static final String CMD_DAILY = "daily";
    private static final String CMD_WEEKLY = "weekly";
    private static final String CMD_ADD = "add";
    private static final String CMD_COMPLETE = "complete";
    private static final String CMD_SUMMARY = "summary";
    private static final String CMD_MEMO = "memo";
    private static final String CMD_SETMEMO = "set-memo";
    private static final String CMD_GROUPS = "groups";
    private static final String CMD_COPY_FROM_THE_PAST = "copy-from-the-past";

    /**
     * Print help info: utility title and help information.
     */
    private static void processCmdHelp() {
        printTitle();
        printHelp();
    }

    /**
     * Print today tasks: all tasks which are created today but not completed.
     * @param w weekly object to use
     * @param when date which is 'today'
     */
    private static void processCmdToday(Weekly w, TaskDate when) {
        int cnt = 0;
        for(int i = 0; i < w.size(); ++i) {
            TodoTask tt = w.taskAt(i);
            if(!tt.isCompleted() && tt.originatedOn().equals(when)) {
                if(0 == cnt++) {
                    System.out.println("Active tasks scheduled to be done on " + when.toString() + ":");
                }
                printTodoTaskItemNoStatus(i + 1, tt);
            }
        }
        if(0 == cnt) {
            info("No active tasks found for " + when.toString() + ".");
        }
    }

    /**
     * Print proposed daily task list: all active task by today.
     * @param w weekly object
     * @param when date which is 'today'
     */
    private static void processCmdDaily(Weekly w, TaskDate when) {
        int cnt = 0;
        for(int i = 0; i < w.size(); ++i) {
            TodoTask tt = w.taskAt(i);
            if(!tt.isCompleted() && (tt.originatedOn().compare(when) <= 0)) {
                if(0 == cnt++) {
                    System.out.println("Proposed todo plan up to " + when.toString() + ":");
                }
                printTodoTaskItemNoStatus(i + 1, tt);
            }
        }
        if(0 == cnt) {
            info("No active tasks found up to " + when.toString() + ".");
        }
    }

    /**
     * Print all weekly tasks, completed or not.
     * @param w weekly object
     */
    private static void processCmdWeekly(Weekly w) {
        printWeekHeader(w);
        int cnt = 0;
        for(int i = 0; i < w.size(); ++i) {
            printTodoTaskItem(i + 1, w.taskAt(i));
            ++cnt;
        }
        if(0 == cnt) {
            info("No tasks found.");
        }
    }

    /**
     * Add new task to the given weekly. The task title is extracted from the
     * given command line argument and its index.
     * @param w weekly object
     * @param args array of command line arguments
     * @param argIdx current command line parameter index
     */
    private static void processCmdAdd(Weekly w, String[] args, int argIdx) throws IOException {
        if(!w.isEditable()) {
            System.err.println("Error: can edit only current weekly plan");
        } else {
            if(args.length == argIdx) {
                System.err.println("Error: add operation requires task description.");
            } else {
                String descr = args[argIdx];
                w.getEditor().addTask(descr);
                info("Task successfully created.");
                w.sync();
            }
        }
    }

    /**
     * Mark the given task as completed. Task number (index) is extracted
     * from the command line parameters.
     * @param w weekly object
     * @param args array of command line arguments
     * @param argIdx index of current command line parameter
     */
    private static void processCmdComplete(Weekly w, String[] args, int argIdx) throws IOException {
        if(!w.isEditable()) {
            System.err.println("Error: can edit only current weekly plan");
        } else {
            if(args.length == argIdx) {
                System.err.println("Error: completed operation requires task index number.");
            } else {
                try {
                    int idx = Integer.parseInt(args[argIdx]);
                    if((idx <= 0) || (idx > w.size())) {
                        System.err.println("Error: cannot identify a task with the index - " + args[argIdx]);
                    } else {
                        TodoTask task = w.taskAt(idx - 1);
                        if(task.isCompleted()) {
                            System.err.println("Error: cannot complete already completed task " +
                                "(id = " + idx + ")");
                        } else {
                            w.getEditor().markTaskCompleted(task);
                            info("Task with id = " + idx + " is completed.");
                            w.sync();
                        }
                    }
                } catch(NumberFormatException nfe) {
                    System.err.println("Error: cannot parse index value of " + args[argIdx]);
                }
            }
        }
    }

    /**
     * Produce weekly report (summary), it has 2 sections: one for
     * completed, and one for uncompleted tasks.
     * @param w weekly object to use
     */
    private static void processCmdSummary(Weekly w) {
        printWeekHeader(w);
        int cnt = 0;
        System.out.println("COMPLETED:");
        for(int i = 0; i < w.size(); ++i) {
            TodoTask tt = w.taskAt(i);
            if(tt.isCompleted()) {
                printTodoTaskHeadline(tt);
                ++cnt;
            }
        }
        if(0 == cnt) {
            info("  No completed tasks found.");
        }
        cnt = 0;
        System.out.println("UNCOMPLETED TASKS OR OPPORTUNITIES:");
        for(int i = 0; i < w.size(); ++i) {
            TodoTask tt = w.taskAt(i);
            if(!tt.isCompleted()) {
                printTodoTaskHeadline(tt);
                ++cnt;
            }
        }
        if(0 == cnt) {
            info("  No active tasks found.");
        }
    }

    /**
     * Show weekly memo text.
     * @param w weekly object
     */
    private static void processCmdMemo(Weekly w) {
        printWeekHeader(w);
        if(w.memo().length() > 0) {
            System.out.println("Memo text:");
            System.out.print(w.memo());
        } else {
            info("No memo record found for this week.");
        }
    }

    /**
     * Set new weekly memo. Memo text is extracted from the command line
     * parameters.
     * @param w weekly object
     * @param args array of command line parameters
     * @param argIdx index of current command line parameter
     */
    private static void processCmdSetmemo(Weekly w, String[] args, int argIdx) throws IOException {
        if(!w.isEditable()) {
            System.err.println("Error: can edit only current weekly plan");
        } else {
            w.getEditor().setMemo((args.length == argIdx) ? null : args[argIdx]);
            w.sync();
        }
    }

    /**
     * Print list of known groups.
     */
    private static void processCmdGroups() {
        File defaultDbDir = new File(DatabaseConfig.instance().getDefaultDatabasePath());
        File[] dbList = defaultDbDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File arg0) {
                // choose only accessible directories
                return arg0.isDirectory() && arg0.canRead() && arg0.canWrite();
            }
        });
        if(0 == dbList.length) {
            info("No groups found.");
        } else {
            for(File f : dbList) {
                System.out.println(f.getName());
            }
        }
    }
    
    /**
     * Copy uncompleted tasks from previous week to the current one.
     */
    private static void processCmdCopyFromThePast() {
        try {
            Weekly thisWeek = new Weekly();
            if(thisWeek.size() != 0) {
                System.err.println("Error: current week is not empty, cannot copy from previous week");
            } else {
                Weekly previousWeek = new Weekly(thisWeek.startedOn().shift(-7));
                int cnt = 0;
                for(int i = 0; i < previousWeek.size(); ++i) {
                    TodoTask tt = previousWeek.taskAt(i);
                    if(!tt.isCompleted()) {
                        thisWeek.getEditor().addTask(tt.title());
                        ++cnt;
                    }
                }
                if(cnt == 0) {
                    info("Warning: no unfinished tasks found, none is copied");
                } else {
                    info("Info: " + cnt + " tasks were copied");
                    thisWeek.sync();
                }
            }
        } catch(IOException ex) {
            System.err.println("Error: database read/write unrecoverable error.");
        }
    }

    /**
     * Print info string to system out if verbose option is on.
     * @param msg string to be printed
     */
    private static void info(String msg) {
        if(verbose) {
            System.out.println(msg);
        }
    }

    // Verbose output flag
    private static boolean verbose = false;

    /**
     * Print utility title.
     */
    private static void printTitle() {
        System.out.println("This is Weekly Plans and Reports. Version " + VERSION);
        System.out.println("Written by " + AUTHOR + " (mailto:" + MAILTO +
            ") " + YEAR);
    }

    private static final String VERSION = "0.1cj";
    private static final String AUTHOR  = "Stas Torgashov";
    private static final String YEAR = "2011";
    private static final String MAILTO = "crimcat@yandex.ru";

    /**
     * Print utility help.
     */
    private static void printHelp() {
        System.out.println("Usage:");
        System.out.println("jwpr.console.jar [option] <command> [arguments]");
        System.out.println("Options are:");
        System.out.println("\t" + OPT_SETDATE + " <YYYY-MM-DD> - use the specified as current");
        System.out.println("\t" + OPT_DBDIR + " <directory> - specify the directory to store weekly databases");
        System.out.println("\t" + OPT_VERBOSE + " - be verbose about notifications in output");
        System.out.println("\t" + OPT_GROUP_SEL + " <group name> - specify the todo group to use");
        System.out.println("\t" + OPT_PREVIOUS_WEEK + " - select previous week instead of selecting a date");
        System.out.println("Commands are:");
        System.out.println("\t" + CMD_HELP + ": print this help");
        System.out.println("\t" + CMD_TODAY + ": print all active tasks created today");
        System.out.println("\t" + CMD_DAILY + ": print all today active tasks");
        System.out.println("\t" + CMD_WEEKLY + ": list all weekly tasks with their status");
        System.out.println("\t" + CMD_ADD + " <description>: create new today task with the description");
        System.out.println("\t" + CMD_COMPLETE + "<index>: mark task completed");
        System.out.println("\t" + CMD_SUMMARY + ": prepare weekly report");
        System.out.println("\t" + CMD_MEMO + ": show weekly memo");
        System.out.println("\t" + CMD_SETMEMO + " <memo text>: set new weekly memo");
        System.out.println("\t" + CMD_GROUPS + ": print groups list");
        System.out.println("\t" + CMD_COPY_FROM_THE_PAST + ": copy uncompleted tasks from previous week (works only if current week is empty)");
    }

    /**
     * Utility options.
     */
    private static final String OPT_SETDATE = "-d";
    private static final String OPT_DBDIR = "-b";
    private static final String OPT_VERBOSE = "-v";
    private static final String OPT_GROUP_SEL = "-g";
    private static final String OPT_PREVIOUS_WEEK = "-p";
}
