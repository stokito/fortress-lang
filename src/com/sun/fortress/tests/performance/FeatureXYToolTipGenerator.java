/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************/

package com.sun.fortress.tests.performance;

import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.data.xy.XYDataset;

public class FeatureXYToolTipGenerator extends StandardXYToolTipGenerator {

    /**
     * Make eclipse happy
     */
    private static final long serialVersionUID = 3334553893280033302L;

    @Override
    public String generateToolTip(XYDataset dataset,
            int series, int item) {        
        boolean feature = false;
        String result = null;
        double xvalue = dataset.getXValue(series, item);
        double yvalue = dataset.getYValue(series, item);        
        if ((item == 0) || (item == (dataset.getItemCount(series) - 1))) {
            feature = true;
        } else {
            double previous = dataset.getYValue(series, item - 1);
            double next = dataset.getYValue(series, item + 1);
            
            /** local min/max */
            if ( (yvalue > previous) && (yvalue > next) ) {
                feature = true;
            } else if ( (yvalue < previous) && (yvalue < next) ) {
                feature = true;
            }
        }
        if (feature) {
            result = "(r" + xvalue + ", " + yvalue + " sec)";
        }
        return result;
    }

}
