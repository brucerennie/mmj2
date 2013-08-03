//********************************************************************/
//* Copyright (C) 2006, 2007, 2008                                   */
//* MEL O'CAT  mmj2 (via) planetmath (dot) org                       */
//* License terms: GNU General Public License Version 2              */
//*                or any later version                              */
//********************************************************************/
//*4567890123456 (71-character line to adjust editor window) 23456789*/

// =================================================
// ===                   Class                   ===
// ===                                           ===
// ===      H y p o t h e s i s   S t e p        ===
// ===                                           ===
// =================================================

/*
 * HypothesisStep.java  0.07 08/01/2008
 * 
 * Version 0.04: 06/01/2007
 *     - Un-nested inner class
 *
 * Version 0.05: 08/01/2007
 *     - Work Var Enhancement misc. changes.
 *
 * Nov-01-2007 Version 0.06
 * - Modify constructor used for proof export to accept
 *   a proofLevel parameter.
 *
 * Aug-01-2008 Version 0.07
 * - Remove stmtHasError() method.
 */

package mmj.pa;

import java.io.IOException;
import java.util.Map;

import mmj.lang.*;
import mmj.mmio.MMIOError;
import mmj.mmio.Statementizer;

/**
 * HypothesisStep represents a Logical Hypothesis of the Theorem being proved.
 * <p>
 * On the GUI a HypothesisStep is shown with an 'h' prefix to step number.
 * <p>
 * A HypothesisStep cannot have Hyp's in its Step/Hyp/Ref field. Nor does it
 * have a proof, just a parse tree.
 */
public class HypothesisStep extends ProofStepStmt {

    /**
     * Default Constructor.
     * 
     * @param w the owning ProofWorksheet
     */
    public HypothesisStep(final ProofWorksheet w) {
        super(w);
    }

    /**
     * Constructor for incomplete DerivationStep destined only for output to the
     * GUI.
     * <p>
     * Creates "incomplete" HypothesisStep which is destined only for output to
     * the GUI, hence, the object references, etc. are not loaded. After display
     * to the GUI this worksheet disappears -- recreated via "load" each time
     * the user selects "StartUnification".
     * 
     * @param w the owning ProofWorksheet
     * @param step step number of the proof step
     * @param refLabel Ref label of the proof step
     * @param formula the proof step formula
     * @param parseTree formula ParseTree (can be null)
     * @param setCaret true means position caret of TextArea to this statement.
     * @param proofLevel level of step in proof.
     */
    public HypothesisStep(final ProofWorksheet w, final String step,
        final String refLabel, final Formula formula,
        final ParseTree parseTree, final boolean setCaret, final int proofLevel)
    {

        super(w, step, refLabel, setCaret);

        this.proofLevel = proofLevel;

        this.formula = formula;
        updateFormulaParseTree(parseTree);

        stmtText = buildStepHypRefSB();
        lineCnt = loadStmtText(stmtText, formula, parseTree);
    }

    public boolean isHypothesisStep() {
        return true;
    }
    public boolean isDerivationStep() {
        return false;
    }

    public boolean stmtIsIncomplete() {
        return false;
    }

    /**
     * Compares input Ref label to this step.
     * 
     * @param newRefLabel ref label to compare
     * @return true if equal, false if not equal.
     */
    public boolean hasMatchingRefLabel(final String newRefLabel) {
        if (refLabel.equals(newRefLabel))
            return true;
        else
            return false;
    }

    /**
     * Renumbers step numbers using a HashMap containing old and new step number
     * pairs.
     * 
     * @param renumberMap contains key/value pairs defining newly assigned step
     *            numbers.
     */
    public void renum(final Map<String, String> renumberMap) {

        final String newNum = renumberMap.get(step);
        if (newNum != null) {
            step = newNum;
            final StringBuilder sb = buildStepHypRefSB(); // hyp version
            reviseStepHypRefInStmtText(sb);
        }
    }

