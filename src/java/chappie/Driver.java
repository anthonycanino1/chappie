/* ************************************************************************************************
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * Copyright 2017 SUNY Binghamton
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 * ***********************************************************************************************/

package chappie;

import java.lang.reflect.Method;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class Driver {
  public static void main(String[] args) throws IOException {
    int iters = 1;
    if (args.length > 0)
      iters = Integer.parseInt(args[0]);

    System.out.println("running test program for " + iters + " iterations");
    for (int i = 0; i < iters; i++) {
      Chaperone.start();

      Thread[] threads = new Thread[1];
      for (int k = 0; k < 1; ++k) {
        threads[k] = new Thread() {
          public void run() {
            try {
              long start = System.currentTimeMillis();
              int i = 0;
              ReentrantLock lock = new ReentrantLock();
              while(System.currentTimeMillis() - start < 1000) {
                lock.lock();
                i++;
                lock.unlock();
                Thread.sleep(1);
              }
              // System.out.println("going to sleep now...");
            } catch(InterruptedException e) { }
          }
        };
        threads[k].start();
      }

      for (int k = 0; k < 1; ++k)
        try {
          threads[k].join();
        } catch(InterruptedException e) { }

      Chaperone.stop();
    }
  }
}
