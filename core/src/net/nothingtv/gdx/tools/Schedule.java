package net.nothingtv.gdx.tools;

import com.badlogic.gdx.utils.Array;

public class Schedule {

    public interface ScheduleAction {
        void run();
    }

    public static class ScheduleActionEntry {
        ScheduleAction action;
        long interval;
        long nextRun;
        public ScheduleActionEntry(long interval, ScheduleAction action) {
            this.interval = interval;
            this.action = action;
            this.nextRun = System.currentTimeMillis() + interval;
        }
    }

    private final Array<ScheduleActionEntry> actions = new Array<>();

    public void everySeconds(long n, ScheduleAction action) {
        actions.add(new ScheduleActionEntry(n * 1000, action));
    }

    public void everyMilliSeconds(long n, ScheduleAction action) {
        actions.add(new ScheduleActionEntry(n, action));
    }

    public void update() {
        long now = System.currentTimeMillis();
        for (ScheduleActionEntry entry : actions)
            if (entry.nextRun <= now) {
                entry.action.run();
                entry.nextRun = now + entry.interval;
        }
    }
}
