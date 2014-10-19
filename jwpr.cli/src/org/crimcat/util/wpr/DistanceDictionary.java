/*
    (Java) Weekly Plans and Reports - simple and handy todo planning tool.

    Copyright (C) 2014  Stas Torgashov

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

/* File:    DistanceDictionary.java
 * Author:  Stas Torgashov aka Crimson Cat (crimcat@yandex.ru)
 * Created: 2014, March 28
 */
package org.crimcat.util.wpr;

import java.util.Arrays;
import java.util.List;

/**
 * DistanceDictionary.
 * It holds a list of words and can find a nearest word for the given one.
 * Distance between words is calculated by Hamming metrics.
 * @author Stas Torgashov
 */
public class DistanceDictionary {
    /**
     * Initialize distance dictionary from a plain array of words and words
     * distance calculator function
     * @param words array of words to create the dictionary
     * @param distCalc distance calculator to use
     */
    public DistanceDictionary(String[] words, WordsDistanceCalculators.ICalculator distCalc) {
        this.words = Arrays.asList(words);
        this.distCalc = distCalc;
    }
    
    /**
     * Find nearest word in the dictionary for the given one.
     * @param word to match
     * @return nearest word in the dictionary or null if nothing's found
     */
    public String findNearest(String word) {
        String nearest = null;
        int distance = 100;
        for(String w : words) {
            int nextDistance = distCalc.calcDistance(word, w);
            if(nextDistance < distance) {
                distance = nextDistance;
                nearest = w;
            }
        }
        return nearest;
    }
    
    private final List<String> words;
    private final WordsDistanceCalculators.ICalculator distCalc;
}
