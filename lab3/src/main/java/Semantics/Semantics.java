package Semantics;

import java.util.*;
import Lexer.*;
import Grammar.*;

public class Semantics {
    private static List<String> errorMessage = new ArrayList<>();
    private static List<Variable> variableTable = new ArrayList<>();
    private static List<Variable> tempVariableTable = new ArrayList<>();
    private static List<Quad> quadTable = new ArrayList<>();

    public void action(Formula formula, List<Token> tokenList) {
        System.out.println(formula + " " + tokenList.toString());
    }

    public static int addVariable(Variable variable) {
        if (!variableTable.contains(variable)) {
            variableTable.add(variable);
            return variableTable.size() - 1;
        } else {
            return -1;
        }
    }

    public static Variable getVariable(String variable) {
        for (Variable v : variableTable) {
            if (v.getSymbol().equals(variable)) {
                return v;
            }
        }
        return null;
    }

    /**
     * @return the variableTable
     */
    public static List<Variable> getVariableTable() {
        return variableTable;
    }

    public static void addErrorMessage(String err) {
        errorMessage.add(err);
    }

    /**
     * @return the errorMessage
     */
    public static List<String> getErrorMessage() {
        return errorMessage;
    }

    public static void addQuad(Quad quad) {
        quadTable.add(quad);
    }

    public static void addTempVariable(Variable variable) {
        tempVariableTable.add(variable);
    }

    /**
     * @return the quadTable
     */
    public static List<Quad> getQuadTable() {
        return quadTable;
    }

    /**
     * @return the tempVariableTable
     */
    public static List<Variable> getTempVariableTable() {
        return tempVariableTable;
    }

    public static void generateCodes(AST ast) {
        if (ast.getRoot() != null && ast.getRoot().getCodes() != null) {
            List<Quad> quads = ast.getRoot().getCodes();
            quads.add(new Quad("end", null, null, null));
            Quad quadTemp;
            int i;
            for (i = 0; i < quads.size(); i++) {
                quadTemp = quads.get(i);
                if (quadTemp.getOperator().equals("if") || quadTemp.getOperator().equals("goto")) {
                    int num = Integer.parseInt(quadTemp.getResult().getOriginWord());
                    quadTemp.getResult().setOriginWord(String.valueOf(num + i));
                }
                quadTable.add(quadTemp);
            }
        }

    }

    public static void clearAll() {
        variableTable = new ArrayList<>();
        errorMessage = new ArrayList<>();
        tempVariableTable = new ArrayList<>();
        quadTable = new ArrayList<>();
    }
}