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

package chappie.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

public class JSON {
  // This takes any map and writes a json from it, assuming its entry types
  // support to string. There is certainly a tool that does this but it
  // seems overkill
  public static void write(Map records, String path) throws IOException {
    PrintWriter writer = new PrintWriter(new FileWriter(path));

    writer.println("{");

    int size = records.size();
    int i = 0;
    Set<Map.Entry> recordSet = records.entrySet();
    for (Map.Entry record: recordSet) {
      String message = "  \"" + record.getKey().toString() + "\": \"" + record.getValue().toString() + "\"" + (++i < size ? "," : "");
      writer.println(message);
    }

    writer.println("}");

    writer.close();
  }
}
