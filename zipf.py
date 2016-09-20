# Copyright 2003-2009 Bill Manaris, Dana Hughes, J.R. Armstrong, Thomas Zalonis, Luca Pellicoro, 
#                     Chris Wagner, Chuck McCormick
#
#    This program is free software: you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with this program.  If not, see <http://www.gnu.org/licenses/>.
#

# zipf.py       Version 1.5          24-Dec-2008
#                                                                
# This module encapsulates functions that may be used to calculate
# the slope and r2 (fit) of a trendline
# of a Zipf distribution (byRank or bySize).
# 
# The byRank distribution plots the values (y-axis)
# against the ranks of the values from largest to smallest 
# (x-axis) in log-log scale. The ranks are generated automatically.
# 
# The bySize distribution plots the values (y-axis)
# against the supplied keys (x-axis) in log-log scale.
# 
# Usage: Call bySize(sizes, counts) and/or byRank(counts) functions 
# Output: slope and R2 
# 
# WARNING:  If an error occurs the current code will NOT raise an exception;
#           it will only print an error message (for ShedSkin compatibility).
#           This may cause problems, if the error messages go undetected
#           (e.g., this code is run in batch mode).
# 
# Authors: Chris Wagner and Bill Manaris (based on VB code by Chuck McCormick and Bill Manaris)
# 
# version 1.5 (December 24, 2008)  J.R. Armstrong and Bill Manaris
#     - Now we are differentiating between monotonous and random phenomena (vertical vs. horizontal trendlines).
#       In the first case, we return slope = 0 and r2 = 0.
#       In the second case, we return slope = 0 and r2 = 1.
#       Also, some variable names have been updated.
# 
# version 1.4 (October 1, 2008) Bill Manaris
#     - Added more unit-testing code (i.e., if __name__=='__main__') for Shed Skin Python-to-C++ conversion to work.
#     - Updated some variable names for usability/readability
# 
# version 1.3 (March 23, 2007) Thomas Zalonis
#     - Added code to the getSlopeR2() function that calculates the y-intercept for the trendline.
#     - getSlopeR2() now returns 3 values, slope, r2 and the trendline y-intercept
# 
# version 1.2 (Feb 03, 2007) Luca Pellicoro 
#     -Translation from Java to Python
#     -Raise exceptions with erroneous user inputs (such as zero keys or values)
#     
# version 1.1 (July 30, 2005)
#
# version 1.0 (May 10, 2003)  
#

# for logarithmic calculations
from math import * 


def byRank(counts):
    '''
    Calculate the slope and R^2 of the counts. 
    Sorting the counts in descending order.
    '''
    
    
    newCounts = []   # to hold the deep copy
    newRanks = []    # the newly created ranks
    numberOfCounts = len(counts)
    for index in range(numberOfCounts):
            newCounts.append(counts[index])         # deep copy the counts
            newRanks.append(numberOfCounts - index) # create the ranks: highest frequency has smallest rank
             
            
    
    newCounts.sort()
        
    checkRanksAndCounts(newRanks, newCounts)

    return getSlopeR2(newRanks, newCounts)
    
def bySize(sizes, counts):
    '''
    Calculate the slope and r2 of the counts without ordering the ranks.
    Keys contains the desired ranking.
    '''
    
      
    checkRanksAndCounts(sizes,counts)
     
        
    return getSlopeR2(sizes, counts)

    
######################################
######### SUPPORTING METHODS #########
######################################

def checkRanksAndCounts(ranks, counts):
    '''
    Verify that:
        - ranks and counts contain at least one element
        - ranks and counts have the same length
        - both ranks and counts do not contain any negative or zero element
    '''

    if len(counts) == 0:  raise ValueError, 'Counts should contain at least one element'
    if min(counts) <= 0.0: raise ValueError, 'Counts should be strictly positive: %f' % (min(counts))
    
    if len(ranks) == 0:  raise ValueError, 'Ranks should contain at least one element'
    if min(ranks) <= 0.0 : raise ValueError, 'Ranks should be strictly positive: %f' % (min(ranks))
    
    
    if len(ranks) != len(counts): 
        raise ValueError,'Ranks (length: %d) and counts (length: %d) should have the same size.'  % (len(ranks), len(counts)) 

##    # Comment the above exception code, and uncomment the code below,
##    # for ShedSkin compatibility.

