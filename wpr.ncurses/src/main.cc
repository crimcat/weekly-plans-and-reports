/* 
 * File:   main.cc
 * Author: crimcat
 *
 * Created on February 16, 2012, 2:33 AM
 */

#include <ncurses.h>
#include <locale.h>

/*
 * 
 */
int
main(int argc, char** argv) {
    
    initscr();
    
    int max_col = getmaxx(stdscr);
    int max_row = getmaxy(stdscr);

    attron(A_BOLD | A_UNDERLINE);
    printw("Hello, world! of (%d, %d)", max_col, max_row);
    attroff(A_BOLD | A_UNDERLINE);
    mvprintw(1, 1, "with or without the bug");
    refresh();
    getch();
    endwin();
    
    return 0;
}
