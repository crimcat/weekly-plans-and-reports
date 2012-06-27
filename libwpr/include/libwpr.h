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

/* File:    libwpr.h
 * Author:  Stas Torgashov aka Crimson Cat (mailto:crimcat@yandex.ru)
 * Created: 2011, January 26
 */

#ifndef LIBWPR__H
#define	LIBWPR__H 1

#ifndef __cplusplus
#error Code is designated for C++ only
#endif /* __cplusplus */

#include <time.h>
#include <iostream>
#include <string>
#include <vector>

/**
 * Weekly Plans and Reports.
 */
namespace wpr {

	/**
	 * Date class, to select task date regardless time, only date.
	 * Date format is YYYY-MM-DD, no locale dependencies.
	 */
    class date {
    private:
        time_t utc_time;
        bool f_locked;
    public:
        /**
         * Ctor: create date object, preset to the today date.
         */
        date();

        /**
         * Get year value.
         * @return year integer value
         */
        int year() const;

        /**
         * Get month value, range is from 1 (January) to 12 (December).
         * @return month integer value [1:12]
         */
        int month() const; // 1-12

        /**
         * Get day of month. Range is from 1 to 31 (max allowed).
         * @return day of month integer value [1:31]
         */
        int day() const;   // 1-31

        /**
         * Get week number in the year.
         * @return week number value
         */
        int week_number() const;

        /**
         * Week day names enumeration.
         */
        enum weekdays {
            MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
        };

        /**
         * Shift the date by the number of days forward if positive, and
         * backward if negative.
         * @param days days value to shift
         */
        void shift(int days);

        /**
         * Shift the date to the nearest weekday by its name within
         * the current week. See @wpr::date::weekdays
         * @param wday weekday symbolic enumeration value
         */
        void shift_to(weekdays wday);

        /**
         * Get weekday of the date, as enumerated value.
         * @see wpr::date::weekday
         * @return weekday enumerated value
         */
        weekdays week_day() const;

        /**
         * Calculate distance between this date and the supplied one, in days.
         * Negative result shows distance to the past.
         * @param d date to calculate distance to
         * @return number of days between dates
         */
        int days_to(const date &d) const;

        /**
         * Get date representation as sting, formatted as YYYY-MM-DD.
         * The format is fixed, it's not intended to be changed.
         * @return date as string
         */
        std::string to_string() const;

        /**
         * Serialize the date in its format (YYYY-MM-DD) to the output stream.
         * @param os pointer to the output stream
         * @return success indicator (true if success, otherwise - false)
         */
        bool write(std::ostream *os) const;

        /**
         * Deserialize date object from the input stream. The method expects
         * date in the following format: YYYY-MM-DD. If operation fails,
         * the method does not change the object.
         * @param is input stream pointer
         * @return true on success
         */
        bool read(std::istream *is);

        /**
         * Comparator: check if this date is less than supplied.
         * @param d date to compare with
         * @return true if less, otherwise false
         */
        bool operator<(const date &d) const;

        /**
         * Comparator: check if this date is less or equal than supplied one.
         * @param d date to compare with
         * @return true if less of equla, otherwise false
         */
        bool operator<=(const date &d) const;

        /**
         * Comparator: check if this date is equal to the supplied one
         * @param d date to compare witj
         * @return true if it's equal, otherwise returns false
         */
        bool operator==(const date &d) const;
        
        /**
         * Lock this instance of date from any changes. Now it will look
         * like a constant, and any change operation will do nothing.
         */
        void lock();
        
        /**
         * Check if this date instance has been already locked.
         * @return true if the date is locked
         */
        bool is_locked() const;
    };

    /**
     * Represents task record to be scheduled and performed. The task is
     * created only on the today date, no information can be changed, except
     * the completion flag.
     */
    class todo_task {
    private:
        date date_originated;
        bool completed;
        std::string descr;
    public:
        /**
         * Ctor: create task with the given description. The task date is
         * taked as today by default. This cannot be changed.
         */
        explicit todo_task(const std::string &task_description);

