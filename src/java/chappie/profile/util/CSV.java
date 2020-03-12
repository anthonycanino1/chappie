/* ************************************************************************************************
 * Copyright 2019 SUNY Binghamton
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

package chappie.profile.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Iterable;
import java.util.logging.Logger;

import chappie.profile.Record;

public class CSV {
  // Takes a list of records and writes them to a file as a csv. At this stage,
  // it doesn't make sense to use a complex csv writer to dump data. Eventually,
  // we may have to dump to a database, so I want to keep things open so the
  // same structure can be used universally

  public static void write(Iterable<Record> records, String[] header, String path) throws IOException {
    Logger logger = Logger.getLogger("chappie");
    PrintWriter writer = new PrintWriter(new FileWriter(path));

    writer.println(String.join(";", header));
    for(Record record: records) {
      writer.println(record);
      if (Runtime.getRuntime().freeMemory() < Runtime.getRuntime().totalMemory() / 100)
        writer.flush();
    }

    writer.close();
  }
}
