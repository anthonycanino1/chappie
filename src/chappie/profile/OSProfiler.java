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
// import java.io.*;
//
// import java.nio.file.Path;
// import java.nio.file.Paths;
// import java.util.ArrayList;
//
// import java.util.List;
// import java.util.Map;
//
// import chappie.Chaperone.Config;
// import chappie.Chaperone.Mode;
// import chappie.glibc.GLIBC;
//
// import jrapl.EnergyCheckUtils.*;
//
// public class OSProfiler implements Profiler {
//   Config config;
//
//   public OSProfiler(Config config) {
//     this.config = config;
//   }
//
//   // Runtime data containers
//   private ArrayList<ArrayList<Object>> data = new ArrayList<ArrayList<Object>>();
//
//   public void sample(int epoch, long unixTime) {
//     ArrayList<Object> record;
//
//     // read os state
//     if (config.osFactor > 0 && epoch % config.osFactor == 0) {
//       for (File f: new File("/proc/" + GLIBC.getProcessId() + "/task/").listFiles())   {
//         if (config.mode == Mode.SAMPLE) {
//           int tid = Integer.parseInt(f.getName());
//           String threadRecord = GLIBC.getThreadStats(tid);
//           if (threadRecord.length() > 0) {
//             record = new ArrayList<Object>();
//
//             record.add(epoch);
//             record.add(unixTime);
//             record.add(tid);
//             record.add(threadRecord);
//
//             osData.add(record);
//           }
//         }
//       }
//     }
//   }
//
//   public void dump() {
//     if (mode == Mode.SAMPLE) {
//       CSVPrinter printer = new CSVPrinter(
//         new FileWriter(config.workDirectory + "/chappie.os" + config.suffix + ".csv"),
//         CSVFormat.DEFAULT.withHeader("epoch", "timestamp", "tid", "record").withDelimiter(";")
//       );
//
//       printer.printRecords(data);
//       printer.close();
//     }
//   }
// }