        /**
         * Check if this task is already completed.
         * @return true if the task is finished
         */
        bool is_completed() const;

        /**
         * Get task creation date.
         * @return the date object when the task was created on
         */
        const date &originated_on() const;

        /**
         * Get task description.
         * @return description string
         */
        const std::string &description() const;

        /**
         * Mark the task as completed. No verification is done.
         * The operation is not reversible.
         */
        void complete_it();

        /**
         * Serialize the task object.
         * @param os pointer to the output stream
         * @return true if operation has been successful
         */
        bool write(std::ostream *os) const;

        /**
         * Deserialize the task object from the input stream. If operation
         * fails, the current object contents is not changed.
         * @param is pointer to the input stream
         * @return true if operations is successful
         */
        bool read(std::istream *is);
    };

    /**
     * Weekly report representation. Basically, it is an array of tasks
     * for the given week. The week is selected as current (by default) or
     * with any date which matches a week in the past. Future dates are
     * treated as errors and are not processed. Any modification can be done
     * only to the current week.
     */
    class weekly {
    public:
    	/**
    	 * Week modifier interface. Used to make changes to the current week or
    	 * to the tasks which belong to the current week.
    	 */
        class editor {
        public:
            virtual ~editor() { }

            /**
             * Add new task.
             * @param descr task description (or title)
             */
            virtual void add_task(const std::string &descr) = 0;

            /**
             * Set memo text for the current week.
             * @param memotext string with memo text
             */
            virtual void set_memo(const std::string &memotext) = 0;

            /**
             * Get number of tasks for this week.
             * @return number of registered tasks
             */
            virtual size_t size() const = 0;

            /**
             * Get task by its index (or ID).
             * @param idx task index value, must be 0 or more,and less than idx value
             * @return reference to the task object
             */
            virtual todo_task &operator[](size_t idx) = 0;

            /**
             * Synonim to the operator[]. Syntax sugar if needed.
             * @param idx task index value, must be 0 or more,and less than idx value
             * @return reference to the task object
             */
            todo_task &at(size_t idx) {
            	return operator [](idx);
            }
        };
    private:
        struct impl;
        impl *p_data_impl;

        // copying is not allowed
        weekly(const weekly &);
        const weekly &operator=(const weekly &);
    public:
        /**
         * Ctor: create and load weekly database for the current week,
         * database directory is specified by its path.
         * @param workdir weekly database directory path
         */
        explicit weekly(const std::string &workdir);

        /**
         * Ctor: create and load weekly database for specified date
         * (week is matched by this date).
         * @param workdir weekly database directory path
         * @param for_date date to load weekly for
         */
        weekly(const std::string &workdir, const date &for_date);
        virtual ~weekly();

        /**
         * Is this weelky object valid and loaded properly?
         * @return true is this weekly object is loaded and can be used
         */
        bool is_valid() const;

        /**
         * Get weekly starting date. Returns week monday date object.
         * @return date of week monday
         */
        const date &starting_date() const;

        /**
         * Get number of tasks registered for the given week.
         * @return number of all tasks
         */
        size_t size() const;

        /**
         * Get task by its index (ID value). Does not allow any editing
         * operations.
         * @param idx task index, should be 0 or greater and less than size()
         * @return task object constant reference
         */
        const todo_task &at(size_t idx) const;

        /**
         * Synonim for at() operation.
         * @param idx task index, should be 0 or greater and less than size()
         * @return task object constant reference
         */
        const todo_task &operator[](size_t idx) const {
        	return at(idx);
        }

        /**
         * Get memo text string for this week.
         * @return memo string
         */
        const std::string &memo() const;

        /**
         * Check if this object is editable (i.e. does this object reference
         * the current week).
         * @return true if one can edit this object
         */
        bool is_editable() const;

        /**
         * Get editor interface pointer if available. If this object cannot be
         * edited the method returns NULL.
         * @return pointer to the editor interface
         */
        weekly::editor *get_editor();
    };

}; // namespace wpr

#endif	/* LIBWPR__H */

