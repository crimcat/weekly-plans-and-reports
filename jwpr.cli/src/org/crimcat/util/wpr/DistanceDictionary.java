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
     * Initialize distance dictionary from a plain array of words
     * @param words 
     */
    public DistanceDictionary(String[] words) {
        this.words = Arrays.asList(words);
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
            int nextDistance = calcLevensteinDistance(word, w);
            if(nextDistance < distance) {
                distance = nextDistance;
                nearest = w;
            }
        }
        return nearest;
    }
    
    /**
     * Find Hamming distance for two given words.
     * @param word1 the first word
     * @param word2 the second word
     * @return Hamming distance between words
     */
    private int calcHammingDistance(String word1, String word2) {
        int length = Math.min(word1.length(), word2.length());
        int hammingDistance = Math.abs(word1.length() - word2.length());
        for(int i = 0; i < length; i++) {
            if(word1.charAt(i) != word2.charAt(i)) {
                ++hammingDistance;
            }
        }
        return hammingDistance;
    }
    
    /**
     * Steps to transform word1 to word2
     * @param word1
     * @param word2
     * @return Levenstein distance
     */
    private int calcLevensteinDistance(String word1, String word2) {
        int N = word1.length();
        int M = word2.length();
        int[][] D = new int[N + 1][M + 1];
        
        D[0][0] = 0;
        for(int i = 1; i < M; i++) {
            D[0][i] = D[0][i - 1] + 1;
        }
        for(int i = 1; i < N; i++) {
            D[i][0] = D[i - 1][0] + 1;
        }
        for(int i = 1; i < N + 1; i++) {
            for(int j = 1; j < M + 1; j++) {
                char c1 = word1.charAt(i - 1);
                char c2 = word2.charAt(j - 1);
                int a11 = D[i - 1][j - 1];
                if(c1 == c2) {
                    D[i][j] = a11;
                } else {
                    int a01 = D[i][j - 1];
                    int a10 = D[i - 1][j];
                    D[i][j] = Math.min(a01, Math.min(a10, a11)) + 1;
                }
            }
        }
        return D[N][M];
    }
    
    private List<String> words;
}
