/*
 * ARX: Powerful Data Anonymization
 * Copyright 2012 - 2015 Florian Kohlmayer, Fabian Prasser
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.deidentifier.arx.risk;

import java.io.Serializable;

import org.deidentifier.arx.ARXPopulationModel;
import org.deidentifier.arx.risk.RiskEstimateBuilder.WrappedBoolean;
import org.deidentifier.arx.risk.RiskEstimateBuilder.WrappedInteger;

/**
 * Class for risks based on uniqueness.
 * 
 * @author Fabian Prasser
 */
public class RiskModelPopulationBasedUniquenessRisk extends RiskModelPopulationBased {

    /**
     * The statistical model used for computing Dankar's estimate.
     * 
     * @author Fabian Prasser
     */
    public static enum StatisticalPopulationModel implements Serializable {
        PITMAN,
        ZAYATZ,
        SNB,
        DANKAR,
    }

    /** Estimate */
    private double                      numUniquesZayatz = -1d;
    /** Estimate */
    private double                      numUniquesSNB    = -1d;
    /** Estimate */
    private double                      numUniquesPitman = -1d;
    /** Estimate */
    private double                      numUniquesDankar = -1d;
    /** Model */
    private StatisticalPopulationModel            dankarModel      = null;
    /** Parameter */
    private int                         numClassesOfSize1;
    /** Parameter */
    private double                      samplingFraction;
    /** Parameter */
    private ARXPopulationModel          model;
    /** Parameter */
    private RiskModelEquivalenceClasses classes;
    /** Parameter */
    private int                         sampleSize;
    /** Parameter */
    private double                      accuracy;
    /** Parameter */
    private int                         maxIterations;
    /** Parameter */
    private WrappedBoolean              stop;

    /**
     * Creates a new instance
     * @param model
     * @param classes
     * @param sampleSize
     */
    public RiskModelPopulationBasedUniquenessRisk(ARXPopulationModel model,
                                                  RiskModelEquivalenceClasses classes,
                                                  int sampleSize) {
        this(model,
             classes,
             sampleSize,
             new WrappedBoolean(),
             new WrappedInteger(),
             RiskEstimateBuilder.DEFAULT_ACCURACY,
             RiskEstimateBuilder.DEFAULT_MAX_ITERATIONS,
             false);
    }

    /**
     * Creates a new instance
     * @param model
     * @param classes
     * @param sampleSize
     * @param stop
     * @param progress
     * @param accuracy
     * @param maxIterations
     * @param precompute
     */
    RiskModelPopulationBasedUniquenessRisk(ARXPopulationModel model,
                                           RiskModelEquivalenceClasses classes,
                                           int sampleSize,
                                           WrappedBoolean stop,
                                           WrappedInteger progress,
                                           double accuracy,
                                           int maxIterations,
                                           boolean precompute) {
        super(classes, model, sampleSize, stop, progress);

        // Init
        this.numClassesOfSize1 = (int) super.getNumClassesOfSize(1);
        this.samplingFraction = super.getSamplingFraction();
        this.model = model;
        this.classes = classes;
        this.sampleSize = sampleSize;
        this.accuracy = accuracy;
        this.maxIterations = maxIterations;
        this.stop = stop;

        // Handle cases where there are no sample uniques
        if (numClassesOfSize1 == 0) {
            numUniquesZayatz = 0d;
            numUniquesSNB = 0d;
            numUniquesPitman = 0d;
            numUniquesDankar = 0d;
            dankarModel = StatisticalPopulationModel.DANKAR;
            progress.value = 100;
            return;
        }
        
        // If precomputation (for interruptible builders)
        if (precompute) {
    
            // Estimate with Zayatz's model
            getNumUniqueTuplesZayatz();
            progress.value = 50;
    
            // Estimate with Pitman's model
            getNumUniqueTuplesPitman();
            progress.value = 75;
    
            // Estimate with SNB model
            getNumUniqueTuplesSNB();
    
            // Decision rule by Dankar et al.
            getNumUniqueTuplesDankar();
            progress.value = 100;
        }
    }

    /**
     * Returns the statistical model, used by Dankar et al.'s decision rule for estimating population uniqueness
     */
    public StatisticalPopulationModel getDankarModel() {
        getNumUniqueTuplesDankar();
        return dankarModel;
    }

    /**
     * Estimated number of unique tuples in the population according to the given model
     */
    public double getFractionOfUniqueTuples(StatisticalPopulationModel model) {
        return getNumUniqueTuples(model) / super.getPopulationSize();
    }

