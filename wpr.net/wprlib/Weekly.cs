using System;
using System.Collections.Generic;
using System.Collections;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.IO;

namespace wprlib {

    public class Weekly {

        public Weekly() {
            monday = new TaskDate().shiftToNearestWeekDay(DayOfWeek.Monday);
            filesBundle = AppDatabase.Gate.getFilesBundle(monday);
            load();
        }

        public Weekly(string groupName) {
            monday = new TaskDate().shiftToNearestWeekDay(DayOfWeek.Monday);
            filesBundle = AppDatabase.Gate.getFilesBundle(monday, groupName);
            load();
        }

        public Weekly(TaskDate td) {
            monday = td.shiftToNearestWeekDay(DayOfWeek.Monday);
            filesBundle = AppDatabase.Gate.getFilesBundle(monday);
            load();
        }

        public Weekly(TaskDate td, string groupName) {
            monday = td.shiftToNearestWeekDay(DayOfWeek.Monday);
            filesBundle = AppDatabase.Gate.getFilesBundle(monday, groupName);
            load();
        }

        public int numberOfTasks {
            get { return tasks.Count; }
        }

        public TaskDate startedOn {
            get { return monday; }
        }

        public bool isEditable() {
            return monday.Equals(new TaskDate().shiftToNearestWeekDay(DayOfWeek.Monday));
        }

        public TodoTask this[int idx] {
            get {
                if(idx < numberOfTasks) {
                    return tasks[idx];
                }
                throw new ArgumentOutOfRangeException();
            }
        }

        public bool addTask(string description) {
            if(isEditable()) {
                tasks.Add(new TodoTask(description, this));
                wasChanged = true;
            }
            return wasChanged;
        }

        public string memo {
            get { return memoText; }
            set {
                if(isEditable()) {
                    memoText = value;
                    wasChanged = true;
                }
            }
        }

        public void sync() {
            if(wasChanged) {
                save();
                filesBundle.updateOnChanges();
                wasChanged = false;
            } else {
                load();
            }
        }

        internal void invalidate() {
            wasChanged = true;
        }

        private void load() {
            if(!filesBundle.checkConsistency()) {
                throw new Exception("Checksum exception");
            }
            
            // load memo file if exists
            if(File.Exists(filesBundle.memoFilePath)) {
                StreamReader sr = new StreamReader(filesBundle.memoFilePath);
                StringBuilder sb = new StringBuilder();
                string line;
                while((line = sr.ReadLine()) != null) {
                    sb.AppendLine(line);
                }
                sr.Close();
                memoText = sb.ToString();
            }

            // load todo list if exists
            if(File.Exists(filesBundle.todoFilePath)) {
                StreamReader sr = new StreamReader(filesBundle.todoFilePath);
                string line;
                while((line = sr.ReadLine()) != null) {
                    TodoTask td = TodoTask.fromString(line, this);
                    tasks.Add(td);
                }
                sr.Close();
            }
        }

        private void save() {
            // save memo
            if(memo.Length == 0) {
                if(File.Exists(filesBundle.memoFilePath)) {
                    File.Delete(filesBundle.memoFilePath);
                }
            } else {
                StreamWriter sw = new StreamWriter(filesBundle.memoFilePath);
                sw.Write(memo);
                sw.Flush();
                sw.Close();
            }

            // save tasks
            if(numberOfTasks > 0) {
                StreamWriter sw = new StreamWriter(filesBundle.todoFilePath);
                foreach(TodoTask cur in tasks) {
                    sw.WriteLine(cur.ToString());
                }
                sw.Flush();
                sw.Close();
            }
        }

        private bool wasChanged = false;
        private string memoText = "";
        private List<TodoTask> tasks = new List<TodoTask>();
        private TaskDate monday = null;
        private AppDatabase.IFilesBundle filesBundle = null;
    }

    public class ChecksumException: Exception {
        public ChecksumException(string msg): base(msg) { }
    }
}