##    if len(counts) == 0: print "Zipf ValueError: ", 'Counts should contain at least one element'
##    if min(counts) <= 0.0: print "Zipf ValueError: ", 'Counts should be strictly positive: %f' % (min(counts))
##    
##    if len(ranks) == 0:  print "Zipf ValueError: ", 'Ranks should contain at least one element'
##    if min(ranks) <= 0.0 : print "Zipf ValueError: ", 'Ranks should be strictly positive: %f' % (min(ranks))
##    
##    if len(ranks) != len(counts): 
##        print "Zipf ValueError: ",'Ranks (length: %d) and counts (length: %d) should have the same size.'  % (len(ranks), len(values)) 
    
    
def getSlopeR2(ranks, counts):
    '''
    Calculates the Zipf Slope and R^2(fit) of a
    set of ranks and counts.
    If slope and/or R^2 cannot be calculated, a zero is returned.
    '''    
    
    assert len(ranks) == len(counts) , 'Ranks and counts must have the same length.'
        
    sumX = sumY = sumXY = sumX2 = sumY2 = 0.0
    
    numberOfRanks = len(ranks)

    # one exterme case:
    # if the phenomenon is monotonous (only one type of event, e.g., ['a', 'a', 'a']),
    # then the slope is negative infinity (cannot draw a line with only one data point),
    # so indicate this with slope = 0 AND r2 = 0
    if numberOfRanks == 1:
        slope = 0.0
        r2 = 0.0
        
    else:
        # the other extreme case:
        # if the phenomenon is uniformly distributed (several types of events,
        # but all having the same number of instances, e.g., ['a', 'b', 'a', 'b', 'a', 'b']),
        # then the slope = 0 and r2 = 1 (a horizontal line).

        # check if all counts are equal
        i = 0
        allCountsEqual = True   # assume they are all equal
        while allCountsEqual and i < numberOfRanks-1:
            allCountsEqual = (counts[i] == counts[i + 1])   # update hypothesis
            i = i + 1

        if allCountsEqual:  # is phenomenon uniformly distributed?
            slope = 0.0
            r2 = 1.0

        # general case, so calculate actual slope and r2 values
        else:
            
            # Sum up the values for the calculations
            for index in range(numberOfRanks):
                sumX += log(ranks[index],10)
                sumY += log(counts[index],10)
                sumXY += log(ranks[index],10) * log(counts[index],10)
                sumX2 += log(ranks[index],10)**2
                sumY2 += log(counts[index],10)**2                
                
            # calculate the slope
            if ((numberOfRanks * sumX2 - sumX * sumX) == 0.0):
                slope = 0.0
            else:
                slope = ((numberOfRanks * sumXY - sumX * sumY) / (numberOfRanks * sumX2 - sumX * sumX))
                
            # calculate the r2
            if(sqrt((numberOfRanks * sumX2 - sumX * sumX) * (numberOfRanks * sumY2 - sumY * sumY)) == 0.0):
                r2 = 0.0
            else:
                r = (numberOfRanks * sumXY - sumX * sumY) / sqrt((numberOfRanks * sumX2 - sumX * sumX) * (numberOfRanks * sumY2 - sumY * sumY))
                r2 = r * r

    # calulate y-intercept
    yint = (sumY - slope * sumX) / len(ranks)
        
    return slope, r2, yint

if __name__ == '__main__':
    #print "Enter sequence of numbers to calculate its Zipfian distribution."
    #print "The rank-frequency distribution is calculated based on how many times each number appears."
    #print "The size-frequency distribution is calculated based on how many times each number appears; also the actual number is treated as if it represents 'size'."
    #phenomenon = input("Enter sequence of numbers (e.g., [50, 100, 50]): ")

    #phenomenon = [1, 1, 1]             # check monotonous
    #phenomenon = [2, 2, 2, 3, 3, 3]    # check uniformly distributed (white noise)
    #phenomenon = [1, 1, 2]             # check truly zipfian (pink noise)    
    #phenomenon = [1, 1, 1, 1, 2]       # check brown noise
    phenomenon = [1, 2, 2, 3, 3, 3, 3] # check general case
    print "Given the sequence", phenomenon

    # calculate frequency of occurrence of each symbol
    histogram = {}
    for event in phenomenon:
       histogram[event] = histogram.get(event, 0) + 1
    # now, the histogram contains the frequencies
    
    # next, extract the counts and calculate their rank-frequency (Zipfian) distribution
    counts = histogram.values()
    slope, r2, yint = byRank(counts)
    print "The byRank slope is", slope, "and the R^2 is", r2

    # now, extract the sizes calculate their side-frequency (Zipfian) distribution
    sizes = histogram.keys()
    slope, r2, yint = bySize(sizes, counts)
    print "The bySize slope is", slope, "and the R^2 is", r2
       
    
    
        
        
