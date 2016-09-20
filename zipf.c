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

/* zipf.c       Version 1.5          24-Dec-2009
 *                                                                
 * This module encapsulates functions that may be used to calculate
 * the slope and r2 (fit) of a trendline
 * of a Zipf distribution (byRank or bySize).
 * 
 * The byRank distribution plots the values (y-axis)
 * against the ranks of the values from largest to smallest 
 * (x-axis) in log-log scale. The ranks are generated automatically.
 * 
 * The bySize distribution plots the values (y-axis)
 * against the supplised keys (x-axis) in log-log scale.
 * 
 * Usage: Call bySize(int *ranks, int numRanks, double *counts, int numCounts) and.or
 *             byRank(double *counts, int numCounts) functions.
 *
 * Output: slope and R2 
 * 
 * WARNING:  If an error occurs the current code will NOT raise an exception;
 *           it will only print an error message (for ShedSkin compatibility).
 *           This may cause problems, if the error messages go undetected
 *           (e.g., this code is run in batch mode).
 * 
 * Authors: Chris Wagner and Bill Manaris (based on VB code by Chuck McCormick and Bill Manaris)
 *
 *          Thomas Zalonis - translate to C.
 *          
 * version 1.5 (December 24, 2008)  J.R. Armstrong and Bill Manaris
 *     - Now we are differentiating between monotonous and random phenomena (vertical vs. horizontal trendlines).
 *       In the first case, we return slope = 0 and r2 = 0.
 *       In the second case, we return slope = 0 and r2 = 1.
 *       Also, some variable names have been updated.
 * 
 * version 1.4 (October 1, 2008) Bill Manaris
 *     - Added more unit-testing code (i.e., if __name__=='__main__') for Shed Skin Python-to-C++ conversion to work.
 *     - Updated some variable names for usability/readability
 * 
 * version 1.3 (March 23, 2007) Thomas Zalonis
 *     - Added code to the getSlopeR2() function that calculates the y-intercept for the trendline.
 *     - getSlopeR2() now returns 3 values, slope, r2 and the trendline y-intercept
 * 
 * version 1.2 (Feb 03, 2007) Luca Pellicoro 
 *     -Translation from Java to Python
 *     -Raise exceptions with erroneous user inputs (such as zero keys or values)
 *     
 * version 1.1 (July 30, 2005)
 *
 * version 1.0 (May 10, 2003)  
 *
 * 
 * Calculates slope and R^2 values of a collection of numbers.
 *
 * Libraries needed:
 * -----------------
 * stdlib for qsort
 *
 */

#include <stdlib.h>
#include <math.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <string.h>

#define FALSE 0
#define TRUE 1

//*****************************************************************************
// This struct is used to return multiple values from byRank()
// ****************************************************************************
struct ZipfValues
{
   float slope;
   float r2;
   float yint;
};


// zipf related 
struct ZipfValues *getSlopeR2(int *, int, double *, int);
int checkRanksAndCounts(int *, int, double *, int);
struct ZipfValues *bySize(int *, int, double *, int);
int compare(const void *, const void *);
struct ZipfValues *byRank(double *, int);


//*****************************************************************************
// The byRank distribution plots the values (y-axis)
// against the ranks of the values from largest to smallest 
// (x-axis) in log-log scale. The ranks are generated automatically.
//*****************************************************************************
struct ZipfValues *byRank(double *counts, int numCounts)
{
   double *newCounts = (double *)malloc(sizeof(double) * numCounts);
   int *newRanks = (int *)malloc(sizeof(int) * numCounts);

   int index;
   for(index=0;index<numCounts;index++)
   {
      newCounts[index] = counts[index];
      newRanks[index]  = numCounts - index;
   }
  
   qsort((void *)newCounts, numCounts, sizeof(double), compare);

   checkRanksAndCounts(newRanks, numCounts, newCounts, numCounts);

   return getSlopeR2(newRanks, numCounts, newCounts, numCounts);
}

//*****************************************************************************
// 'Double' comparison function needed for sorting. This function
// is passed in as a parameter to qsort() in the byRank() function.
//*****************************************************************************
int compare(const void *a, const void *b)
{
   double d = *( (double *) a)  - *( (double *) b );
   
   if(d > 0.0) 
   {
      return 1;
   } 
   else if (d < 0.0) 
   {
      return -1;
   } 
   else 
   {
      return 0;
   }
}

