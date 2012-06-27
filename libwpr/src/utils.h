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

/* File:    util.h
 * Author:  Stas Torgashov aka Crimson Cat (mailto:crimcat@yandex.ru)
 * Created: 2011, January 28
 */

#ifndef LIBWPR_UTILS__H
#define	LIBWPR_UTILS__H

#include "libwpr.h"

#include <iostream>

/**
 * Weekly Plans and Reports.
 */
namespace wpr {

	/**
	 * Object parsers functionality here.
	 */
    namespace parser {

    	/**
    	 * Function type for parser: character selector. Checks if the given
    	 * character is good for the required criteria. If so, returns true.
    	 */
        typedef bool (*char_selector)(char);

        /**
         * Char selector: any character is ok, default action.
         * @return true
         */
        bool equality(char);

        /**
         * Char selector: is the character a digit?
         * @param c character symbol to test
         * @return true if the supplied character is a digit symbol
         */
        bool is_digit(char c);

        /**
         * Char selecter: is the character a dash symbol ('-')?
         * @param c character symbol to test
         * @return true if the supplied character is a dash '-'
         */
        bool is_dash(char c);

        /**
         * Char selecter: is the character a letter?
         * @param c character symbol to test
         * @return true if the supplied character is a letter
         */
        bool is_char(char c);

        /**
         * Char selecter: is the character a colon symbol (':')?
         * @param c character symbol to test
         * @return true if the supplied character is a colon ':'
         */
        bool is_colon(char c);

        /**
         * Parser function: expect the predefined number of characters from
         * the input stream which are selected by the char selector function.
         * The result is put into the result string (is the pointer is not NULL).
         * @param is input stream pointer
         * @param n number of expected characters
         * @param res pointer to the result string or NULL
         * @param f character selector function
         * @return true if the given number of characters are read
         */
        bool expect(std::istream *is, int n, std::string *res, char_selector f);

        /**
         * Read line (till '\n' end of line symbol) from the input stream. All
         * read characters are put into the result string if it's specified.
         * @param is input stream pointer
         * @param res pointer to the result string or NULL
         * @return true if read is successfull
         */
        bool readline(std::istream *is, std::string *res);
        
    }; // namespace parser

    /**
     * Weekly database filesystem operations
     */
    namespace fs {

    	/**
    	 * Checksum type.
    	 */
        typedef unsigned int checksum_t;

        /**
         * Calculate CRC32 checksum from the given file and the input value.
         * If the input value is 0 then the checksum is calculated as new one,
         * otherwise it's accumulated with the given one.
         * The implementation is borrowed from here:
         * http://ru.wikipedia.org/wiki/%D0%A6%D0%B8%D0%BA%D0%BB%D0%B8%D1%87%D0%B5%D1%81%D0%BA%D0%B8%D0%B9_%D0%B8%D0%B7%D0%B1%D1%8B%D1%82%D0%BE%D1%87%D0%BD%D1%8B%D0%B9_%D0%BA%D0%BE%D0%B4#CRC-32
         * @param ival initial checksum value or 0
         * @param filename path the file
         * @return checksum value
         */
        checksum_t calc_crc32(checksum_t ival, const std::string &filename);

        /**
         * Ajust and setup the working directory, normalizing the path.
         * @wd pointer to the path string to be normalized
         */
        void adjust_and_setup_working_dir(std::string *wd);

        /**
         * Check if the given file exists.
         * @param path path string to the file to be checked
         * @return true if the file exists
         */
        bool does_file_exist(const std::string &path);

        /**
         * Remove the file with the given path.
         * @param path path to the file to be removed
         * @return true if the operation has been successful
         */
        bool remove_file(const std::string &path);

    }; // namespace fs

}; // namespace wpr


#endif	/* LIBWPR_UTILS__H */

