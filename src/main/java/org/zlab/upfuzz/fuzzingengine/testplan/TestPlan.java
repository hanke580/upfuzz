package org.zlab.upfuzz.fuzzingengine.testplan;

import java.util.List;

import com.google.gson.JsonSerializer;
import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;

public class TestPlan {
    public List<Event> events;

    public TestPlan(List<Event> events) {
        this.events = events;
    }

    public List<Event> getEvents() {
        return events;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("test plan:\n");
        for (Event event : events) {
            sb.append(event + "\n");
        }
        sb.append("test plan end\n");
        return sb.toString();
    }

}