//*****************************************************************************
// The bySize distribution plots the values (y-axis)
// against the supplised keys (x-axis) in log-log scale.
//*****************************************************************************
struct ZipfValues *bySize(int *sizes, int numSizes, double *counts, int numCounts)
{
   checkRanksAndCounts(sizes, numSizes, counts, numCounts);
   return getSlopeR2(sizes, numSizes, counts, numCounts);
}

//*****************************************************************************
// Supporting function for bySize() and byRank(). Checks the passed values
// for correctness.
//*****************************************************************************
int checkRanksAndCounts(int *ranks, int numRanks, double *counts, int numCounts)
{
   if(numCounts == 0)
   {
      fprintf(stderr, "Counts should contain at least one element.\n");
      exit(0);
   }

   if(numRanks == 0)
   {
      fprintf(stderr, "Ranks should contain at least one element.\n");
      exit(0);
   }

   if(numRanks != numCounts)
   {
      fprintf(stderr, "Ranks (%d) and counts (%d) should have the same size.\n", numRanks, numCounts);
      exit(0);
   }

   int i;
   for(i=0;i<numRanks;i++)
   {
      if(ranks[i] <= 0.0)
      {
         fprintf(stderr, "Ranks should be strictly positive.\n");
         exit(0);
      }

      if(counts[i] <= 0.0)
      {
         fprintf(stderr, "Counts and values should be strictly positive.\n");
         exit(0);
      }
   }

   return 0;
}

//*****************************************************************************
// Supporting function for byRank() and bySize(). The actual zipf values 
// (slope, R2 and yint) are calculated in this function and returned
// as a struct.
//*****************************************************************************
struct ZipfValues *getSlopeR2(int *ranks, int numRanks, double *counts, int numCounts)
{
   struct ZipfValues *results = (struct ZipfValues *)malloc(sizeof(struct ZipfValues));
   double sumX, sumY, sumXY, sumX2, sumY2,slope, r2, yint;
   int index;

   sumX = sumY = sumXY = sumX2 = sumY2 = 0.0;

   // one exterme case:
   // if the phenomenon is monotonous (only one type of event, e.g., ['a', 'a', 'a']),
   // then the slope is negative infinity (cannot draw a line with only one data point),
   // so indicate this with slope = 0 AND r2 = 0
   if(numRanks == 1)
   {
      slope = 0.0;
      r2 = 0.0;
   }
   else
   {
      //the other extreme case:
      //if the phenomenon is uniformly distributed (several types of events,
      //but all having the same number of instances, e.g., ['a', 'b', 'a', 'b', 'a', 'b']),
      //then the slope = 0 and r2 = 1 (a horizontal line).

      //check if all counts are equal

      int allCountsEqual = 1;
      for(index=0;(index < numRanks - 1) && allCountsEqual;index++)
      {
         if(counts[index] != counts[index + 1])
            allCountsEqual = 0;
      }

      if(allCountsEqual)
      {
         slope = 0.0;
         r2 = 1.0;
      }
      else // general case, so calculate actual slope and r2 values
      {
         double tmp1,tmp2;
         for(index=0;index<numRanks;index++)
         {
            // only calculating the follow log()s once for efficiency
            tmp1 = log10(ranks[index]);
            tmp2 = log10(counts[index]);

            sumX  += tmp1;
            sumY  += tmp2;
            sumXY += tmp1 * tmp2;
            sumX2 += pow(tmp1, 2);
            sumY2 += pow(tmp2, 2);
         }

         // calculate slope
         if((numRanks*sumX2 - sumX*sumX) == 0.0)
            slope = 0.0;
         else
            slope = ((numRanks*sumXY - sumX*sumY) / (numRanks*sumX2 - sumX*sumX));

         // calculate r2   
         if(sqrt((numRanks*sumX2 - sumX*sumX) * (numRanks*sumY2 - sumY*sumY)) == 0.0)
         {
            r2 = 0.0;
         }
         else
         {
            r2 = (numRanks*sumXY - sumX*sumY)/(sqrt(numRanks*sumX2 - sumX*sumX)*sqrt(numRanks*sumY2 - sumY*sumY));
            r2 = r2 * r2;
         }
      }
   }

   // calculate y-intercept
   yint = (sumY - slope * sumX) / numRanks;

   // packing slope, r2 and yint into a ZipfValues struct
   // so that all three can be returned at once.
   results->slope = slope;
   results->r2    = r2;
   results->yint  = yint;

   return results;
}

