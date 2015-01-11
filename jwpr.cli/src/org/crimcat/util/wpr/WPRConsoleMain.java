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

/* File:    WPRConsoleMain.java
 * Author:  Stas Torgashov aka Crimson Cat (crimcat@yandex.ru)
 * Created: 2011, March 25
 */

package org.crimcat.util.wpr;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.crimcat.lib.wpr.AppDatabase;

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
        if(0 == args.length) {
            System.err.println("No commands specified.");
            System.err.println("Run with help command to see help information.");
        } else {
            AppDatabase.Configuration configuration = AppDatabase.getAppConfig();
            opt_verbose = configuration.doVerboseOuput();
            opt_do_copy_on_mondays = configuration.doCopyFromThePastOnMondays();
            
            List<String> argList = Arrays.asList(args);
            if(readOptions(argList)) {
                if(validateOptions()) {
                    try {
                        Weekly weekly = opt_groups
                            ? new Weekly(selectedDate, groupName)
                            : new Weekly(selectedDate);
                        if(opt_do_copy_on_mondays) {
                            if(weekly.isEditable() && (0 == weekly.size()) &&
                               (TaskDate.WeekDay.MONDAY == selectedDate.weekDay())) {
                                info("Info: copying items from previous week.");
                                processCmdCopyFromThePast(weekly);
                            }
                        }
                        readAndExecuteCommand(argList, weekly);
                    } catch(IOException ex) {
                        System.err.println("Error: cannot open or process weekly database.");
                        System.err.println(ex.toString());
                    }
                }
            } else {
                System.err.println("Unknown option or insufficient parameters. See help for information.");
            }
        }
    }

    private static boolean readOptions(List<String> args) {
        for(String arg : args) {
            if(arg.charAt(0) == '-') {
                switch(args.remove(0)) {
                    case OPT_DBDIR:
                        if(0 == args.size()) return false;
                        if(!AppDatabase.setDefaultAppDatabasePath(args.remove(0))) {
                            System.err.println("Error: cannot use provided database path in " + OPT_DBDIR + " option.");
                            return false;
                        }
                        break;
                    case OPT_GROUP_SEL:
                        if(0 == args.size()) return false;
                        opt_groups = true;
                        groupName = args.remove(0);
                        break;
                    case OPT_PREVIOUS_WEEK:
                        opt_prev_week = true;
                        selectedDate.shiftToWeekDay(WeekDay.MONDAY).shift(-1);
                        break;
                    case OPT_SETDATE:
                        if(0 == args.size()) return false;
                        opt_date_selection = true;
                        if(!selectedDate.fromString(args.remove(0))) {
                            System.err.println("Error: cannot parse date for " + OPT_SETDATE + " option.");
                            return false;
                        }
                        break;
                    case OPT_VERBOSE:
                        opt_verbose = true;
                        break;
                    default:
                        return false;
                }
            } else break;
        }
        return true;
    }
    
    /**
     * Validate collected options. Should be called after <code>readOptions</code>.
     * @return true if options are set correctly, otherwise it has false
     */
    private static boolean validateOptions() {
        if(opt_prev_week && opt_date_selection) {
            System.err.println("Error: cannot use " + OPT_PREVIOUS_WEEK + " and " + OPT_SETDATE + " at the same time.");
            return false;
        }
        return true;
    }
    
    private static void readAndExecuteCommand(List<String> args, Weekly weekly) throws IOException {
        String expectedCmd = args.remove(0);
        switch(expectedCmd) {
            case CMD_ADD:
                processCmdAdd(weekly, args);
                break;
            case CMD_COMPLETE:
                processCmdComplete(weekly, args);
                break;
            case CMD_COPY_FROM_THE_PAST:
                processCmdCopyFromThePast(weekly);
                break;
            case CMD_DAILY:
                processCmdDaily(weekly);
                break;
            case CMD_GROUPS:
                processCmdGroups();
                break;
            case CMD_HELP:
                processCmdHelp();
                break;
            case CMD_HELP_CANINOCAL_LONG:
                processCmdHelp();
                break;
            case CMD_HELP_CANONICAL:
                processCmdHelp();
                break;
            case CMD_MEMO:
                processCmdMemo(weekly);
                break;
            case CMD_SETMEMO:
                processCmdSetmemo(weekly, args);
                break;
            case CMD_SUMMARY:
                processCmdSummary(weekly);
                break;
            case CMD_TODAY:
                processCmdToday(weekly);
                break;
            case CMD_WEEKLY:
                processCmdWeekly(weekly);
                break;
            default: {
                System.err.println("Unknown command: \'" + expectedCmd + "\'.");
                String possibleCmd = distanceDict.findNearest(expectedCmd);
                if(possibleCmd != null) {
                    System.err.println("Did you mean \'" + possibleCmd + "\'?");
                }
                break;
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
     * Print todo task headline: only task title with prompt
     * @param tt todo task object
     */
    private static void printTodoTaskHeadline(TodoTask tt) {
        System.out.println("\t= " + tt.title());
    }
    
    /**
     * Print todo task headline: only task title with id and prompt
     * @param id todo task id number
     * @param tt todo task object
     */
    private static void printTodoTaskHeadLineWithId(int id, TodoTask tt) {
        System.out.println(String.format("\t= (id:%3d) %s", id, tt.title()));
    }

    /**
     * Print todo task item as @see printTodoTaskItem does, but without
     * task status: number, originated date and task title.
     * @param itemNo number of task
     * @param tt todo task object
     */
    private static void printTodoTaskItemNoStatus(int itemNo, TodoTask tt) {
        System.out.println(String.format("[%s] (id:%3d) %s", tt.originatedOn(), itemNo, tt.title()));
    }

    /**
     * Predefined commands.
     */
    private static final String CMD_HELP = "help";
    private static final String CMD_HELP_CANONICAL = "-h";
    private static final String CMD_HELP_CANINOCAL_LONG = "--help";
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
    
    private static final DistanceDictionary distanceDict = new DistanceDictionary(
        new String[] {
            CMD_HELP,
            CMD_TODAY,
            CMD_DAILY,
            CMD_WEEKLY,
            CMD_ADD,
            CMD_COMPLETE,
            CMD_SUMMARY,
            CMD_MEMO,
            CMD_SETMEMO,
            CMD_GROUPS,
            CMD_COPY_FROM_THE_PAST
        },
        WordsDistanceCalculators.LEVENSTEIN
    );

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
    private static void processCmdToday(Weekly w) {
        int cnt = 0;
        for(int i = 0; i < w.size(); ++i) {
            TodoTask tt = w.taskAt(i);
            if(!tt.isCompleted() && tt.originatedOn().equals(selectedDate)) {
                if(0 == cnt++) {
                    System.out.println("Today ToDo on " + selectedDate.toString() + ":");
                }
                printTodoTaskItemNoStatus(i + 1, tt);
            }
        }
        if(0 == cnt) {
            info("No today plan on " + selectedDate.toString());
        }
    }

    /**
     * Print proposed daily task list: all active task by today.
     * @param w weekly object
     * @param when date which is 'today'
     */
    private static void processCmdDaily(Weekly w) {
        int cnt = 0;
        for(int i = 0; i < w.size(); ++i) {
            TodoTask tt = w.taskAt(i);
            if(!tt.isCompleted() && (tt.originatedOn().compare(selectedDate) <= 0)) {
                if(0 == cnt++) {
                    System.out.println("Daily ToDo up to " + selectedDate.toString() + ":");
                }
                printTodoTaskItemNoStatus(i + 1, tt);
            }
        }
        if(0 == cnt) {
            info("No tasks found up to " + selectedDate.toString());
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
    private static void processCmdAdd(Weekly w, List<String> args) throws IOException {
        if(!w.isEditable()) {
            System.err.println("Error: can edit only current weekly plan.");
        } else {
            if(args.isEmpty()) {
                System.err.println("Error: " + CMD_ADD + " command required task description.");
            } else {
                w.getEditor().addTask(args.remove(0));
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
    private static void processCmdComplete(Weekly w, List<String> args) throws IOException {
        if(!w.isEditable()) {
            System.err.println("Error: can edit only current weekly plan.");
        } else {
            if(args.isEmpty()) {
                System.err.println("Error: " + CMD_COMPLETE + " operation requires task index number.");
            } else {
                String taskIdxStr = args.remove(0);
                try {
                    int idx = Integer.parseInt(taskIdxStr);
                    if((idx <= 0) || (idx > w.size())) {
                        System.err.println("Error: cannot identify a task with the index - " + taskIdxStr);
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
                    System.err.println("Error: " + CMD_COMPLETE + " command cannot parse index value of " + taskIdxStr);
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
        System.out.println("- List of open items:");
        for(int i = 0; i < w.size(); ++i) {
            TodoTask tt = w.taskAt(i);
            if(!tt.isCompleted()) {
                printTodoTaskHeadLineWithId(i + 1, tt);
                ++cnt;
            }
        }
        if(0 == cnt) {
            info("  No active tasks found.");
        }
        
        cnt = 0;
        System.out.println("- List of completed items:");
        for(int i = 0; i < w.size(); ++i) {
            TodoTask tt = w.taskAt(i);
            if(tt.isCompleted()) {
                printTodoTaskHeadline(tt);
                ++cnt;
            }
        }
        if(0 == cnt) {
            info("\tNo completed tasks found.");
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
    private static void processCmdSetmemo(Weekly w, List<String> args) throws IOException {
        if(!w.isEditable()) {
            System.err.println("Error: can edit only current weekly plan.");
        } else {
            if(args.size() == 0) {
                System.err.println("Error: " + CMD_SETMEMO + " command needs argument of memo text.");
            } else {
                w.getEditor().setMemo(args.remove(0));
                w.sync();
            }
        }
    }

    /**
     * Print list of known groups.
     */
    private static void processCmdGroups() {
        File defaultDbDir = new File(AppDatabase.getDefaultAppDatabasePath().toString());
        File[] dbList = defaultDbDir.listFiles(
            (File item) -> item.isDirectory() && item.canRead() && item.canWrite()
        );
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
    private static void processCmdCopyFromThePast(Weekly thisWeek) throws IOException {
        if(thisWeek.size() != 0) {
            System.err.println("Error: current week is not empty, copying from previous week is not possible.");
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
                info("Warning: no unfinished tasks found, none is copied.");
            } else {
                info("Info: " + cnt + " tasks were copied.");
                thisWeek.sync();
            }
        }
    }

    /**
     * Print info string to system out if verbose option is on.
     * @param msg string to be printed
     */
    private static void info(String msg) {
        if(opt_verbose) {
            System.out.println(msg);
        }
    }

    /**
     * Print utility title.
     */
    private static void printTitle() {
        System.out.println("This is Weekly Plans and Reports. Version " + VERSION);
        System.out.println("Written by " + AUTHOR + " (mailto:" + MAILTO + ") " + YEAR);
    }

    private static boolean opt_verbose = false;
    private static boolean opt_do_copy_on_mondays = false;
    
    private static boolean opt_groups = false;
    private static String groupName = null;
    
    private static boolean opt_date_selection = false;
    
    private static boolean opt_prev_week = false;
    
    private final static TaskDate selectedDate = new TaskDate();
    
    private static final String VERSION = "0.3cj";
    private static final String AUTHOR  = "Stas Torgashov";
    private static final String YEAR = "2011-2015";
    private static final String MAILTO = "stas.torgashov@outlook.com";

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
        System.out.println("\t" + CMD_HELP + " (" + CMD_HELP_CANONICAL + ", " + CMD_HELP_CANINOCAL_LONG
            + ") : print this help and exit");
        System.out.println("\t" + CMD_TODAY + " : print all active tasks created today");
        System.out.println("\t" + CMD_DAILY + " : print all today active tasks");
        System.out.println("\t" + CMD_WEEKLY + " : list all weekly tasks with their status");
        System.out.println("\t" + CMD_ADD + " <description> : create new today task with the description");
        System.out.println("\t" + CMD_COMPLETE + "<index> : mark task completed");
        System.out.println("\t" + CMD_SUMMARY + " : prepare weekly report");
        System.out.println("\t" + CMD_MEMO + " : show weekly memo");
        System.out.println("\t" + CMD_SETMEMO + " <memo text> : set new weekly memo");
        System.out.println("\t" + CMD_GROUPS + " : print groups list");
        System.out.println("\t" + CMD_COPY_FROM_THE_PAST + " : copy uncompleted tasks from previous week (works only if current week is empty)");
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
