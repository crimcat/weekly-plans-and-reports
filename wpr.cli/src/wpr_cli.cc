/*
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Library General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

/* File:    wpr_cli.cc
 * Author:  Stas Torgashov aka Crimson Cat (crimcat@yandex.ru)
 * Created: 2011, February 23
 */

#include "libwpr.h"

#include <getopt.h>

#include <iostream>
#include <string>
#include <string.h>
#include <sstream>
#include <cstdlib>

#include <sys/types.h>
#include <sys/stat.h>
#include <dirent.h>
#include <unistd.h>

#include <errno.h>

/**
 * Print application title.
 */
void
print_title();

/**
 * Print application help.
 */
void
print_help(int argc, char *argv[]);

/**
 * getopt data structure,
 */
static struct option long_options[] = {
    { "date",          1, 0, 'd' },
    { "database-path", 1, 0, 'b' },
    { "previous-week", 0, 0, 'p' },
    { "group",         1, 0, 'g' },
    { "verbose",       0, 0, 'v' },
    { 0,               0, 0,  0  }
};

/**
 * Command constants are below. Used to identify command line commands.
 */
static const char *CMD_HELP = "help";
static const char *CMD_WEEKLY = "weekly";
static const char *CMD_TODAY = "today";
static const char *CMD_DAILY = "daily";
static const char *CMD_MEMO = "memo";
static const char *CMD_SET_MEMO = "set-memo";
static const char *CMD_COMPLETE = "complete";
static const char *CMD_ADD = "add";
static const char *CMD_SUMMARY = "summary";
static const char *CMD_GROUPS = "groups";
static const char *CMD_COPY_FROM_THE_PAST = "copy-from-the-past";

/**
 * Print weekly object header (week number, its ranges, etc).
 * @see wpr::weekly
 * @param w weekly object reference
 */
void
print_week_header(const wpr::weekly &w);

/**
 * Get default storage path in user home.
 * @return string with user home dir path
 */
std::string
get_default_storage_path();

/**
 * Pretty printing.
 * @param os reference to the output stream
 * @param d date object
 * @return reference to the output stream to be chained
 */
std::ostream &
operator<<(std::ostream &os, const wpr::date &d);
/**
 * Pretty printing.
 * @param os reference to the output stream
 * @param tt task object to be printed
 * @return reference to the output stream to be chained
 */
std::ostream &
operator<<(std::ostream &os, const wpr::todo_task &tt);

// global flag - verbose output
static bool verbose = false;

/**
 * Print weekly summary,
 * @param w weekly object to be printed to the STDOUT
 */
void
print_summary(const wpr::weekly &w);

/**
 * Create weekly schedule object (should be manually destroyed after use)
 * @param workdir working directory path
 * @param when date selecting this week
 * @return pointer to the created object or null if failed
 */
wpr::weekly *
create_weekly(const std::string &workdir, const wpr::date &when);

/**
 * Main,
 * @param argc number of supplied command line arguments
 * @param argv array of arguments
 * @return return value
 */
