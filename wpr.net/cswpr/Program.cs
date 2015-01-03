using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.IO;

using wprlib;

namespace cswpr {
    class Program {
        static void Main(string[] args) {
            if(args.Length == 0) {
                System.Console.WriteLine("Command line options needed.");
                System.Console.WriteLine("Run with help command to see help information.");
            } else {
                List<string> cmdLineArgs = new List<string>(args);
                readGlobalConfig();
                if(readAndSetOptions(cmdLineArgs)) {
                    readAndExecuteCommand(cmdLineArgs);
                }
            }
        }

        internal static void cmdAdd(List<string> args) {
            Weekly w = getWeekly();
            if(!w.isEditable()) {
                System.Console.Error.WriteLine("Error: can edit only current weekly plan.");
            } else {
                if(args.Count != 2) {
                    System.Console.Error.WriteLine("Error: add command requires task description.");
                } else {
                    w.addTask(args[1]);
                    info("Task successfully created.");
                    w.sync();
                }
            }
        }

        internal static void cmdComplete(List<string> args) {
            Weekly w = getWeekly();
            if(!w.isEditable()) {
                System.Console.Error.WriteLine("Error: can edit only current weekly plan.");
            } else {
                if(args.Count != 2) {
                    System.Console.Error.WriteLine("Error: complete command requires task number.");
                } else {
                    int idx = Int32.Parse(args[1]);
                    if((idx <= 0) || (idx > w.numberOfTasks)) {
                        System.Console.Error.WriteLine("Error: cannot identify a task with the index of {0}", idx);
                    } else {
                        TodoTask tt = w[idx - 1];
                        if(tt.isCompleted) {
                            System.Console.Error.WriteLine("Error: cannot complete already completed task.");
                        } else {
                            tt.markCompleted();
                            info(String.Format("Task with id = {0} is completed.", idx));
                            w.sync();
                        }
                    }
                }
            }
        }

        internal static void cmdDaily(List<string> args) {
            Weekly w = getWeekly();
            TaskDate when = new TaskDate();
            int cnt = 0;
            for(int i = 0; i < w.numberOfTasks; ++i) {
                TodoTask tt = w[i];
                if(!tt.isCompleted && (tt.originatedOn.CompareTo(when) <= 0)) {
                    if(cnt++ == 0) {
                        System.Console.WriteLine("Daily ToDo up to " + when.ToString() + ":");
                    }
                    printTodoTaskItemNoStatus(i + 1, tt);
                }
            }
            if(cnt == 0) {
                info("No tasks found up to " + new TaskDate().ToString());
            }
        }

        internal static void cmdGroups(List<string> args) {
            var list = new List<string>();
            string dbPath = wprlib.AppDatabase.Gate.getFilesBundle(selectedDate).databaseDirPath;
            int cnt = 0;
            foreach(var dir in Directory.EnumerateDirectories(dbPath)) {
                System.Console.WriteLine(dir.Substring(dir.LastIndexOf(System.IO.Path.DirectorySeparatorChar) + 1));
                ++cnt;
            }
            if(0 == cnt) {
                info("No groups found.");
            }
        }

        internal static void cmdMemo(List<string> args) {
            Weekly w = getWeekly();
            printWeekHeader(w);
            if(w.memo.Length > 0) {
                System.Console.WriteLine("Memo text:");
                System.Console.WriteLine(w.memo);
            } else {
                info("No memo record found for this week.");
            }
        }

        internal static void cmdSetMemo(List<string> args) {
            if(args.Count != 2) {
                throw new Exception(CMD_SETMEMO + " requires 2 arguments");
            }

            Weekly w = getWeekly();
            if(w.isEditable()) {
                w.memo = args[1];
            } else {
                System.Console.Error.WriteLine("Error: can edit only current weekly plan.");
            }

            w.sync();
        }

        internal static void cmdSummary(List<string> args) {
            Weekly w = getWeekly();
            printWeekHeader(w);
            int cnt = 0;
            System.Console.WriteLine("- List of open items:");
            for(int i = 0; i < w.numberOfTasks; ++i) {
                TodoTask tt = w[i];
                if(!tt.isCompleted) {
                    printTodoTaskHeadLineWithId(i + 1, tt);
                    ++cnt;
                }
            }
            if(cnt == 0) {
                info("\tNo active tasks found.");
            }

            cnt = 0;
            System.Console.WriteLine("- List of completed items:");
            for(int i = 0; i < w.numberOfTasks; ++i) {
                TodoTask tt = w[i];
                if(tt.isCompleted) {
                    printTodoTaskHeadline(tt);
                    ++cnt;
                }
            }
            if(cnt == 0) {
                info("\tNo completed tasks found.");
            }
        }

