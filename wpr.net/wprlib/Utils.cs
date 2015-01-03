using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.IO;

namespace wprlib.utilities {

    public static class CRC32 {
        private static uint[] crcTable = new uint[256];
        private static bool isCrcTableInitialized = false;
        private const uint POLY = 0xEDB88320;

        public static uint calcFileCRC32(uint ival, string filename) {
            uint crc = 0;
            if(!isCrcTableInitialized) {
                for(uint i = 0; i < 256; ++i) {
                    crc = i;
                    for(uint j = 0; j < 8; ++j) {
                        crc = (0 != (crc & 0x01)) ? ((crc >> 1) ^ POLY) : (crc >> 1);
                    }
                    crcTable[i] = crc;
                }
                isCrcTableInitialized = true;
            }

            crc = (0 != ival) ? (ival ^ 0xFFFFFFFF) : 0xFFFFFFFF;

            if(File.Exists(filename)) {
                FileStream fs = new FileStream(filename, FileMode.Open);
                if(fs.CanRead) {
                    while(true) {
                        int nextChar = fs.ReadByte();
                        if(-1 != nextChar) {
                            crc = crcTable[(crc ^ nextChar) & 0x0ff] ^ (crc >> 8);
                        } else
                            break;
                    }
                    fs.Close();
                }
            }
            return crc ^ 0xFFFFFFFF;
        }
    }
}
