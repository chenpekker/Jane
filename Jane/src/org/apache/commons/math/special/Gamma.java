/**
 * The information about where this file comes from is described below. Under
 * the terms of the Apache license, we are required to inform you that we have
 * made modifications to the source code containted in this file.
 * THIS FILE HAS BEEN MODIFIED FROM THE ORIGINAL VERSION DISTRIBUTED
 * BY THE APACHE SOFTWARE FOUNDATION.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.math.special;

/**
 * This is a utility class that provides computation methods related to the
 * Gamma family of functions.
 *
 * @version
 * $Revision: 920558 $ $Date: 2010-03-08 17:57:32 -0500 (Mon, 08 Mar 2010)$
 */
public class Gamma {
    /**
     * <a href="http://en.wikipedia.org/wiki/Euler-Mascheroni_constant">
     * @since 2.0
     */
    public static final double GAMMA = 0.577215664901532860606512090082;

    /**
     * Maximum allowed numerical error.
     */
    private static final double DEFAULT_EPSILON = 10e-15;

    /**
     * Lanczos coefficients.
     */
    private static final double[] LANCZOS =
    {
        0.99999999999999709182,
        57.156235665862923517,
        -59.597960355475491248,
        14.136097974741747174,
        -0.49191381609762019978,
        .33994649984811888699e-4,
        .46523628927048575665e-4,
        -.98374475304879564677e-4,
        .15808870322491248884e-3,
        -.21026444172410488319e-3,
        .21743961811521264320e-3,
        -.16431810653676389022e-3,
        .84418223983852743293e-4,
        -.26190838401581408670e-4,
        .36899182659531622704e-5,
    };

    /**
     * Avoid repeated computation of log of 2 PI in logGamma
     */
    private static final double HALF_LOG_2_PI = 0.5 * Math.log(2.0 * Math.PI);

    // Limits for switching algorithm in digamma
    /**
     * C limit.
     */
    private static final double C_LIMIT = 49;

    /**
     * S limit.
     */
    private static final double S_LIMIT = 1e-5;

    /**
     * Default constructor.  Prohibit instantiation.
     */
    private Gamma() {
        super();
    }

    /**
     * Returns the natural logarithm of the gamma function &#915;(x).
     *
     * The implementation of this method is based on:
     * <ul>
     * <li><a href="http://mathworld.wolfram.com/GammaFunction.html">
     * Gamma Function</a>, equation (28).</li>
     * <li><a href="http://mathworld.wolfram.com/LanczosApproximation.html">
     * Lanczos Approximation</a>, equations (1) through (5).</li>
     * <li><a href="http://my.fit.edu/~gabdo/gamma.txt">Paul Godfrey, A note on
     * the computation of the convergent Lanczos complex Gamma approximation
     * </a></li>
     * </ul>
     *
     * @param x the value.
     * @return log(&#915;(x))
     */
    public static double logGamma(double x) {
        double ret;

        if (Double.isNaN(x) || (x <= 0.0))
            ret = Double.NaN;
        else {
            double g = 607.0 / 128.0;
            double sum = 0.0;

            for (int i = LANCZOS.length - 1; i > 0; --i)
                sum = sum + (LANCZOS[i] / (x + i));

            sum = sum + LANCZOS[0];

            double tmp = x + g + .5;
            ret = ((x + .5) * Math.log(tmp)) - tmp + HALF_LOG_2_PI
                        + Math.log(sum / x);
        }

        return ret;
    }
}