        internal static void cmdToday(List<string> args) {
            Weekly w = getWeekly();
            TaskDate when = new TaskDate();
            int cnt = 0;
            for(int i = 0; i < w.numberOfTasks; ++i) {
                TodoTask tt = w[i];
                if(!tt.isCompleted && tt.originatedOn.Equals(when)) {
                    if(cnt++ == 0) {
                        System.Console.WriteLine("Today ToDo on " + when.ToString() + ":");
                    }
                    printTodoTaskItemNoStatus(i + 1, tt);
                }
            }
            if(cnt == 0) {
                info("No today plan on " + when.ToString());
            }
        }

        internal static void cmdWeekly(List<string> args) {
            Weekly w = getWeekly();
            printWeekHeader(w);
            if(w.numberOfTasks == 0) {
                info("No tasks found.");
            } else {
                for(int i = 0; i < w.numberOfTasks; ++i) {
                    printTodoTaskItem(i + 1, w[i]);
                }
            }
        }

        internal static Weekly getWeekly() {
            try {
                return option_groupsel
                    ? new Weekly(selectedDate, groupName)
                    : new Weekly(selectedDate);
            } catch(ChecksumException ex) {
                System.Console.WriteLine("Error: " + ex.ToString());
            }
            return null;
        }

        internal static void info(string msg) {
            if(option_verbose) {
                System.Console.WriteLine(msg);
            }
        }

        internal static void printTodoTaskItemNoStatus(int itenNo, TodoTask tt) {
            System.Console.WriteLine("[{0}] (id:{1,3}) {2}", tt.originatedOn, itenNo, tt.title);
        }

        internal static void printTodoTaskHeadline(TodoTask tt) {
            System.Console.WriteLine(String.Format("\t= {0}", tt.title));
        }

        internal static void printTodoTaskHeadLineWithId(int id, TodoTask tt) {
            System.Console.WriteLine(String.Format("\t= (id:{0,3}) {1}", id, tt.title));
        }

        internal static void printTodoTaskItem(int itemNo, TodoTask tt) {
            System.Console.WriteLine(
                String.Format("{0}. {1}|{2}: {3}",
                    itemNo,
                    tt.originatedOn.ToString(),
                    tt.isCompleted ? "DONE" : "WORK",
                    tt.title));
        }

        internal static void printWeekHeader(Weekly w) {
            System.Console.WriteLine(
                String.Format("Week {0} - {1} .. {2}:", 
                    w.startedOn.weekNumber,
                    w.startedOn.ToString(),
                    w.startedOn.shiftToNearestWeekDay(DayOfWeek.Sunday).ToString())
            );
        }

        internal static void printTitle() {
            System.Console.WriteLine("This is Weekly Plans and Reports. Version {0}.", STR_VERSION);
            System.Console.WriteLine("Written by {0} ({1}) {2}.", STR_AUTHOR, STR_MAILTO, STR_YEAR);
        }

        internal static void printHelp() {
            System.Console.WriteLine("Usage:");
            System.Console.WriteLine("cswpr.exe [options] <command> [arguments]");
            System.Console.WriteLine("Options are:");
            System.Console.WriteLine("\t{0} <YYYY-MM-DD> - use the specified as current", OPT_SETDATE);
            System.Console.WriteLine("\t{0} <directory> - specify the directory to store weekly databases", OPT_DBDIR);
            System.Console.WriteLine("\t{0} - be verbose about notifications in output", OPT_VERBOSE);
            System.Console.WriteLine("\t{0} <group name> - specify the todo group to use", OPT_GROUP_SEL);
            System.Console.WriteLine("\t{0} - select previous week instead of selecting a date", OPT_PREVIOUS_WEEK);
            System.Console.WriteLine("Commands are:");
            System.Console.WriteLine("\t{0} ({1}, {2}) : print this help and exit", CMD_HELP, CMD_HELP_CANONICAL, CMD_HELP_CANINOCAL_LONG);
            System.Console.WriteLine("\t{0} : print all active tasks created today", CMD_TODAY);
            System.Console.WriteLine("\t{0} : print all today active tasks", CMD_DAILY);
            System.Console.WriteLine("\t{0} : list all weekly tasks with their status", CMD_WEEKLY);
            System.Console.WriteLine("\t{0} <description> : create new today task with the description", CMD_ADD);
            System.Console.WriteLine("\t{0} <index> : mark task completed", CMD_COMPLETE);
            System.Console.WriteLine("\t{0} : prepare weekly report", CMD_SUMMARY);
            System.Console.WriteLine("\t{0} : show weekly memo", CMD_MEMO);
            System.Console.WriteLine("\t{0} <memo text> : set new weekly memo", CMD_SETMEMO);
            System.Console.WriteLine("\t{0} : print groups list", CMD_GROUPS);
            System.Console.WriteLine("\t{0} : copy uncompleted tasks from previous week (works only if current week is empty)", CMD_COPY_FROM_THE_PAST);
        }

        internal static void readGlobalConfig() {
            wprlib.AppDatabase.IAppConfig appCfg = wprlib.AppDatabase.Gate.getAppConfig();
            option_verbose = appCfg.doVerboseOutput;
        }

