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

/* File:    TaskDate.java
 * Author:  Stas Torgashov aka Crimson Cat (crimcat@yandex.ru)
 * Created: 2011, March 25
 */

package org.crimcat.lib.wpr;

import java.util.Calendar;

/**
 * Date representation for the tasks. It's a simple representation,
 * providing only year, month and day of month information; time is preset to
 * 0 hours 0 minutes 1 second of the given date.
 */
public class TaskDate implements Cloneable {

    /**
     * Weekdays enumeration. Used to distinguish the same from @see Calendar
     * object.
     */
    public enum WeekDay {
        MONDAY,
        TUESDAY,
        WEDNESDAY,
        THURSDAY,
        FRIDAY,
        SATURDAY,
        SUNDAY,
    };

    /**
     * Create new date as today.
     */
    public TaskDate() {
        calTime = Calendar.getInstance();
        calTime.set(Calendar.HOUR, 0);
        calTime.set(Calendar.MINUTE, 0);
        calTime.set(Calendar.SECOND, 1);
        calTime.set(Calendar.DST_OFFSET, 0);
    }

    /**
     * Clone the object,
     * @return clone object reference
     */
    @Override
    public Object clone() {
        TaskDate td = new TaskDate();
        td.calTime = (Calendar)calTime.clone();
        return td;
    }

    /**
     * Get year number.
     * @return year
     */
    public int year() {
        return calTime.get(Calendar.YEAR);
    }

    /**
     * Get month number.
     * @return month as 1..12
     */
    public int month() {
        return calTime.get(Calendar.MONTH) + 1;
    }

    /**
     * Get day of month number.
     * @return day of month as 1..31
     */
    public int dayOfMonth() {
        return calTime.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Get week day, @see WeekDay
     * @return week day enumeration element
     */
    public TaskDate.WeekDay weekDay() {
        return fromCalendarDayToWeekDay(calTime.get(Calendar.DAY_OF_WEEK));
    }

    /**
     * Get week number (number of the week in the year)
     * @return week number 1..~52
     */
    public int weekNumber() {
        return calTime.get(Calendar.WEEK_OF_YEAR);
    }

    /**
     * Create new date object from this one shifted by the give number of days.
     * Negative shift value indicates shifting in the past.
     * @param days number of days to shift
     * @return new date object
     */
    public TaskDate shift(int days) {
        TaskDate td = (TaskDate)this.clone();
        long currentTime = td.calTime.getTimeInMillis();
        currentTime += (long)days * 24l * 60l * 60l * 1000l;
        td.calTime.setTimeInMillis(currentTime);
        return td;
    }

    /**
     * Create new date object from this one shifted to the nearest week day.
     * @param wd week day enumeration element
     * @return new date object
     */
    public TaskDate shiftToWeekDay(TaskDate.WeekDay wd) {
        int diff = normalizeWeekDay(fromWeekDayToCalendarDay(wd)) -
            normalizeWeekDay(fromWeekDayToCalendarDay(weekDay()));
        return shift(diff);
    }

    /**
     * Check if two dates are equal.
     * @param td another date object to compare with
     * @return true if the dates are for the same day
     */
    public boolean equals(TaskDate td) {
        return (year() == td.year()) && (month() == td.month()) && (dayOfMonth() == td.dayOfMonth());
    }

    /**
     * Compare two dates. Return negative value if this one is less (in the past)
     * if compared with the given one, returns positive value if this one is
     * greater and return 0 if the dates are equal.
     * @param td another date to compare with
     * @return &lt;0 - less, ==0 - equal, &gt;0 - greater
     */
    public int compare(TaskDate td) {
        final long dateInSeconds = calTime.getTimeInMillis() / 1000L / 60L;
        final long tdDateInSeconds = td.calTime.getTimeInMillis() / 1000L / 60L;
        return (int)(dateInSeconds - tdDateInSeconds);
    }

    /**
     * Convert the date to the string representation: YYYY-MM-DD.
     * @return as string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(year());
        sb.append('-');
        if(month() < 10) sb.append('0');
        sb.append(month());
        sb.append('-');
        if(dayOfMonth() < 10) sb.append('0');
        sb.append(dayOfMonth());
        return sb.toString();
    }

    /**
     * Read date to this object from the given string. It expects the
     * following date format: YYYY-MM-DD. If error happens (it cannot parse
     * the input string) this object does not change its state.
     * @param str string to read the date from
     * @return true if successful (date string recognized)
     */
    public boolean fromString(String str) {
        String[] parts = str.split("-");
        if(parts.length == 3) {
            try {
                int _year = Integer.parseInt(parts[0]);
                int _month = Integer.parseInt(parts[1]) - 1;
                int _day = Integer.parseInt(parts[2]);
                Calendar tmpCal = Calendar.getInstance();
                tmpCal.clear();
                tmpCal.set(_year, _month, _day, 0, 0, 1);
                if((tmpCal.get(Calendar.YEAR) == _year) &&
                   (tmpCal.get(Calendar.MONTH) == _month) &&
                   (tmpCal.get(Calendar.DAY_OF_MONTH) == _day)) {
                    calTime = tmpCal;
                    return true;
                }
            } catch(NumberFormatException nfe) { }
        }
        return false;
    }

    /**
     * Normalize week days. @see Calendar. The week is started on Monday
     * which gets number of 1, while Sunday is the end of the week and has
     * number of 7.
     * @param wd week day number
     * @return normalized week day number
     */
    private static int normalizeWeekDay(int wd) {
        if(Calendar.SUNDAY == wd) {
            wd = 7;
        } else {
            wd -= 1;
        }
        return wd;
    }

    /**
     * Convert from integer week day number for week day enumerated value.
     * @see Calendar, @see WeekDay
     * @param calDayNo week day number
     * @return week day enumeration element
     */
    private static WeekDay fromCalendarDayToWeekDay(int calDayNo) {
        switch(calDayNo) {
        case Calendar.SUNDAY: return WeekDay.SUNDAY;
        case Calendar.MONDAY: return WeekDay.MONDAY;
        case Calendar.TUESDAY: return WeekDay.TUESDAY;
        case Calendar.WEDNESDAY: return WeekDay.WEDNESDAY;
        case Calendar.THURSDAY: return WeekDay.THURSDAY;
        case Calendar.FRIDAY: return WeekDay.FRIDAY;
        case Calendar.SATURDAY: return WeekDay.SATURDAY;
        }
        throw new RuntimeException();
    }

    /**
     * Convert from week day enumeration to integer week day number.
     * @see Calendar, @see WeekDay
     * @param day week day enumeration element
     * @return week day number
     */
    private static int fromWeekDayToCalendarDay(WeekDay day) {
        switch(day) {
        case SUNDAY: return Calendar.SUNDAY;
        case MONDAY: return Calendar.MONDAY;
        case TUESDAY: return Calendar.TUESDAY;
        case WEDNESDAY: return Calendar.WEDNESDAY;
        case THURSDAY: return Calendar.THURSDAY;
        case FRIDAY: return Calendar.FRIDAY;
        case SATURDAY: return Calendar.SATURDAY;
        }
        throw new RuntimeException();
    }

    /**
     * Calendar object instance.
     */
    private Calendar calTime = null;
}
