/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.crimcat.util.wpr;

/**
 *
 * @author Stas
 */
public final class WordsDistanceCalculators {
    
    public interface ICalculator {
        /**
        * Find (calculate) distance between words
        * @param w1 word one
        * @param w2 word two
        * @return integer distance between two given words
        */
       int calcDistance(String w1, String w2);
   }

    /**
     * Get Hamming distance between words function
     */
    public static final ICalculator HAMMING = (String w1, String w2) -> {
        int length = Math.min(w1.length(), w2.length());
        int hammingDistance = Math.abs(w1.length() - w2.length());
        for(int i = 0; i < length; i++) {
            if(w1.charAt(i) != w2.charAt(i)) {
                ++hammingDistance;
            }
        }
        return hammingDistance;
    };
    
    /**
     * Get Levenstein distance between words function
     */
    public static final ICalculator LEVENSTEIN = (String w1, String w2) -> {
        int N = w1.length();
        int M = w2.length();
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
                char c1 = w1.charAt(i - 1);
                char c2 = w2.charAt(j - 1);
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
    };
}
