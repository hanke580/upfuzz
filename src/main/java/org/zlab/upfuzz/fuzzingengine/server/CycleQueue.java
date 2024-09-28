package org.zlab.upfuzz.fuzzingengine.server;

import java.util.LinkedList;

public class CycleQueue<T> {
    private final LinkedList<T> queue;
    private int currentIndex;

    public CycleQueue() {
        this.queue = new LinkedList<>();
        this.currentIndex = 0;
    }

    // Adds a seed to the queue
    public void addSeed(T seed) {
        // avoid add duplicate seed
        if (queue.contains(seed)) {
            return;
        }
        queue.add(seed);
    }

    // Retrieves the current seed and moves the pointer to the next
    public T getNextSeed() {
        if (queue.isEmpty()) {
            return null;
        }

        T currentSeed = queue.get(currentIndex);
        currentIndex = (currentIndex + 1) % queue.size(); // Move to next or
                                                          // cycle to the start
        return currentSeed;
    }

    // Peeks at the current seed without moving the pointer
    public T peekSeed() {
        if (queue.isEmpty()) {
            return null; // or throw an exception based on your use case
        }
        return queue.get(currentIndex);
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

}
