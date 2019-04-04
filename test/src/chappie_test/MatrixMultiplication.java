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

public class MatrixMultiplication extends Benchmark {

  private int size;
  private int iterations;

  private int[][] matrix;

  public MatrixMultiplication(int size, int iterations) {
    super();

    this.size = size;
    this.iterations = iterations;

    matrix = new int[this.size][this.size];
    for(int i = 0; i < this.size; ++i)
      for(int j = 0; j < this.size; ++j)
        matrix[i][j] = 0;

  }

  public void work() {
    for(int n = 0; n < this.iterations; ++n) {
      int[][] resultMatrix = new int[size][size];
      for(int i = 0; i < this.size; ++i)
        for(int j = 0; j < this.size; ++j) {
          resultMatrix[i][j] = 0;
          for(int k = 0; k < size; ++k)
            resultMatrix[i][j] += matrix[i][k] * matrix[k][j];
        }

      matrix = resultMatrix;
    }
  }
}
