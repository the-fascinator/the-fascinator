/*
 * The Fascinator - Common Library
 * Copyright (C) 2008-2010 University of Southern Queensland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.googlecode.fascinator.common;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for DummyFileLock
 *
 * @author Greg Pendlebury
 */
public class DummyFileLockTest {
    // Threading
    private Random generator = new Random();
    private int complexI = 0;
    private Boolean[] completed = new Boolean[10];

    // General locking
    private DummyFileLock file_lock;
    private String file_name;

    @Before
    public void setup() throws Exception {
        // Create a temp file
        String tmp_dir = System.getProperty("java.io.tmpdir");
        file_name = tmp_dir + "/test.lock";
        File lock_file = new File(file_name);
        if (!lock_file.exists()) lock_file.createNewFile();

        // Wrap a dummy lock around it
        file_lock = new DummyFileLock(file_name);
    }

    /**
     * Multi-threaded lock and release test
     *
     * @throws Exception if any error occurs
     */
    @Test
    public void complexLock() throws Exception {
        int THREAD_COUNT = 10;
        System.out.println("=========================");
        System.out.println("DummyFileLockTest::complexLock() Starting... " + THREAD_COUNT + " threads");

        // A collection of test threads
        Collection<Callable<Boolean>> lockTest = new ArrayList<Callable<Boolean>>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            // Init return value test
            completed[i] = false;

            // Thread logic
            lockTest.add(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    int delay = generator.nextInt(1000);
                    int i = incCounter();

                    try {
                        System.out.println("Thread " + i + " starting.");
                        Thread.sleep(delay);
                        file_lock.getLock();
                        System.out.println("Thread " + i + " lock acquired.");
                        Thread.sleep(delay);
                        System.out.println("Thread " + i + " releasing lock.");
                        file_lock.release();
                        completed[i] = true;
                        return true;
                    } catch(IOException e) {
                        Assert.fail("Locking failure");
                        return false;
                    } catch(InterruptedException e) {
                        Assert.fail("Thread abnormally interrupted");
                        return false;
                    }
                }
            });
        }

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        executorService.invokeAll(lockTest);

        for (int i = 0; i < THREAD_COUNT; i++) {
            Assert.assertTrue(completed[i]);
        }
 
        System.out.println("DummyFileLockTest::complexLock() Finished");
        System.out.println("=========================");
    }

    // Thread safe counter increment method.
    private synchronized int incCounter() {
        return complexI++;
    }
}
