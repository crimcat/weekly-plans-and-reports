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

/* File:    tasks.cc
 * Author:  Stas Torgashov aka Crimson Cat (mailto:crimcat@yandex.ru)
 * Created: 2011, January 28
 */


#include "libwpr.h"
#include "utils.h"

#include <assert.h>
#include <iostream>
#include <fstream>
#include <sstream>

namespace wpr {

    /** todo_task */

    todo_task::todo_task(const std::string &task_description)
        : date_originated()
        , completed(false)
        , descr(task_description) {
    }

    bool todo_task::is_completed() const {
        return completed;
    }

    const date &
    todo_task::originated_on() const {
        return date_originated;
    }

    const std::string &
    todo_task::description() const {
        return descr;
    }

    void
    todo_task::complete_it() {
        completed = true;
    }

    bool
    todo_task::write(std::ostream *os) const {
        if(os && os->good()) {
            date_originated.write(os);
            (*os) << ':'
                << (completed ? 'C' : 'A') << ':'
                << descr;
        }
        return false;
    }

    bool
    todo_task::read(std::istream *is) {
        date creation_date;
        std::string status, title;
        if(creation_date.read(is) &&
           parser::expect(is, 1, NULL, parser::is_colon) &&
           parser::expect(is, 1, &status, parser::is_char) &&
           parser::expect(is, 1, NULL, parser::is_colon) &&
           parser::readline(is, &title)) {
            switch(status[0]) {
            case 'C': completed = true; break;
            case 'A': completed = false; break;
            default: return false;
            }
            date_originated = creation_date;
            descr.swap(title);
            return true;
        }
        return false;
    }

    /** weekly */

    static const std::string EXT_TODOLIST(".todolist");
    static const std::string EXT_MEMO(".memo");
    static const std::string EXT_CHECKSUM(".checksum");

    struct weekly::impl : public weekly::editor {
        date monday;
        std::string memo_string;
        std::vector<todo_task> tasks;
        bool is_changed;
        std::string working_dir;

        impl(const std::string &wd)
            : is_changed(false)
            , working_dir(wd) {
            monday.shift_to(date::MONDAY);
            fs::adjust_and_setup_working_dir(&working_dir);
        }

        explicit impl(const std::string &wd, const date &d)
            : monday(d)
            , is_changed(false)
            , working_dir(wd) {
            monday.shift_to(date::MONDAY);
            fs::adjust_and_setup_working_dir(&working_dir);
        }

        virtual ~impl() {
            if(is_changed) {
                save();
            }
        }

        bool load() {
            std::string basename = working_dir + monday.to_string();
            std::string cksum_filename = basename + EXT_CHECKSUM;
            std::string todo_filename = basename + EXT_TODOLIST;
            std::string memo_filename = basename + EXT_MEMO;

            // load and verify checksum if the file is present
            // if file does not exist - not an error, just proceed further
            std::ifstream cksum_is(cksum_filename.c_str(), std::ios::in);
            fs::checksum_t loaded_cksum = 0;
            if(cksum_is.good()) {
                cksum_is >> loaded_cksum;
                cksum_is.close();

            	// verify checksum
            	fs::checksum_t acc_cksum = fs::calc_crc32(0, memo_filename);
                acc_cksum = fs::calc_crc32(acc_cksum, todo_filename);
            	if(acc_cksum != loaded_cksum) return false;
            } else {
                // checksum file is possibly bad or not found
                // mark that here to create it on exit
                is_changed = true;
            }

            // load todos list
            std::ifstream todo_is(todo_filename.c_str(), std::ios::in);
            if(todo_is.bad()) return false;
            std::vector<todo_task> loaded_tasks;
            todo_task next_task("");
            while(true) {
            	if(!next_task.read(&todo_is)) {
            		break;
            	}
            	loaded_tasks.push_back(next_task);
            }
            tasks.swap(loaded_tasks);
            todo_is.close();

            // load memo, if the memo file is not found or failed
            // this is not an error, just ignore this issue
            std::ifstream memo_is(memo_filename.c_str(), std::ios::in);
            memo_string.clear();
            if(!memo_is.bad()) {
            	std::stringstream buf;
                while(true) {
                    char next_ch;
                    memo_is.get(next_ch);
                    if(!memo_is.good() || memo_is.eof()) {
                        break;
                    }
                    buf << next_ch;
                }
                memo_is.close();
                memo_string.assign(buf.str());
            }
            return true;
        }