    /**
     * Reformats Hypothesis Step using TMFF.
     * <p>
     * Note: This isn't guaranteed to work unless the Proof Worksheet is free of
     * structural errors. Also, a formula which has not yet been parsed or which
     * failed to parse successfully will contain a null parse tree and be
     * formatted using Format 0 - "Unformatted".
     */
    public void tmffReformat() {
        stmtText = buildStepHypRefSB();
        lineCnt = loadStmtText(stmtText, formula, formulaParseTree);
    }

    /**
     * Loads HypothesisStep with step and ref.
     * <p>
     * This method is passed the contents of the first token which has already
     * been parsed into step, and ref fields and loads them into the
     * HypothesisStep -- which already contains the step's formula! This
     * back-asswardness is a result of trying to maintain cursor/caret control
     * when the formula is validated. Messy...
     * 
     * @param origStepHypRefLength length of first token of statement
     * @param lineStartCharNbr character number in the input stream of the
     *            statement start
     * @param stepField step number field
     * @param refField step ref field
     * @return first token of next statement.
     * @throws IOException if an error occurred
     * @throws MMIOError if an error occurred
     * @throws ProofAsstException if an error occurred
     */
    public String loadHypothesisStep(final int origStepHypRefLength,
        final int lineStartCharNbr, final String stepField,
        final String refField) throws IOException, MMIOError,
        ProofAsstException
    {

        // update ProofStepStmt fields
        step = stepField;
        refLabel = refField;

        final String nextT = loadStmtTextWithFormula(false); // false =
                                                             // !workVarsOk

        if (formula == null)
            w.triggerLoadStructureException(PaConstants.ERRMSG_FORMULA_REQ_1
                + w.getErrorLabelIfPossible()
                + PaConstants.ERRMSG_FORMULA_REQ_2 + step
                + PaConstants.ERRMSG_FORMULA_REQ_3,
                (int)w.proofTextTokenizer.getCurrentCharNbr() + 1
                    - lineStartCharNbr);

        if (w.isNewTheorem()) {
            getValidNewTheoremLogHypRef(lineStartCharNbr);
            getNewFormulaStepParseTree();
        }
        else {
            getValidOldTheoremLogHypRef(lineStartCharNbr);
            formulaParseTree = ref.getExprParseTree();
        }

        loadStepHypRefIntoStmtText(origStepHypRefLength, buildStepHypRefSB());

        return nextT;
    }