int
main(int argc, char *argv[]) {
    std::string wdir(get_default_storage_path());
    wpr::date the_date; // today by default
    std::string group_name;
    bool date_selected = false;

    while(true) {
        int option_index = 0;
        int c = getopt_long(argc, argv, "d:b:pg:v", long_options, &option_index);
        if(-1 == c) break;
        switch(c) {
        case 'd':
        {
            if(date_selected) {
                std::cerr << "Error: the date specified several times.\n";
                return -1;
            }

            std::stringstream buf(optarg);
            if(!the_date.read(&buf) || (buf.get() && !buf.eof())) {
                std::cerr << "Error: cannot parse the date of " << optarg << "\n";
                return -1;
            }
            if(wpr::date() < the_date) {
                std::cerr << "Error: now we do not deal with the future in this app - "
                    << the_date << "\n";
                return -1;
            }
            date_selected = true;
        }
        break;
        case 'b':
            wdir = std::string(optarg);
            break;
        case 'p':
            if(date_selected) {
                std::cerr << "Error: the date specified several times.\n";
                return -1;
            }
            the_date.shift_to(wpr::date::MONDAY);
            the_date.shift(-1);
            date_selected = true;
            break;
        case 'g':
            group_name = std::string(optarg);
            break;
        case 'v':
            verbose = true;
            break;
        default:
            std::cerr << "Error parsing command line.\n";
            return 1;
        }
    }
    if(optind >= argc) {
        std::cerr << "No command specified.\n"
            << "Run with help command to read help information.\n";
    } else {
        const char *cmd = argv[optind++];
        if(!strcmp(cmd, CMD_HELP)) {
            print_title();
            print_help(argc, argv);
            std::cout << "\n";
            return 0;
        }

        // determine the full db directory path if group specified
        if(group_name.length()) {
            wdir += "/" + group_name;

            struct stat sb;
            if(-1 == stat(wdir.c_str(), &sb)) {
                if(ENOENT == errno) {
                    // create directory here
                    if(mkdir(wdir.c_str(), 0755)) {
                        std::cerr << "Error: cannot create database for the specified group.\n";
                        return -1;
                    }
                } else {
                    std::cerr << "Error: cannot access database for the specified group\n";
                }
            } else {
                // check access rights here
                if(!S_ISDIR(sb.st_mode) ||
                   !(sb.st_mode & S_IRUSR) ||
                   !(sb.st_mode & S_IWUSR) ||
                   !(sb.st_mode & S_IXUSR)) {
                    std::cerr << "Error: cannot find database for the specified group\n";
                    return -1;
                }
            }
        }

        wpr::weekly *w = 0;
        if(!strcmp(cmd, CMD_WEEKLY)) {
            w = create_weekly(wdir, the_date);
            if(w) {
                print_week_header(*w);

                int cnt = 0;
                for(size_t i = 0; i < w->size(); ++i) {
                    std::cout << (i + 1) << ". " << w->at(i) << "\n";
                    ++cnt;
                }
                if(!cnt && verbose) {
                    std::cout << "Nothing found.\n";
                }
            }
        } else if(!strcmp(cmd, CMD_TODAY)) {
            w = create_weekly(wdir, the_date);
            if(w) {
                bool first_found = false;
                int cnt = 0;
                for(size_t i = 0; i < w->size(); ++i) {
                    if(!w->at(i).is_completed() && (the_date == w->at(i).originated_on())) {
                        if(!first_found) {
                            std::cout << "Active tasks scheduled on " << the_date << ":\n";
                            first_found = true;
                        }

                        std::cout << (i + 1) << ". "
                                  << w->at(i).originated_on() << ": "
                                  << w->at(i).description() << "\n";
                        ++cnt;
                    }
                }
                if(!cnt && verbose) {
                    std::cout << "No active tasks found for " << the_date.to_string() << ".\n";
                }
            }
        } else if(!strcmp(cmd, CMD_DAILY)) {
            w = create_weekly(wdir, the_date);
            if(w) {
                bool first_found = false;
                int cnt = 0;
                for(size_t i = 0; i < w->size(); ++i) {
                    if(!w->at(i).is_completed() && (w->at(i).originated_on() <= the_date)) {
                        if(!first_found) {
                            std::cout << "Proposed todo plan up to " << the_date << ":\n";
                            first_found = true;
                        }

                        std::cout << (i + 1) << ". "
                            << w->at(i).originated_on() << ": "
                            << w->at(i).description() << "\n";
                        ++cnt;
                    }
                }
                if(!cnt && verbose) {
                    std::cout << "No active tasks found up to " << the_date.to_string()
                              << ".\n";
                }
            }
        } else if(!strcmp(cmd, CMD_MEMO)) {
            w = create_weekly(wdir, the_date);
            if(w) {
                print_week_header(*w);
                if(!w->memo().length()) {
                    if(verbose) {
                        std::cout << "No memo record found for this week.\n";
                    }
                } else {
                    std::cout << "Memo text:\n" << w->memo() << "\n";
                }
            }
        } else if(!strcmp(cmd, CMD_SET_MEMO)) {
            const char *memo_text = optind < argc
                ? argv[optind++]
                : "";
            w = create_weekly(wdir, the_date);
            if(w) {
                if(!w->is_editable()) {
                    std::cerr << "Error: cannot edit weeklies in the past.\n";
                } else {
                    w->get_editor()->set_memo(std::string(memo_text));
                    if(verbose) {
                        std::cout << "Memo recorded.\n";
                    }
                }
            }
        } else if(!strcmp(cmd, CMD_COMPLETE)) {
            if(optind == argc) {
                std::cerr << "Error: operation requires task id number.\n";
                return -1;
            }
            const char *task_id = argv[optind++];
            w = create_weekly(wdir, the_date);
            if(w) {
                if(!w->is_editable()) {
                    std::cerr << "Error: cannot edit weeklies in the past.\n";
                } else {
                    int task_num = std::atoi(task_id);
                    if((task_num < 1) || ((size_t)task_num > w->size())) {
                        std::cerr << "Error: wrong task id number.\n";
                    } else {
                        if(w->at(task_num - 1).is_completed()) {
                            std::cerr << "Error: cannot complete already completed task (id = "
                                << task_num << ")\n";
                        } else {
                            w->get_editor()->at(task_num - 1).complete_it();
                            if(verbose) {
                                std::cout << "Task with id=" << task_num << " is marked completed.\n";
                            }
                        }
                    }
                }
            }
        } else if(!strcmp(cmd, CMD_ADD)) {
            if(optind == argc) {
                std::cerr << "Error: operation requires task description.\n";
                return -1;
            }
            const char *descr = argv[optind++];
            w = create_weekly(wdir, the_date);
            if(w) {
                if(!w->is_editable()) {
                    std::cerr << "Error: cannot edit weeklies in the past.\n";
                } else {
                    w->get_editor()->add_task(std::string(descr));
                    if(verbose) {
                        std::cout << "New task successfully added.\n";
                    }
                }
            }
        } else if(!strcmp(cmd, CMD_SUMMARY)) {
            w = create_weekly(wdir, the_date);
            if(w) {
                print_summary(*w);
            }
        } else if(!strcmp(cmd, CMD_GROUPS)) {
            DIR *dbdir = opendir(wdir.c_str());
            if(!dbdir) {
                std::cerr << "Error: cannot read database directory - " << wdir << "\n";
                return -1;
            }
            int groups_cnt = 0;
            struct dirent *direntry = readdir(dbdir);
            while(direntry) {
                if(DT_DIR == direntry->d_type) {
                    if(strcmp(".", direntry->d_name) &&
                       strcmp("..", direntry->d_name)) {
                        std::cout << direntry->d_name << "\n";
                        ++groups_cnt;
                    }
                }
                direntry = readdir(dbdir);
            }
            closedir(dbdir);
            if(verbose && !groups_cnt) {
                std::cout << "No groups found.\n";
            }
        } else if(!strcmp(cmd, CMD_COPY_FROM_THE_PAST)) {
            if(date_selected) {
                std::cerr << "Warning: -p option ignored, not applicable.\n";
                the_date = wpr::date();
                the_date.shift_to(wpr::date::MONDAY);
            }
            w = create_weekly(wdir, the_date);
            if(w->size()) {
                std::cerr << "Error: current week is not empty, cannot copy.\n";
            } else {
                the_date.shift(-7); // move to the previous Monday
                wpr::weekly prev(wdir, the_date); // get previous week schedule
                size_t cnt = 0;
                for(size_t i = 0; i < prev.size(); ++i) {
                    const wpr::todo_task &tt = prev[i];
                    if(!tt.is_completed()) {
                        w->get_editor()->add_task(tt.description());
                        ++cnt;
                    }
                }
                if(verbose) {
                    if(!cnt) {
                        std::cerr << "Warning: no unfinished tasks found, nothing is copied.\n";
                    } else {
                        std::cerr << cnt << " tasks have been copied.\n";
                    }
                }
            }
        } else {
            std::cerr << "Error: unknown command " << cmd << "\n";
            return -1;
        }
        if(w) {
            delete w;
        }
        if(optind != argc) {
            std::cerr << "Warning: supplied extra commands or parameters are ignored.\n";
        }
    }
    return 0;
}