        bool save() const {
            std::string basename = working_dir + monday.to_string();
            std::string memo_filename = basename + EXT_MEMO;
            std::string todo_filename = basename + EXT_TODOLIST;
            std::string cksum_filename = basename + EXT_CHECKSUM;

            // save memo
            if(memo_string.length() || fs::does_file_exist(memo_filename)) {
            	if(!memo_string.length()) {
                    fs::remove_file(memo_filename);
            	} else {
                    std::ofstream memo_os(memo_filename.c_str(), std::ios::trunc);
                    memo_os << memo_string;
                    memo_os.close();
            	}
            }

            // save todos list
            std::ofstream todo_os(todo_filename.c_str(), std::ios::trunc);
            for(std::vector<todo_task>::const_iterator it = tasks.begin();
                it != tasks.end();
                ++it) {
                it->write(&todo_os);
                todo_os << std::endl;
            }
            todo_os.close();

            // update checksum
            fs::checksum_t acc_cksum = fs::calc_crc32(0, memo_filename);
            acc_cksum = fs::calc_crc32(acc_cksum, todo_filename);

            // save checksum, if failed it's ok for this implementation
            std::ofstream cksum_os(cksum_filename.c_str(), std::ios::trunc);
            cksum_os << acc_cksum << std::endl;
            cksum_os.close();

            return true;
        }

        virtual void add_task(const std::string &descr) {
            tasks.push_back(todo_task(descr));
            is_changed = true;
        }

        virtual void set_memo(const std::string &memotext) {
            memo_string = memotext;
            is_changed = true;
        }

        virtual size_t size() const {
            return tasks.size();
        }

        virtual todo_task &operator[](size_t idx) {
            assert(idx < size());
            is_changed = true;
            return tasks.at(idx);
        }
    };

    weekly::weekly(const std::string &workdir)
        : p_data_impl(new weekly::impl(workdir)) {
        if(p_data_impl) {
            if(!p_data_impl->load()) {
            	delete p_data_impl;
            	p_data_impl = 0;
            }
        }
    }

    weekly::weekly(const std::string &workdir, const date& for_date)
        : p_data_impl(new weekly::impl(workdir, for_date)) {
        if(p_data_impl) {
            if(!p_data_impl->load()) {
                delete p_data_impl;
                p_data_impl = 0;
            }
        }
    }

    weekly::~weekly() {
        if(p_data_impl) {
            delete p_data_impl;
        }
    }

    bool
    weekly::is_valid() const {
        return p_data_impl != 0;
    }

    const date &
    weekly::starting_date() const {
        assert(p_data_impl);
        return p_data_impl->monday;
    }

    size_t
    weekly::size() const {
        assert(p_data_impl);
        return p_data_impl->tasks.size();
    }

    const todo_task &
    weekly::at(size_t idx) const {
        assert(p_data_impl);
        assert(idx < size());
        return p_data_impl->tasks[idx];
    }

    const std::string &
    weekly::memo() const {
        assert(p_data_impl);
        return p_data_impl->memo_string;
    }

    bool
    weekly::is_editable() const {
        if(is_valid()) {
            date now;
            now.shift_to(date::MONDAY);
            return now == p_data_impl->monday;
        }
        return false;
    }

    weekly::editor *
    weekly::get_editor() {
        assert(p_data_impl);
        if(is_editable()) {
            return p_data_impl;
        }
        return 0;
    }

}; // namespace wpr