    /**
     * <code>
     * If Ref field input
     *     see if it matches one of theorem's log hyps
     *     if no match, throw exception
     *     if match, compare log hyp formula to
     *       proof step formula,
     *     if formulas not equal, throw exception
     * else (Ref field not input)
     *     look up Ref label using formula
     *     if not found, throw exception
     * end-if
     * save Ref stmt
     * </code>
     * 
     * @param lineStartCharNbr character number in the input stream of the
     *            statement start
     * @throws ProofAsstException if an error occurred
     */
    private void getValidOldTheoremLogHypRef(final int lineStartCharNbr)
        throws ProofAsstException
    {
        ref = null;
        final LogHyp[] logHypArray = w.theorem.getLogHypArray();
        if (refLabel == null) {
            for (final LogHyp element : logHypArray)
                if (formula.equals(element.getFormula())) {
                    if (dupLogHypFormulas(formula))
                        w.triggerLoadStructureException(
                            PaConstants.ERRMSG_DUP_LOG_HYPS_1
                                + w.getErrorLabelIfPossible()
                                + PaConstants.ERRMSG_DUP_LOG_HYPS_2 + step
                                + PaConstants.ERRMSG_DUP_LOG_HYPS_3,
                            (int)w.proofTextTokenizer.getCurrentCharNbr() + 1
                                - lineStartCharNbr);
                    ref = element;
                    refLabel = ref.getLabel();
                }
            if (ref == null)
                w.triggerLoadStructureException(
                    PaConstants.ERRMSG_HYP_FORMULA_ERR_1
                        + w.getErrorLabelIfPossible()
                        + PaConstants.ERRMSG_HYP_FORMULA_ERR_2 + step
                        + PaConstants.ERRMSG_HYP_FORMULA_ERR_3,
                    (int)w.proofTextTokenizer.getCurrentCharNbr() + 1
                        - lineStartCharNbr);
        }
        else {
            // refLabel was input
            ref = w.logicalSystem.getStmtTbl().get(refLabel);
            if (ref == null)
                w.triggerLoadStructureException(
                    PaConstants.ERRMSG_REF_NOTFND2_1
                        + w.getErrorLabelIfPossible()
                        + PaConstants.ERRMSG_REF_NOTFND2_2 + step
                        + PaConstants.ERRMSG_REF_NOTFND2_3 + refLabel
                        + PaConstants.ERRMSG_REF_NOTFND2_4,
                    (int)w.proofTextTokenizer.getCurrentCharNbr() + 1
                        - lineStartCharNbr);
            if (!ref.isLogHyp())
                w.triggerLoadStructureException(
                    PaConstants.ERRMSG_REF_NOT_LOGHYP_1
                        + w.getErrorLabelIfPossible()
                        + PaConstants.ERRMSG_REF_NOT_LOGHYP_2 + step
                        + PaConstants.ERRMSG_REF_NOT_LOGHYP_3 + refLabel
                        + PaConstants.ERRMSG_REF_NOT_LOGHYP_4,
                    (int)w.proofTextTokenizer.getCurrentCharNbr() + 1
                        - lineStartCharNbr);
            int i;
            for (i = 0; i < logHypArray.length; i++)
                if (ref == logHypArray[i])
                    break;
            if (i >= logHypArray.length)
                w.triggerLoadStructureException(
                    PaConstants.ERRMSG_LOGHYP_MISMATCH_1
                        + w.getErrorLabelIfPossible()
                        + PaConstants.ERRMSG_LOGHYP_MISMATCH_2 + step
                        + PaConstants.ERRMSG_LOGHYP_MISMATCH_3 + refLabel
                        + PaConstants.ERRMSG_LOGHYP_MISMATCH_4,
                    (int)w.proofTextTokenizer.getCurrentCharNbr() + 1
                        - lineStartCharNbr);
            if (!formula.equals(ref.getFormula()))
                w.triggerLoadStructureException(
                    PaConstants.ERRMSG_HYP_FORMULA_ERR2_1
                        + w.getErrorLabelIfPossible()
                        + PaConstants.ERRMSG_HYP_FORMULA_ERR2_2 + step
                        + PaConstants.ERRMSG_HYP_FORMULA_ERR2_3 + refLabel
                        + PaConstants.ERRMSG_HYP_FORMULA_ERR2_4,
                    (int)w.proofTextTokenizer.getCurrentCharNbr() + 1
                        - lineStartCharNbr);
        }
        checkDupHypRefLabel(lineStartCharNbr);
    }

    private boolean dupLogHypFormulas(final Formula f) {
        for (final ProofWorkStmt x : w.proofWorkStmtList)
            if (x.isHypothesisStep())
                if (((HypothesisStep)x).formula.equals(f))
                    return true;
        return false;
    }

