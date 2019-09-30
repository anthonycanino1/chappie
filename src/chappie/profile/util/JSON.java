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
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.Map.Entry;

public class JSON {
  private static String format(Object value) {
    return "\"" + value.toString() + "\"";
  }

  private static String[] toRecords(Map map) {
    ArrayList<String> records = new ArrayList<String>(map.size());

    Set<Entry> recordSet = map.entrySet();
    for (Entry record: recordSet)
      records.add(format(record.getKey()) + ": " + format(record.getValue()));

    return records.toArray(new String[records.size()]);
  }

  public static void write(Map map, String path) throws IOException {
    String[] records = toRecords(map);

    PrintWriter writer = new PrintWriter(new FileWriter(path));

    writer.println("{");
    for (int i = 0; i < records.length; i++)
      writer.println("  " + records[i] + (i + 1 < records.length ? "," : ""));
    writer.println("}");

    writer.close();
  }
}
