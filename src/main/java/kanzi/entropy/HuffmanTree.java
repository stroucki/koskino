/*
Copyright 2011-2013 Frederic Langlet
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
you may obtain a copy of the License at

                http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package kanzi.entropy;

import kanzi.ArrayComparator;
import kanzi.util.sort.QuickSort;


// Tree utility class for a canonical implementation of Huffman codec
public final class HuffmanTree
{
    // Return the number of codes generated
    public static int generateCanonicalCodes(short[] sizes, int[] codes, int[] ranks, int count)
    {
       // Sort by decreasing size (first key) and increasing value (second key)
       if (count > 1)
       {
          QuickSort sorter = new QuickSort(new HuffmanArrayComparator(sizes));
          sorter.sort(ranks, 0, count);
       }
       
       int code = 0;
       int len = sizes[ranks[0]];

       for (int i=0; i<count; i++)
       {
          final int currentSize = sizes[ranks[i]];

          if (len > currentSize)
          {
             code >>= (len - currentSize);
             len = currentSize;
          }

          codes[ranks[i]] = code;
          code++;
       }
       
       return count;
    }
    

    // Huffman node
    public static class Node
    {
       protected final int weight;
       protected final byte symbol;
       protected Node left;
       protected Node right;


       // Leaf
       Node(byte symbol, int frequency)
       {
          this.weight = frequency;
          this.symbol = symbol;
       }


       // Not leaf
       Node(int frequency, Node node1, Node node2)
       {
          this.weight = frequency;
          this.symbol = 0;
          this.left  = node1;
          this.right = node2;
       }
    }


    // Array comparator used to sort keys and values to generate canonical codes
    private static class HuffmanArrayComparator implements ArrayComparator
    {
        private final short[] array;
        

        public HuffmanArrayComparator(short[] array)
        {
            if (array == null)
                throw new NullPointerException("Invalid null array parameter");

            this.array = array;
        }


        @Override
        public int compare(int lidx, int ridx)
        {
            // Check size (reverse order) as first key
            final int res = this.array[ridx] - this.array[lidx];

            // Check index (natural order) as second key
            return (res != 0) ? res : lidx - ridx;
        }
    }  
}