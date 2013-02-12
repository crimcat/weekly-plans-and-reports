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

/* File:    utils.cc
 * Author:  Stas Torgashov aka Crimson Cat (mailto:crimcat@yandex.ru)
 * Created: 2011, January 28
 */


#include "utils.h"

#include <ctype.h>
#include <sstream>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>
#include <fstream>

namespace wpr {

    namespace parser {

        bool
        equality(char) {
            return true;
        }

        bool
        is_digit(char c) {
            return (bool)isdigit((int)c);
        }

        bool
        is_dash(char c) {
            return '-' == c;
        }

        bool
        is_char(char c) {
            return (bool)isalpha((int)c);
        }

        bool
        is_colon(char c) {
            return ':' == c;
        }

        bool
        expect(std::istream *is, int n, std::string *res, char_selector f) {
            if(is && is->good() && !is->eof()) {
                std::stringstream buf;
                if(!f) f = &equality;
                int i;
                for(i = 0; i < n; i++) {
                    char c = is->get();
                    if(is->eof() || !f(c)) {
                        break;
                    }
                    buf << c;
                }
                if(n != i) return false;
                if(res) {
                    *res = buf.str();
                }
                return is->good() && !is->eof();
            }
            return false;
        }

        bool
        readline(std::istream *is, std::string *res) {
            if(is && is->good() && !is->eof()) {
                std::stringstream buf;
                char c;
                while(!is->eof() && ('\n' != (c = is->get()))) {
                    buf << c;
                }
                if(res) {
                    *res = buf.str();
                }
                return is->good() && !is->eof();
            }
            return false;
        }

    }; // namespace parser

    namespace fs {

    	/**
    	 * The implementation is borrowed from Wikipedia:
    	 * http://ru.wikipedia.org/wiki/%D0%A6%D0%B8%D0%BA%D0%BB%D0%B8%D1%87%D0%B5%D1%81%D0%BA%D0%B8%D0%B9_%D0%B8%D0%B7%D0%B1%D1%8B%D1%82%D0%BE%D1%87%D0%BD%D1%8B%D0%B9_%D0%BA%D0%BE%D0%B4#CRC-32
    	 */
        checksum_t
        calc_crc32(checksum_t ival, const std::string& filename) {
            static unsigned int crc_table[256] = { 0 };
            static bool is_initialized = false;
            static const unsigned int POLY = 0xEDB88320;

            if(!is_initialized) {
                for(unsigned int i = 0; i < 256; ++i) {
                    unsigned int crc = i;
                    for(unsigned j = 0; j < 8; ++j) {
                        crc = (crc & 1) ? ((crc >> 1) ^ POLY) : (crc >> 1);
                    }
                    crc_table[i] = crc;
                }
                is_initialized = true;
            }

            unsigned int crc = ival ? (ival ^ 0xFFFFFFFF) : 0xFFFFFFFF;

            // scan file and calculate the checksum
            std::ifstream fos(filename.c_str(), std::ios::in);
            if(fos.good()) {
                while(true) {
                    unsigned int c = fos.get();
                    if(!fos.eof()) {
                        crc = crc_table[(crc ^ c) & 0xFF] ^ (crc >> 8);
                    } else break;
                }
                fos.close();
            }
            return crc ^ 0xFFFFFFFF;
        }

        static const char SEPARATOR = '/';

        void
        adjust_and_setup_working_dir(std::string *wd) {
            if(wd) {
                if(wd->length()) {
                    if(SEPARATOR != wd->at(wd->length() - 1)) {
                        *wd += SEPARATOR;
                    }
                    mkdir(wd->c_str(), 0755);
                }
            }
        }

        bool
        does_file_exist(const std::string &path) {
            struct stat sbuf;
            return 0 == stat(path.c_str(), &sbuf);
        }

        bool
        remove_file(const std::string &path) {
            return 0 == unlink(path.c_str());
        }

    }; // namespace fs

}; // namespace wpr