void
print_week_header(const wpr::weekly &w) {
    wpr::date end_of_week(w.starting_date());
    end_of_week.shift(6);
    std::cout << "Week " << w.starting_date().week_number()
              << " - " << w.starting_date() << "..." << end_of_week << ":\n";
}

std::string
get_default_storage_path() {
    const char *userhome = std::getenv("HOME");
    if(!userhome) {
        userhome = ".";
    }
    return std::string(userhome) + "/.wpr/";
}

void
print_title() {
    std::cout << "This is Weekly Plans & Reports. Version 0.1c\n";
    std::cout << "Written by Stas Torgashov (mailto:crimcat@yandex.ru)\n";
}

void
print_help(int argc, char *argv[]) {
    std::cout << "Usage:\n" << argv[0] << " [options] command [parameters]\n";
    std::cout << "Options are:\n"
        << "\t-d, --date <YYYY-MM-DD> - specify date (current or in the past)\n"
        << "\t-b, --database-path <path> - specify a folder path to store weeklies database\n"
        << "\t-p, --previous-week - select a week before instead of selecting a date\n"
        << "\t-g, --group <group name> - specify weekly group to use\n"
        << "\t-v, --verbose - be verbose about additional information in output\n";
    std::cout << "-d/--date option is used in commands to specify the nearest target date "
        "where the desired weekly or daily information is requested,\nit's not used in modification "
        "commands which are applicable only for the current week.\n";
    std::cout << "-b/--database-path option specifies the directory path to store weekly "
        "databases; if this directory does not exist it's created.\n";
    std::cout << "Commands are:\n"
        << "\t" << CMD_HELP << ": print this help\n"
        << "\t" << CMD_WEEKLY << ": print all weekly todo tasks\n"
        << "\t" << CMD_DAILY << ": print all active tasks\n"
        << "\t" << CMD_TODAY << ": print active today tasks\n"
        << "\t" << CMD_MEMO << ": print memo\n"
        << "\t" << CMD_SET_MEMO << " <memo text>: update memo with the given text\n"
        << "\t" << CMD_COMPLETE << " <task no>: set task with the given number completed\n"
        << "\t" << CMD_ADD << " <title>: add new task for today with the given title\n"
        << "\t" << CMD_SUMMARY << ": generate weekly summary\n"
        << "\t" << CMD_GROUPS << ": print groups list\n"
        << "\t" << CMD_COPY_FROM_THE_PAST << ": copy unfinished tasks from the previous week\n";
    std::cout << "\'" << CMD_SET_MEMO << "\', \'" << CMD_COMPLETE << "\', \'" << CMD_ADD
        << "\', \'" << CMD_COPY_FROM_THE_PAST
        << "\' commands modify database and so they can be used only for the current week\n";
}

