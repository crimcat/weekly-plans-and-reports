using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.IO;

namespace wprlib.AppDatabase {

    internal class AppConfigImpl: IAppConfig {
        private const string APP_CONFIG_FILE_NAME = ".global_config";
        private const string CFG_OPTION_AUTO_COPY_FROM_THE_PAST = "auto-copy-from-the-past";
        private const string CFG_OPTION_VERBOSE_OUTPUT = "verbose-output";

        private bool f_DoCopyFromThePastOnMondays = false;
        private bool f_DoVerboseOutput = false;

        public AppConfigImpl() {
            string settingsFIle = AppDatabase.Gate.getDefaultDatabasePath() +
                System.IO.Path.DirectorySeparatorChar +
                APP_CONFIG_FILE_NAME;

            if(!File.Exists(settingsFIle)) {
                StreamWriter sw = new StreamWriter(settingsFIle);
                sw.WriteLine("{0}=false", CFG_OPTION_AUTO_COPY_FROM_THE_PAST);
                sw.WriteLine("{0}=false", CFG_OPTION_VERBOSE_OUTPUT);
                sw.Flush();
                sw.Close();
            } else {
                StreamReader sr = new StreamReader(settingsFIle);
                while(-1 != sr.Peek()) {
                    string line = sr.ReadLine();
                    line.Trim();
                    if(('#' == line[0]) || (0 == line.Length))
                        continue;

                    string[] parts = line.Split('=');
                    if(2 != parts.Length) {
                        throw new Exception("Bad config file format");
                    }
                    switch(parts[0]) {
                    case CFG_OPTION_AUTO_COPY_FROM_THE_PAST:
                        f_DoCopyFromThePastOnMondays = parts[1].Equals("true");
                        break;
                    case CFG_OPTION_VERBOSE_OUTPUT:
                        f_DoVerboseOutput = parts[1].Equals("true");
                        break;
                    }
                }
                sr.Close();
            }
        }

        public bool doCopyFromThePastOnMondays {
            get { return f_DoCopyFromThePastOnMondays; }
        }
        public bool doVerboseOutput {
            get { return f_DoVerboseOutput; }
        }
    }

    internal class FilesBundleImpl: IFilesBundle {
        private const string EXT_TODOLIST = ".todolist";
        private const string EXT_MEMOFILE = ".memo";
        private const string EXT_CHECKSUM = ".checksum";

        private string basePath;
        private string groupName = null;

        public FilesBundleImpl(TaskDate td) {
            basePath = AppDatabase.Gate.getDefaultDatabasePath()
                + System.IO.Path.DirectorySeparatorChar
                + td.shiftToNearestWeekDay(DayOfWeek.Monday).ToString();
        }

        public FilesBundleImpl(TaskDate td, string groupName) {
            this.groupName = groupName;
            basePath = AppDatabase.Gate.getDefaultDatabasePath()
                + System.IO.Path.DirectorySeparatorChar
                + groupName
                + System.IO.Path.DirectorySeparatorChar
                + td.shiftToNearestWeekDay(DayOfWeek.Monday).ToString();
        }

        public string databaseDirPath {
            get {
                return AppDatabase.Gate.getDefaultDatabasePath()
                    + System.IO.Path.DirectorySeparatorChar
                    + groupName;
            }
        }

        public string todoFilePath {
            get {
                return basePath + EXT_TODOLIST;
            }
        }

        public string memoFilePath {
            get {
                return basePath + EXT_MEMOFILE;
            }
        }

        internal string checkSumPath {
            get {
                return basePath + EXT_CHECKSUM;
            }
        }

        public bool checkConsistency() {
            if(File.Exists(todoFilePath)) {
                uint crc = utilities.CRC32.calcFileCRC32(0, memoFilePath);
                crc = utilities.CRC32.calcFileCRC32(crc, todoFilePath);
                StreamReader sr = new StreamReader(checkSumPath);
                string chksumStr = sr.ReadLine();
                sr.Close();
                return UInt32.Parse(chksumStr) == crc;
            }
            return true;
        }

        public void updateOnChanges() {
            if(File.Exists(todoFilePath)) {
                uint crc = utilities.CRC32.calcFileCRC32(0, memoFilePath);
                crc = utilities.CRC32.calcFileCRC32(crc, todoFilePath);
                StreamWriter sw = new StreamWriter(checkSumPath);
                sw.Write(crc);
                sw.Flush();
                sw.Close();
            }
        }
    }
}
