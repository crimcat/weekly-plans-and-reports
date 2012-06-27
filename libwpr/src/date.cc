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

/* File:    date.cc
 * Author:  Stas Torgashov aka Crimson Cat (mailto:crimcat@yandex.ru)
 * Created: 2011, January 28
 */

#include "libwpr.h"

#include "utils.h"

#include <sstream>
#include <iomanip>
#include <stdlib.h>

namespace wpr {

    date::date() : utc_time(time(0)), f_locked(false) {
        struct tm timedata;
        localtime_r(&utc_time, &timedata);
        timedata.tm_hour = 0;
        timedata.tm_min = 0;
        timedata.tm_sec = 1;
        timedata.tm_isdst = 0;
        utc_time = mktime(&timedata);
    }

    int
    date::year() const {
        struct tm timedata;
        localtime_r(&utc_time, &timedata);
        return timedata.tm_year + 1900;
    }
    int
    date::month() const {
        struct tm timedata;
        localtime_r(&utc_time, &timedata);
        return timedata.tm_mon + 1;
    }
    int
    date::day() const {
        struct tm timedata;
        localtime_r(&utc_time, &timedata);
        return timedata.tm_mday;
    }

    void
    date::shift(int days) {
        if(!is_locked() && days) {
            utc_time += days * 24 * 60 * 60;
        }
    }

    void
    date::shift_to(weekdays wday) {
        shift((int)wday - (int)week_day());
    }

    int
    date::week_number() const {
        struct tm jan01;
        localtime_r(&utc_time, &jan01);
        jan01.tm_mon = 0;
        jan01.tm_mday = 1;

        date jan01_date;
        jan01_date.utc_time = mktime(&jan01);

        if(MONDAY != jan01_date.week_day()) {
            jan01_date.shift_to(SUNDAY);
            jan01_date.shift(1);
        }
        if(*this < jan01_date) return 1;

        date now(*this);
        now.shift_to(MONDAY);

        return 2 + jan01_date.days_to(now) / 7;
    }

    date::weekdays
    date::week_day() const {
        struct tm timedata;
        localtime_r(&utc_time, &timedata);
        return (weekdays)(timedata.tm_wday ? timedata.tm_wday - 1 : 6);
    }

    int
    date::days_to(const date &d) const {
        return (int)(d.utc_time - utc_time) / 60 / 60 / 24;
    }

    std::string
    date::to_string() const {
        std::stringstream buf;
        buf << std::setfill('0') << std::setw(4) << year() << '-'
            << std::setw(2) << month() << '-'
            << std::setw(2) << day();
        return std::string(buf.str());
    }

    bool
    date::write(std::ostream *os) const {
        if(os && os->good()) {
            (*os) << to_string();
            return os->good();
        }
        return false;
    }
    bool
    date::read(std::istream *is) {
        if(is_locked()) return false;

        std::string yyyy, mm, dd;
        if(parser::expect(is, 4, &yyyy, &parser::is_digit) &&
           parser::expect(is, 1, 0, &parser::is_dash) &&
           parser::expect(is, 2, &mm, &parser::is_digit) &&
           parser::expect(is, 1, 0, &parser::is_dash) &&
           parser::expect(is, 2, &dd, &parser::is_digit)) {
               struct tm timedata;
               int y = timedata.tm_year = strtol(yyyy.c_str(), 0, 10) - 1900;
               int m = timedata.tm_mon = strtol(mm.c_str(), 0, 10) - 1;
               int d = timedata.tm_mday = strtol(dd.c_str(), 0, 10);
               timedata.tm_hour = 0;
               timedata.tm_min = 0;
               timedata.tm_sec = 1;
               timedata.tm_isdst = 0;
               time_t tp = mktime(&timedata);
               if((time_t)-1 != tp) {
                   localtime_r(&tp, &timedata);
                   if((y == timedata.tm_year) &&
                      (m == timedata.tm_mon) &&
                      (d == timedata.tm_mday)) {
                          utc_time = tp;
                          return true;
                   }
               }
        }
        return false;
    }

    bool
    date::operator<(const date &d) const {
        return utc_time < d.utc_time;
    }

    bool
    date::operator<=(const date &d) const {
        return utc_time <= d.utc_time;
    }

    bool
    date::operator==(const date &d) const {
        return utc_time == d.utc_time;
    }

    void
    date::lock() {
        f_locked = true;
    }

    bool
    date::is_locked() const {
        return f_locked;
    }

}; // namespace wpr
