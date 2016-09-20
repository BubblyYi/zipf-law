// Copyright 2003-2009 Bill Manaris, Dana Hughes, J.R. Armstrong, Thomas Zalonis, Luca Pellicoro, 
//                     Chris Wagner, Chuck McCormick
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
//


package nevmuse.utilities;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Enumeration;

/**
 * This class contains static methods that calculate the slope and R^2 of a trendline
 * of a Zipf distribution (byRank or bySize).
 * <br>
 * <br>The byRank distribution plots the values (y-axis) against the ranks of the values from largest to smallest 
 * (x-axis) in log-log scale. The ranks are generated automatically.
 * <br>
 * <br>The bySize distribution plots the values (y-axis) against the supplied keys (x-axis) in log-log scale.
 * <br>
 * <br>
 * <b>NOTE: </b> The provided methods are static, so call them as Zipf.byRank(values) or Zipf.bySize(keys, values).
 * <br>
 * <br> 
 * @author Luca Pellicoro, Chris Wagner, Bill Manaris (based on VB code by Chuck McCormick and Bill Manaris )
 *
 * @version 1.6 (October 19, 2009) Dana Hughes
 *     - Replaced IllegalArgumentException when no values are passed with slope = 0 and r2 = 0.
 *       This allows for batch processes to continue without exiting due to this exception.
 *       Also, numeric values may be more meaningful.
 * @version 1.5 (November 3, 2009) Thomas Zalonis
 *     - Translated the latest zipf.py update (see zipf.py update message below) to Zipf.java 
 * 	 zipf.py version 1.5 (December 24, 2008)  J.R. Armstrong and Bill Manaris
 *     		- Now we are differentiating between monotonous and random phenomena (vertical vs. horizontal trendlines).
 *       	  In the first case, we return slope = 0 and r2 = 0.
 *       	  In the second case, we return slope = 0 and r2 = 1.
 *       	  Also, some variable names have been updated.
 *
 * @version 1.2 (Jan  2007) Luca Pellicoro
 *      - Static Methods only (no more class instantiation)
 *      - Raising exceptions intead of Assert statements
 *      - Zero values and zero keys are considered erroneous input (raise IllegalArgumentException)
 * @version 1.1 (July 30, 2005)
 * @version 1.0 (May 10, 2003)
 */

public class Zipf 
{
   /*
   public static void main(String[] args)
   {
      // numbers can be entered from the command line as "java Zipf 1 2 2 3 3 3 3" or 
      // any other sequence of numbers by uncommenting the code below.

      //double[] phen = new double[args.length];
      //for(int i = 0;i < args.length;i++)
      //{
      //   phen[i] = (int)Double.parseDouble(args[i]);
      //}

      //double[] phen = {1, 1, 1};             // check monotonous
      //double[] phen = {2, 2, 2, 3, 3, 3};    // check uniformly distributed (white noise)
      //double[] phen = {1, 1, 2};             // check truly zipfian (pink noise)    
      //double[] phen = {1, 1, 1, 1, 2};       // check brown noise
      double[] phen = {1, 2, 2, 3, 3, 3, 3}; // check general case

      System.out.print("Given the sequence: ");
      for(int i = 0;i < phen.length;i++)
      {
         System.out.print(phen[i] + ", ");
      }
      System.out.println();

      // calculate frequency of occurrence of each symbol
      Hashtable histogram = new Hashtable();

      for(int i = 0;i < phen.length;i++)
      {
         if(histogram.containsKey(phen[i]))
         {
            int currentValue = ((Integer)histogram.get(phen[i])).intValue();

            histogram.put(phen[i], new Integer(currentValue + 1));
         }
         else
         {
            histogram.put(phen[i], new Integer(1)); 
         }
      }

      // next, extract the counts and calculate their rank-frequency (Zipfian) distribution
      double[] counts = new double[histogram.size()];
      int i = 0;
      for (Enumeration e = histogram.keys(); e.hasMoreElements();) 
      {
         counts[i] = (double)((Integer)histogram.get(e.nextElement())).intValue();
         i++;
      }

      double[] result = byRank(counts);
      double slope = result[0];
      double r2    = result[1];

      System.out.println("The byRank slope is " + slope + " and the R2 is " + r2);

      // now, extract the sizes calculate their side-frequency (Zipfian) distribution
      double[] sizes = new double[histogram.size()];
      i = 0;
      for (Enumeration e = histogram.keys(); e.hasMoreElements();) 
      {
         sizes[i] = ((Double)e.nextElement()).doubleValue();
         i++;
      }

      result = bySize(sizes, counts);
      slope = result[0];
      r2    = result[1];

      System.out.println("The bySize slope is " + slope + " and the R2 is " + r2);
   }
   */

