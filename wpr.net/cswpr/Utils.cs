using System;

namespace cswpr.utils {

    class DistanceDictionary {
        public DistanceDictionary(string[] dictionary, Func<string, string, int> calculator) {
            this.dictionary = dictionary;
            this.calculator = calculator;
        }

        public string findNearest(string word) {
            string nearest = null;
            int distance = 10000; // hoping that any word couldn't be that long
            foreach(string w in dictionary) {
                int nextDistance = calculator(w, word);
                if(nextDistance < distance) {
                    distance = nextDistance;
                    nearest = w;
                }
            }
            return nearest;
        }

        private string[] dictionary;
        private Func<string, string, int> calculator;
    }

    static class WordDistanceCalculators {

        public static Func<string, string, int> HAMMING = (string w1, string w2) => {
            int length = Math.Min(w1.Length, w2.Length);
                int hammingDistance = Math.Abs(w1.Length - w2.Length);
                for(int i = 0; i < length; i++) {
                    if(w1[i] != w2[i]) {
                        ++hammingDistance;
                    }
                }
                return hammingDistance;
        };

        public static Func<string, string, int> LEVENSTEIN = (string w1, string w2) => {
            int N = w1.Length;
            int M = w2.Length;
            int[,] D = new int[N + 1, M + 1];
        
            D[0, 0] = 0;
            for(int i = 1; i < M; i++) {
                D[0, i] = D[0, i - 1] + 1;
            }
            for(int i = 1; i < N; i++) {
                D[i, 0] = D[i - 1, 0] + 1;
            }
            for(int i = 1; i < N + 1; i++) {
                for(int j = 1; j < M + 1; j++) {
                    char c1 = w1[i - 1];
                    char c2 = w2[j - 1];
                    int a11 = D[i - 1, j - 1];
                    if(c1 == c2) {
                        D[i, j] = a11;
                    } else {
                        int a01 = D[i, j - 1];
                        int a10 = D[i - 1, j];
                        D[i, j] = Math.Min(a01, Math.Min(a10, a11)) + 1;
                    }
                }
            }
            return D[N, M];
        };
    }
}