        internal static bool readAndSetOptions(List<string> args) {
            while(args.Count != 0) {
                string elem = args[0];
                if('-' == elem[0]) {
                    switch(elem) {
                    case OPT_SETDATE:
                        args.RemoveAt(0);
                        if(args.Count != 0) {
                            option_setdate = true;
                            selectedDate = new TaskDate(args[0]);
                        }
                        break;
                    case OPT_DBDIR:
                        args.RemoveAt(0);
                        if(args.Count != 0) {
                            if(!wprlib.AppDatabase.Gate.setDefaultDatabasePath(args[0])) {
                                System.Console.Error.WriteLine("Error: cannot set database path, the directory does not exist.");
                                return false;
                            }
                        }
                        break;
                    case OPT_VERBOSE:
                        option_verbose = true;
                        break;
                    case OPT_GROUP_SEL:
                        args.RemoveAt(0);
                        if(args.Count != 0) {
                            option_groupsel = true;
                            groupName = args[0];
                        }
                        break;
                    case OPT_PREVIOUS_WEEK:
                        option_prevweek = true;
                        break;
                    default:
                        System.Console.WriteLine("Unknown option: {0}", elem);
                        System.Console.WriteLine("Run with help command to see help information.");
                        return false;
                    }
                    // Remove current argument
                    args.RemoveAt(0);
                } else {
                    break;
                }
            }
            // reset values
            if(null == selectedDate) {
                selectedDate = new TaskDate();
            }
            return validateOptions();
        }

        internal static bool validateOptions() {
            if(option_prevweek && option_setdate) {
                System.Console.WriteLine("Error: cannot use {0} and {1} at the same time.", OPT_SETDATE, OPT_PREVIOUS_WEEK);
                return false;
            }
            return true;
        }

        internal static void readAndExecuteCommand(List<string> args) {
            if(args.Count == 0) {
                System.Console.WriteLine("Error: no command specified.");
                return;
            }

            switch(args[0]) {
            case CMD_HELP:
            case CMD_HELP_CANINOCAL_LONG:
            case CMD_HELP_CANONICAL:
                printTitle();
                printHelp();
                break;
            case CMD_ADD:
                cmdAdd(args);
                break;
            case CMD_COMPLETE:
                cmdComplete(args);
                break;
            case CMD_DAILY:
                cmdDaily(args);
                break;
            case CMD_GROUPS:
                cmdGroups(args);
                break;
            case CMD_MEMO:
                cmdMemo(args);
                break;
            case CMD_SETMEMO:
                cmdSetMemo(args);
                break;
            case CMD_SUMMARY:
                cmdSummary(args);
                break;
            case CMD_TODAY:
                cmdToday(args);
                break;
            case CMD_WEEKLY:
                cmdWeekly(args);
                break;
            default:
                System.Console.WriteLine("Unknown command \'{0}\' specified.", args[0]);
                string possibleCommand = ddict.findNearest(args[0]);
                if(null != possibleCommand) {
                    System.Console.WriteLine("Did you mean \'{0}\'?", possibleCommand);
                }
                System.Console.WriteLine("Run with help command to see help information.");
                return;
            }
        }

        // Commands
        private const string CMD_HELP = "help";
        private const string CMD_HELP_CANONICAL = "-h";
        private const string CMD_HELP_CANINOCAL_LONG = "--help";
        private const string CMD_TODAY = "today";
        private const string CMD_DAILY = "daily";
        private const string CMD_WEEKLY = "weekly";
        private const string CMD_ADD = "add";
        private const string CMD_COMPLETE = "complete";
        private const string CMD_SUMMARY = "summary";
        private const string CMD_MEMO = "memo";
        private const string CMD_SETMEMO = "set-memo";
        private const string CMD_GROUPS = "groups";
        private const string CMD_COPY_FROM_THE_PAST = "copy-from-the-past";

        private static cswpr.utils.DistanceDictionary ddict = new utils.DistanceDictionary(
            new string[] {
                CMD_HELP,
                CMD_HELP_CANONICAL,
                CMD_HELP_CANINOCAL_LONG,
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
            utils.WordDistanceCalculators.LEVENSTEIN
        );
        private static TaskDate selectedDate = null;
        private static string groupName = null;

        // Option flags
        private static bool option_setdate = false;
        private static bool option_verbose = false;
        private static bool option_groupsel = false;
        private static bool option_prevweek = false;

        // Options codes
        private const string OPT_SETDATE = "-d";
        private const string OPT_DBDIR = "-b";
        private const string OPT_VERBOSE = "-v";
        private const string OPT_GROUP_SEL = "-g";
        private const string OPT_PREVIOUS_WEEK = "-p";

        // application info
        private const string STR_VERSION = "0.2dnet";
        private const string STR_AUTHOR = "Stas Torgashov";
        private const string STR_YEAR = "2014";
        private const string STR_MAILTO = "mailto:stas.torgashov@outlook.com";
    }
}