    /**
     * Estimated number of unique tuples in the population according to Dankar's decision rule
     */
    public double getFractionOfUniqueTuplesDankar() {
        return getNumUniqueTuplesDankar() / super.getPopulationSize();
    }

    /**
     * Estimated number of unique tuples in the population according to Pitman's statistical model
     */
    public double getFractionOfUniqueTuplesPitman() {
        return getNumUniqueTuplesPitman() / super.getPopulationSize();
    }

    /**
     * Estimated number of unique tuples in the population according to the SNB statistical model
     */
    public double getFractionOfUniqueTuplesSNB() {
        return getNumUniqueTuplesSNB() / super.getPopulationSize();
    }

    /**
     * Estimated number of unique tuples in the population according to Zayatz's statistical model
     */
    public double getFractionOfUniqueTuplesZayatz() {
        return getNumUniqueTuplesZayatz() / super.getPopulationSize();
    }

    /**
     * Estimated number of unique tuples in the population according to the given model
     */
    public double getNumUniqueTuples(StatisticalPopulationModel model) {
        switch (model) {
        case ZAYATZ:
            return getNumUniqueTuplesZayatz();
        case PITMAN:
            return getNumUniqueTuplesPitman();
        case SNB:
            return getNumUniqueTuplesSNB();
        case DANKAR:
            return getNumUniqueTuplesDankar();
        }
        throw new IllegalArgumentException("Unknown model");
    }

    /**
     * Estimated number of unique tuples in the population according to Dankar's decision rule
     */
    public double getNumUniqueTuplesDankar() {
        if (numUniquesDankar == -1) {
            if (this.numClassesOfSize1 == 0) {
                numUniquesDankar = 0;
                dankarModel = StatisticalPopulationModel.DANKAR;
            } else {
                // Decision rule by Dankar et al.
                if (samplingFraction <= 0.1) {
                    getNumUniqueTuplesPitman();
                    if (isValid(numUniquesPitman)) {
                        numUniquesDankar = numUniquesPitman;
                        dankarModel = StatisticalPopulationModel.PITMAN;
                    } else {
                        getNumUniqueTuplesZayatz();
                        numUniquesDankar = numUniquesZayatz;
                        dankarModel = StatisticalPopulationModel.ZAYATZ;
                    }
                } else {
                    getNumUniqueTuplesSNB();
                    getNumUniqueTuplesZayatz();
                    if (isValid(numUniquesSNB)) {
                        if (numUniquesZayatz < numUniquesSNB) {
                            numUniquesDankar = numUniquesZayatz;
                            dankarModel = StatisticalPopulationModel.ZAYATZ;
                        } else {
                            numUniquesDankar = numUniquesSNB;
                            dankarModel = StatisticalPopulationModel.SNB;
                        }
                    } else {
                        numUniquesDankar = numUniquesZayatz;
                        dankarModel = StatisticalPopulationModel.ZAYATZ;
                    }
                }
            }
        }
        return isValid(numUniquesDankar) ? numUniquesDankar : 0d;
    }

    /**
     * Estimated number of unique tuples in the population according to Pitman's statistical model
     */
    public double getNumUniqueTuplesPitman() {
        if (numUniquesPitman == -1) {
            if (this.numClassesOfSize1 == 0) {
                numUniquesPitman = 0;
            } else {
                numUniquesPitman = new ModelPitman(model, classes, sampleSize, accuracy, maxIterations, stop).getNumUniques();
            }
        }
        return isValid(numUniquesPitman) ? numUniquesPitman : 0d;
    }

    /**
     * Estimated number of unique tuples in the population according to the SNB model
     */
    public double getNumUniqueTuplesSNB() {
        if (numUniquesSNB == -1) {
            if (this.numClassesOfSize1 == 0) {
                numUniquesSNB = 0;
            } else {
                numUniquesSNB = new ModelSNB(model, classes, sampleSize, accuracy, maxIterations, stop).getNumUniques();
            }
        }
        return isValid(numUniquesSNB) ? numUniquesSNB : 0d;
    }

    /**
     * Estimated number of unique tuples in the population according to Zayatz's statistical model
     */
    public double getNumUniqueTuplesZayatz() {
        if (numUniquesZayatz == -1) {
            if (this.numClassesOfSize1 == 0) {
                numUniquesZayatz = 0;
            } else {
                numUniquesZayatz = new ModelZayatz(model, classes, sampleSize, stop).getNumUniques();
            }
        }
        return isValid(numUniquesZayatz) ? numUniquesZayatz : 0d;
    }

    /**
     * Is an estimate valid?
     * @param value
     * @return
     */
    private boolean isValid(double value) {
        return !Double.isNaN(value) && value != 0d;
    }
}
