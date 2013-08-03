//********************************************************************/
//* Copyright (C) 2005-2011                                          */
//* MEL O'CAT  X178G243 (at) yahoo (dot) com                         */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

/*
 * MinHypothesisStep.java  0.01 11/01/2011
 *
 * Version 0.01:
 * Nov-01-2011: new.
 */

package mmj.gmff;

/**
 * Hypothesis step on a {@code MinProofWorksheet}.
 * <p>
 * Note: at this time GMFF does not need to know whether a proof step is an
 * hypothesis or a derivation step, but {@code MinHypothesisStep} and
 * {@code MinDerivationStep} are provided for completeness and future
 * possibilities :-)
 */
public class MinHypothesisStep extends MinProofStepStmt {

    /**
     * Standard {@code MinHypothesisStep} constructor.
     *
     * @param w the {@code MinProofworksheet} object which contains the proof
     *            step.
     * @param slc Array of lines each comprised of an Array of {@code String}s
     *            called "chunks", which may be either Metamath whitespace or
     *            Metamath tokens. Hence the acronym "slc" refers to Statement
     *            Line Chunks.
     */
    public MinHypothesisStep(final MinProofWorksheet w, final String[][] slc) {

        super(w, slc);
    }
}
