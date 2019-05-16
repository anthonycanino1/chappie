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

package chappie.input;

import chappie.input.Config;
import chappie.input.Config.Mode;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;

public class Parser {
  private Config config;

  Parser(String configPath) {
    // Setup the document parser
    try {
      DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Element root = builder.parse(new File(configPath)).getDocumentElement();

      // Grab the chappie values and make a config
      String workPath = root.getElementsByTagName("workPath").item(0).getTextContent();
      Mode mode = Mode.valueOf(root.getElementsByTagName("mode").item(0).getTextContent());
      if (mode != Mode.NOP) {
        int timerRate = Integer.parseInt(root.getElementsByTagName("timerRate").item(0).getTextContent());
        int vmFactor = Integer.parseInt(root.getElementsByTagName("vmFactor").item(0).getTextContent());
        int hpFactor = Integer.parseInt(root.getElementsByTagName("hpFactor").item(0).getTextContent());
        int osFactor = Integer.parseInt(root.getElementsByTagName("osFactor").item(0).getTextContent());
        int raplFactor = Integer.parseInt(root.getElementsByTagName("raplFactor").item(0).getTextContent());
        config = new Config(workPath, mode, timerRate, vmFactor, hpFactor, osFactor, raplFactor);
      } else {
        config = new Config(workPath);
      }

    } catch(Exception e) {
      System.out.println("Couldn't parse " + configPath + ":");
      e.printStackTrace();
      config = null;
    }
  }

  public Config getConfig() {
    return config;
  }
}