    /**
     * <code>
     * If Ref field not input,
     *     generates ref field as theoremLabel
     *        + "." + sequence number of hyp
     * End-if
     * 
     * See if it is a dup of another proof step ref
     * If dup, throw exception
     * 
     * See if it already exists in database
     * If exists, throw exception.
     * 
     * See if it is a valid label according to
     * the Metamath.pdf spec -- not on the prohibited
     * list and no offending characters...
     * </code>
     * 
     * @param lineStartCharNbr character number in the input stream of the
     *            statement start
     * @throws ProofAsstException if an error occurred
     */
    private void getValidNewTheoremLogHypRef(final int lineStartCharNbr)
        throws ProofAsstException
    {
        ref = null;
        if (refLabel == null)
            refLabel = new String(w.getTheoremLabel() + "."
                + Integer.toString(w.hypStepCnt + 1));

        checkDupHypRefLabel(lineStartCharNbr);

        final Stmt stmt = w.logicalSystem.getStmtTbl().get(refLabel);
        if (stmt != null)
            w.triggerLoadStructureException(PaConstants.ERRMSG_HYP_REF_DUP_1
                + w.getErrorLabelIfPossible()
                + PaConstants.ERRMSG_HYP_REF_DUP_2 + step
                + PaConstants.ERRMSG_HYP_REF_DUP_3 + refLabel
                + PaConstants.ERRMSG_HYP_REF_DUP_4,
                (int)w.proofTextTokenizer.getCurrentCharNbr() + 1
                    - lineStartCharNbr);
        if (!Statementizer.areLabelCharsValid(refLabel))
            w.triggerLoadStructureException(
                PaConstants.ERRMSG_REF_CHAR_PROHIB_1
                    + w.getErrorLabelIfPossible()
                    + PaConstants.ERRMSG_REF_CHAR_PROHIB_2 + step
                    + PaConstants.ERRMSG_REF_CHAR_PROHIB_3 + refLabel
                    + PaConstants.ERRMSG_REF_CHAR_PROHIB_4,
                (int)w.proofTextTokenizer.getCurrentCharNbr() + 1
                    - lineStartCharNbr);
        if (Statementizer.isLabelOnProhibitedList(refLabel))
            w.triggerLoadStructureException(PaConstants.ERRMSG_PROHIB_LABEL2_1
                + w.getErrorLabelIfPossible()
                + PaConstants.ERRMSG_PROHIB_LABEL2_2 + step
                + PaConstants.ERRMSG_PROHIB_LABEL2_3 + refLabel
                + PaConstants.ERRMSG_PROHIB_LABEL2_4,
                (int)w.proofTextTokenizer.getCurrentCharNbr() + 1
                    - lineStartCharNbr);

        if (w.logicalSystem.getSymTbl().containsKey(refLabel))
            w.triggerLoadStructureException(
                PaConstants.ERRMSG_STMT_LABEL_DUP_OF_SYM_ID_2_1
                    + w.getErrorLabelIfPossible()
                    + PaConstants.ERRMSG_STMT_LABEL_DUP_OF_SYM_ID_2_2 + step
                    + PaConstants.ERRMSG_STMT_LABEL_DUP_OF_SYM_ID_2_3
                    + refLabel
                    + PaConstants.ERRMSG_STMT_LABEL_DUP_OF_SYM_ID_2_4,
                (int)w.proofTextTokenizer.getCurrentCharNbr() + 1
                    - lineStartCharNbr);

    }

    private void checkDupHypRefLabel(final int lineStartCharNbr)
        throws ProofAsstException
    {

        for (final ProofWorkStmt x : w.proofWorkStmtList)
            if (x.hasMatchingRefLabel(refLabel))
                w.triggerLoadStructureException(
                    PaConstants.ERRMSG_DUP_HYP_REF_1
                        + w.getErrorLabelIfPossible()
                        + PaConstants.ERRMSG_DUP_HYP_REF_2 + step
                        + PaConstants.ERRMSG_DUP_HYP_REF_3 + refLabel
                        + PaConstants.ERRMSG_DUP_HYP_REF_4,
                    (int)w.proofTextTokenizer.getCurrentCharNbr() + 1
                        - lineStartCharNbr);
    }

    private StringBuilder buildStepHypRefSB() {

        final StringBuilder sb = new StringBuilder();

        sb.append(PaConstants.HYP_STEP_PREFIX);
        sb.append(step);
        sb.append(PaConstants.FIELD_DELIMITER_COLON);
        sb.append(PaConstants.FIELD_DELIMITER_COLON);
        if (refLabel != null)
            sb.append(refLabel);
        return sb;
    }
}
