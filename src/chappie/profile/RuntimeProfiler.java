// /* ************************************************************************************************
//  * Permission is hereby granted, free of charge, to any person obtaining a copy of this
//  * Copyright 2019 SUNY Binghamton
//  * software and associated documentation files (the "Software"), to deal in the Software
//  * without restriction, including without limitation the rights to use, copy, modify, merge,
//  * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
//  * persons to whom the Software is furnished to do so, subject to the following conditions:
//  *
//  * The above copyright notice and this permission notice shall be included in all copies or
//  * substantial portions of the Software.
//  *
//  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
//  * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
//  * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
//  * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
//  * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
//  * DEALINGS IN THE SOFTWARE.
//  * ***********************************************************************************************/
//
// package chappie.profile;
//
// import java.io.FileWriter;
//
// import org.apache.commons.csv.CSVPrinter;
//
// public class RuntimeProfiler implements Profiler {
//   public RuntimeProfiler(Config config) { this.config = config; }
//
//   public void sample(int epoch, long timestamp) {
//     if (start == -1) {
//       start = System.nanoTime();
//     }
//   }
//
//   private void dump() {
//     long runtime = System.currentTimeMillis() - start;
//     double[] raplReading = jrapl.EnergyCheckUtils.getEnergyStats();
//
//     CSVPrinter printer = new CSVPrinter(
//       new FileWriter(config.workDirectory + Chaperone.iteration + "/runtime.csv"),
//       CSVFormat.DEFAULT.withHeader("epoch", "timestamp", "elapsed", "activity").withDelimiter(";")
//     );
//
//     // double package1 = raplReading[2] - initialRaplReading[2];
//     // double package2 = raplReading[5] - initialRaplReading[5];
//     // double dram1 = raplReading[0] - initialRaplReading[0];
//     // double dram2 = raplReading[3] - initialRaplReading[3];
//
//     printer.printRecord("runtime", runtime);
//     // printer.printRecord("main_id", GLIBC.getProcessId());
//     // printer.printRecord("package1", package1);
//     // printer.printRecord("package2", package2);
//     // printer.printRecord("dram1", dram1);
//     // printer.printRecord("dram2", dram2);
//
//     printer.close();
//   }
// }