std::ostream &
operator<<(std::ostream &os, const wpr::date &d) {
    d.write(&os);
    return os;
}

std::ostream &
operator<<(std::ostream &os, const wpr::todo_task &tt) {
    tt.originated_on().write(&os);
    os << "|" << (tt.is_completed() ? "DONE" : "WORK")
       << ": " << tt.description();
    return os;
}

void
print_summary(const wpr::weekly &w) {
    wpr::date now;
    int cnt = 0;
    if(!w.is_editable()) {
        now = w.starting_date();
        now.shift(6);
    }
    print_week_header(w);
    std::cout << "Completed tasks:\n";
    for(size_t i = 0; i < w.size(); ++i) {
        if(w[i].is_completed()) {
            ++cnt;
            std::cout << "- " << w[i].description() << "\n";
        }
    }
    if(!cnt && verbose) {
        std::cout << "  No completed tasks found.\n";
    }
    cnt = 0;
    std::cout << "Uncompleted tasks or opportunities:\n";
    for(size_t i = 0; i < w.size(); ++i) {
        if(!w[i].is_completed()) {
            ++cnt;
            std::cout << "- " << w[i].description() << "\n";
        }
    }
    if(!cnt && verbose) {
        std::cout << "  No active tasks found.\n";
    }
}

wpr::weekly *
create_weekly(const std::string &workdir, const wpr::date &when) {
    wpr::weekly *w = new wpr::weekly(workdir, when);
    if(w->is_valid()) {
        return w;
    }
    delete w;
    std::cerr << "Error: cannot load database from " << workdir
        << " for " << when << "\n";
    std::cerr << "Try to check manually .todolist file, or remove"
        " .checksum or .memo file for this date to let the application"
        " fix the issue by itself.\n";
    return 0;
}
