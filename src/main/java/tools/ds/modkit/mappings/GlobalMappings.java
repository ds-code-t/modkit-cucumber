package tools.ds.modkit.mappings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * GlobalMappings
 *
 * Identical to NodeMap, but the public getters/setters are made thread-safe:
 * - put(...): write-locked
 * - get(...), getPojo(...): read-locked
 *
 * All other behavior (wildcards, direct path auto-creation, POJO sidecar, Guava support, etc.)
 * is inherited unchanged from NodeMap.
 */
public class GlobalMappings extends NodeMap {

    public final static GlobalMappings GLOBALS = new GlobalMappings();

    private final ReadWriteLock rw = new ReentrantReadWriteLock();
    private final Lock r = rw.readLock();
    private final Lock w = rw.writeLock();

    @Override
    public void put(Object key, Object value) {
        w.lock();
        try {
            super.put(key, value);
        } finally {
            w.unlock();
        }
    }

    @Override
    public ArrayNode get(Object key) {
        r.lock();
        try {
            return super.get(key);
        } finally {
            r.unlock();
        }
    }

    /** Optional: thread-safe POJO retrieval (sidecar is concurrent, but we guard read symmetry). */
    @Override
    public Object getPojo(Object key) {
        r.lock();
        try {
            return super.getPojo(key);
        } finally {
            r.unlock();
        }
    }

    /** Optional: typed POJO accessor with read lock. */
    @Override
    public <T> T getPojo(Object key, Class<T> type) {
        r.lock();
        try {
            return super.getPojo(key, type);
        } finally {
            r.unlock();
        }
    }

    // --- Tiny demo ---
    public static void main(String[] args) throws InterruptedException {
        GlobalMappings gm = new GlobalMappings();

        // Writer thread
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                gm.put("nums.arrayA[" + i + "].val", i);
            }
        });

        // Reader thread (concurrent)
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                JsonNode n = gm.get("nums.arrayA.val");
                System.out.println("read last val -> " + (n == null ? "null" : n.toString()));
                try { Thread.sleep(10); } catch (InterruptedException ignored) {}
            }
        });

        t1.start(); t2.start();
        t1.join();  t2.join();

        System.out.println("\nFinal state:");
        System.out.println(gm.multi().toPrettyString());
    }
}
