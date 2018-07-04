/* ************************************************************************************************
* Copyright 2017 SUNY Binghamton
* Permission is hereby granted, free of charge, to any person obtaining a copy of this
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

package chappie_test;

import chappie_test.*;

import java.util.List;
import java.util.Arrays;

import java.io.*;

import net.openhft.affinity.Affinity;

import java.util.BitSet;

public abstract class Benchmark implements Runnable {

  static Boolean socketOne = true;

  static BitSet socket_1 = new BitSet(40);
  static BitSet socket_2 = new BitSet(40);
  static {
    socket_1.set(0, 20);
    socket_2.set(20, 40);
  }

  private Boolean socket;

  protected Benchmark() {
    this.socket = socketOne;
    socketOne = !socketOne;
  }

  public void run() {
    // synchronized(socket) {
    // if (this.socket == true)
    Affinity.setAffinity(socket_1);
    // else
      // Affinity.setAffinity(socket_2);

      // socket = !socket;
    // }

    work();
  }

  public abstract void work();

  public static void main(String[] args) throws IOException {
    int count = Integer.parseInt(args[0]);
    System.out.println("Making " + count + " thread" + (count != 1 ? "s" : ""));

    Thread[] threads = new Thread[count];
    for(int i = 0; i < threads.length; ++i) {
      // if (i % 2 == 0)
      threads[i] = new Thread(new MatrixMultiplication(250, 250));
      //   // threads[i] = new Thread(new BusyWaiting());
      // else
        // threads[i] = new Thread(new MatrixMultiplication(125, 125));
        // threads[i] = new Thread(new BusyWaiting());
    }

    for(int i = 0; i < threads.length; ++i) {
      threads[i].start();
      // try {
      //   Thread.sleep(10);
      // } catch(Exception e) { }
    }

    // try {
    // Thread.sleep(5000);
    // } catch(Exception e) { }

    for(int i = 0; i < threads.length; i++)
      try {
        threads[i].interrupt();
        threads[i].join();
        System.out.println(threads[i].getName() + " finished");
      } catch (InterruptedException e) { }

    // for(int i = 1; i < threads.length; i+=2)
    //   try {
    //     threads[i].interrupt();
    //     threads[i].join();
    //     System.out.println(threads[i].getName() + " finished");
    //   } catch (InterruptedException e) { }


    // List<Thread> threads = new ArrayList<Thread>();
    //
    // int iterations = 100;
    // if (System.getenv("ITER") != null)
    //   iterations = Integer.parseInt(System.getenv("ITER"));
    // System.out.println("Product iterations set to " + iterations + ".");
    //
    // int[] dim = new int[]{100};
    //
    // int i = 0;
    // int j = 0;
    // if (System.getenv("DIM") != null) {
    //   List<String> items = Arrays.asList(System.getenv("DIM").split("\\s*,\\s*"));
    //
    //   dim = new int[items.size()];
    //   for(String size: items) {
    //     try {
    //       dim[i] = Integer.parseInt(size);
    //       i++;
    //     } catch(Exception e) {
    //       j++;
    //     }
    //   }
    // }
    //
    // Thread[] threads = new Thread[i + j];
    //
    // for(int n = 0; n < i; ++n) {
    //   threads[n] = new Thread(new ChappieBench(iterations, dim[n], 0));
    //   System.out.println("Thread-" + n + " (" + dim[n] + 'x' + dim[n] + ") born.");
    // }
    //
    // for(int n = i; n < i + j; ++n) {
    //   threads[n] = new Thread() {
    //     public void run() {
    //       while(!isInterrupted());
    //       return;
    //     }
    //   };
    //   System.out.println("Thread-" + n + " (BUSY) born.");
    // }

    // for(int n = 0; n < i + j; ++n)
    //   threads[n].start();
    //
    // for(int n = 0; n < i; ++n) {
    //   try {
    //     threads[n].join();
    //   } catch (InterruptedException e) { }
    // }
  }
}