	/**
     *      
	 * Calculate the slope and R^2 of the rank-frequency distribution of the provided frequencies.  
	 * Ranks will be automatically generated.
	 * <br>
	 * <br>
	 * '''NOTE:''' Caller does not need to sort the frequencies.
	 * <br>
	 * <br>
	 * @param frequencies	The values whose rank-frequency distribution to calculate (y-axis).
	 * 
	 * @return  A double array containing slope (at index 0) and R^2 (at index 1).
	 */
    public static double[] byRank(double[] frequencies)
    {
        
        int numberOfValues = frequencies.length;
        
        // Step 1 and 2: Sort the vals and create keys
		// Copy vals so sort doesn't alter it.
		double[] newValues = new double[numberOfValues];
		double[] keys = new double[numberOfValues];
		for(int i = 0; i < numberOfValues; i++)
		{
		      keys[i] = numberOfValues - i;
		      newValues[i] = frequencies[i];
      }

		Arrays.sort(newValues);

		checkKeysAndValues(keys, newValues);
		
		// Step 3: Get Zipf Slope and R2 of keys/values.
        return calculateSlopeR2(keys, newValues);
    }
    
   /**
    * Calculate the slope and R^2 of the size-frequency distribution of the provided sizes and frequencies.
	* <br>
	* <br>
	* '''NOTE:''' Caller does not need to sort the sizes or frequencies provided.
	* <br>
	* <br>
    * @param sizes	The sizes (x-axis).
    * @param frequencies	The frequencies (y-axis).
    * 
	* @return  An double array containing slope (at index 0) and R^2 (at index 1).    
	*/
    public static double[] bySize(double[] sizes, double[] frequencies)
    {   

        checkKeysAndValues(sizes, frequencies);    
        // NOTE:  There is no need to sort the parallel arrays of keys and vals, since
	    //        getSlopeR2() does not care if the keys are sorted in any particular order;
	    //        it cares only that the association between keys[i] and vals[i] is correct.
        
        return calculateSlopeR2(sizes, frequencies);
    }
    
    
    
    /*******************************
	 * SUPPORTING METHODS
	 *******************************/	 
    
