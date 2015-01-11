using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace wprlib {
    
    public class TaskDate {

        public TaskDate() {
            now = DateTime.Now;
        }

        public TaskDate(string str) {
            string[] parts = str.Split('-');
            if(parts.Length == 3) {
                now = new DateTime(Convert.ToInt32(parts[0]), Convert.ToInt32(parts[1]), Convert.ToInt32(parts[2]));
            } else {
                throw new ArgumentException();
            }
        }

        public int year {
            get { return now.Year; }
        }

        public int month {
            get { return now.Month; }
        }

        public int dayOfMonth {
            get { return now.Day; }
        }

        public DayOfWeek weekDay {
            get { return now.DayOfWeek; }
        }

        public int weekNumber {
            get {
                return (now.DayOfYear / 7) + 1;
            }
        }

        public TaskDate shift(int days) {
            return new TaskDate(now.AddDays((double)days));
        }

        public TaskDate shiftToNearestWeekDay(DayOfWeek day) {
            int delta = weekDayToInt(day) - weekDayToInt(weekDay);
            return shift(delta);
        }

        public override string ToString() {
            StringBuilder sb = new StringBuilder();
            sb.Append(year);
            sb.Append('-');
            if(month < 10)
                sb.Append('0');
            sb.Append(month);
            sb.Append('-');
            if(dayOfMonth < 10)
                sb.Append('0');
            sb.Append(dayOfMonth);
            return sb.ToString();
        }

        public override bool Equals(object obj) {
            TaskDate td = obj as TaskDate;
            if(null != td) {
                return (year == td.year) && (month == td.month) && (dayOfMonth == td.dayOfMonth);
            }
            return false;
        }

        public int CompareTo(TaskDate td) {
            return now.CompareTo(td.now);
        }

        public override int GetHashCode() {
            return ToString().GetHashCode();
        }

        private TaskDate(DateTime theDate) {
            now = theDate;
        }

        private static int weekDayToInt(DayOfWeek day) {
            int dayNo = (int)day;
            return (0 == dayNo) ? 6 : (dayNo - 1);
        }

        private DateTime now;
    }

    public class TodoTask {

        internal TodoTask(string description, Weekly w) {
            taskDescr = description;
            weekly = w;
        }

        public string title {
            get { return taskDescr; }
        }

        public TaskDate originatedOn {
            get { return originated; }
        }

        public bool isCompleted {
            get { return fCompleted; }
        }

        public void markCompleted() {
            weekly.invalidate();
            fCompleted = true;
        }

        public override String ToString() {
            return originatedOn.ToString() + ":" +
                (fCompleted ? "C" : "A") + ":" +
                title;
        }

        internal static TodoTask fromString(string str, Weekly w) {
            string[] parts = str.Split(':');
            if(parts.Length >= 3) {
                TaskDate tdate = new TaskDate(parts[0]);
                if(TASK_ACTIVE.Equals(parts[1]) || TASK_COMPLETED.Equals(parts[1])) {
                    string descr = parts[2];
                    for(int i = 3; i < parts.Length; ++i) {
                        descr += parts[i];
                    }
                    TodoTask ttask = new TodoTask(descr, w);
                    ttask.originated = tdate;
                    ttask.fCompleted = TASK_COMPLETED.Equals(parts[1]);
                    return ttask;
                }
            }
            throw new ArgumentException();
        }

        private const string TASK_ACTIVE = "A";
        private const string TASK_COMPLETED = "C";

        private string taskDescr = null;
        private Weekly weekly = null;
        private TaskDate originated = new TaskDate();
        private bool fCompleted = false;
    }
}
