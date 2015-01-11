using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.IO;

namespace wprlib.AppDatabase {

    public interface IAppConfig {
        bool doCopyFromThePastOnMondays { get; }
        bool doVerboseOutput { get; }
    }

    public interface IFilesBundle {
        string databaseDirPath { get; }
        string todoFilePath { get; }
        string memoFilePath { get; }
        bool checkConsistency();
        void updateOnChanges();
    }

    public static class Gate {

        public static IAppConfig getAppConfig() {
            return new AppConfigImpl();
        }

        public static IFilesBundle getFilesBundle(TaskDate td) {
            return new FilesBundleImpl(td);
        }

        public static IFilesBundle getFilesBundle(TaskDate td, string groupName) {
            return new FilesBundleImpl(td, groupName);
        }

        public static bool setDefaultDatabasePath(string dbPath) {
            if(Directory.Exists(dbPath)) {
                defaultDatabasePath = dbPath;
                return true;
            }
            return false;
        }

        public static string getDefaultDatabasePath() {
            if(null == defaultDatabasePath) {
                string path = System.Environment.GetFolderPath(System.Environment.SpecialFolder.UserProfile) +
                    System.IO.Path.DirectorySeparatorChar +
                    ".wpr";
                if(!Directory.Exists(path)) {
                    Directory.CreateDirectory(path);
                }
                defaultDatabasePath = path;
            }
            return defaultDatabasePath;
        }

        private static string defaultDatabasePath = null;
    }
}