    /**
    * Checks if provided data is relatively error free.  In particular, it will raise exceptions if
    *   - a data array is empty
    *   - keys and values do not contain the same number of elements
    *   - a data array contains negative or zero elements   
    */
    private static void checkKeysAndValues(double[] keys, double[] values) 
    {
        // NOTE:  The first exception (keys or values contain no elements) has been replaced with
        //        setting the slope and r2 values to 0.  This allows for batch operations to be 
        //        performed without generating an Exception, or requiring the use of NaN's.

        // if (keys.length == 0 || values.length == 0)
        //     throw new IllegalArgumentException ("Data (values and keys) must contain at least one element.");
            
        if (keys.length != values.length)
            throw new IllegalArgumentException("Keys and values must have the same length  (keys length was " + keys.length + " and values length was " + values.length + ").");
            
        for (int i = 0; i < values.length; i++)
            if (keys[i] <= 0.0 || values[i] <= 0.0)
              throw new IllegalArgumentException ("Data must be positive: keys[" + i + "] was " + keys[i]  + " and values[" + i + "] was " + values[i] );
    }
    
    
	/**
	 * Calculates the linear regression (slope and R^2 (fit)) of a set of keys and values.
	 * If slope and/or R^2 cannot be calculated, zero is returned.
	 * 
	 * @param keys	The keys for the set
	 * @param vals	The values for the set
	 * 
	 * @return An array of doubles (slope is stored in index 0 and R^2 (fit) in index 1).
	 */
	private static double[] calculateSlopeR2(double[] keys, double[] vals) 
	 {
	 
		// Log10(keys) is mapped to the X axis.
		// Log10(vals) is mapped to the Y axis.
		double sumX = 0;	// holds the sum of X values.
		double sumY = 0;	// holds the sum of Y values.
		double sumXY = 0;	// holds the sum of X*Y values.
		double sumX2 = 0;	// holds the sum of X*X values.
		double sumY2 = 0;	// holds the sum of Y*Y values.
		double[] sr2 = new double[2];	// holds the slope and r2 to be returned (slope is stored in index 0 and R^2 in index 1)

      // one exterme case:
      // if the phenomenon is monotonous (only one type of event, e.g., ['a', 'a', 'a']),
      // then the slope is negative infinity (cannot draw a line with only one data point),
      // so indicate this with slope = 0 AND r2 = 0
      if (keys.length == 1)
      {
         sr2[0] = 0.0;
         sr2[1] = 0.0;
      }
      // another extreme case (added 10/20/10):
      // if the phenomenon contains no information (i.e., no statistical data exists),
      // then the slope is undefined.  Rather than causing an Exception, indicate this
      // with a slope = 0 AND r2 = 0.  Classes utilizing Zipf can always override this
      // prior to calling and set these values to NaN, if desired.
      else if (keys.length == 0)
      {
         sr2[0] = 0.0;
         sr2[1] = 0.0;
      }
      
      else
      {
        // the other extreme case:
        // if the phenomenon is uniformly distributed (several types of events,
        // but all having the same number of instances, e.g., ['a', 'b', 'a', 'b', 'a', 'b']),
        // then the slope = 0 and r2 = 1 (a horizontal line).

        // check if all counts are equal
        int i = 0;
        boolean allCountsEqual = true; // assume they are all equal
        while(allCountsEqual && i < (keys.length - 1))
        {
           allCountsEqual = (vals[i] == vals[i + 1]); // update hypothesis
           i = i + 1;
        }

        if (allCountsEqual) // is phenomenon uniformly distributed?
        {
           sr2[0] = 0.0;
           sr2[1] = 1.0;
        }
        else // general case, so caluclate actual slope and r2 values
        {

            // Sum up the values for the calculations.
            for (i = 0; i < keys.length; i++) 
            {
                //System.out.print(" " + i + " ");  
               sumX += log10(keys[i]);
               sumY += log10(vals[i]);
               sumXY += log10(keys[i]) * log10(vals[i]);
               sumX2 += log10(keys[i]) * log10(keys[i]);
               sumY2 += log10(vals[i]) * log10(vals[i]);
            }

            // calculate the slope
            if ((keys.length * sumX2 - sumX * sumX) == 0)  // check for division by zero (below)
               sr2[0] = 0;
            else
               sr2[0] = ((keys.length * sumXY - sumX * sumY) / (keys.length * sumX2 - sumX * sumX));

            // If you want to create the line: y = mx + b
            // m = slope
            // This calculates b.
            // double b = (sumY - sr2[0] * sumX) / keys.length;

            // calculate the R^2
            if (Math.sqrt((keys.length * sumX2 - sumX * sumX) * (keys.length * sumY2 - sumY * sumY)) == 0)   // check for division by zero (below)
               sr2[1] = 0;
            else
               sr2[1] = (keys.length * sumXY - sumX * sumY) / Math.sqrt((keys.length * sumX2 - sumX * sumX) * (keys.length * sumY2 - sumY * sumY));
            sr2[1] = sr2[1] * sr2[1];  // get the square
        }
      }

		// return the result (slope is stored in index 0 and R^2 in index 1)
		return sr2;
	}

	/**
	 * The natural log of 10
	 */
	private static final double LN_10 = 2.3025850929940456840179914546844;	

	/**
	 * Calculate the Log base 10 of a number.
	 * This is required because Math.log is not Log(10) but Ln (natural Log).
	 * 
	 * Note: Log(b) n = Ln n / Ln b.
	 * 
	 * @param n	The original number.
	 * 
	 * @return Log(10) n
	 */
	private static double log10(double n) {
		return Math.log(n)/LN_10;
	}
    
    


}

