package Grammar;

import java.util.*;

import Lexer.*;
import Semantics.*;

public class AST {
    private Node root = null;
    List<String> processMessage = new ArrayList<>();
    private static List<String> numType = new ArrayList<>();
    private static List<String> symbolStackState = new ArrayList<>();
    private static List<String> stateStackState = new ArrayList<>();
    private String variableTable = "variable";
    private String tempTable = "temp";
    private String tempSymbol = "$";
    static {
        numType.add("boolean");
        numType.add("integer");
        numType.add("float");
    }

    public AST(Analyse analyse) throws Exception {
        generateAST(analyse);
        Semantics.generateCodes(this);
    }

    /**
     * @return the root
     */
    public Node getRoot() {
        return root;
    }

    /**
     * @param root the root to set
     */
    public void setRoot(Node root) {
        this.root = root;
    }

    public Formula getFormula(Node parent, List<Node> children) {
        String formula = "";
        formula = parent.getToken().getTokenValue();
        formula += LR1.separatorString;
        for (Node node : children) {
            formula += node.getToken().getTokenValue();
            formula += " ";
        }
        return new Formula(formula.substring(0, formula.length() - 1));
    }

    /**
     * @return the processMessage
     */
    public List<String> getProcessMessage() {
        return processMessage;
    }

    public void generateAST(Analyse analyse) throws Exception {
        int top = 0;
        Stack<Node> symbolStack = new Stack<>();
        Stack<Integer> stateStack = new Stack<>();
        symbolStack.push(new Node(new Token("punctuation", LR1.endString, 0, LR1.endString, -1)));
        stateStack.push(0);
        while (true) {
            Node symbol = new Node(analyse.getTokenList().get(top));
            if (symbol.getToken().getType().equals("comment")) {
                top += 1;
                continue;
            }
            if (LR1.actionTable.get(stateStack.peek()).containsKey(symbol.getToken().getTokenValue())) {
                String action = LR1.actionTable.get(stateStack.peek()).get(symbol.getToken().getTokenValue());
                String actionType = String.valueOf(action.charAt(0));
                final String SHIFT = "s";
                final String REDUCE = "r";
                Integer actionState = null;
                if (!action.equals("acc")) {
                    actionState = Integer.parseInt(action.substring(1, action.length()));
                }
                if (actionType.equals(SHIFT)) {
                    symbolStack.push(symbol);
                    stateStack.push(actionState);
                    processMessage.add("Action " + action);
                    symbolStackState.add(symbolStack.toString());
                    stateStackState.add(stateStack.toString());
                    top += 1;
                } else if (actionType.equals(REDUCE)) {
                    Formula formula = LR1.grammars.get(actionState);
                    List<String> formulaRight = formula.getSymbols();
                    Stack<Node> stackFormulaRight = new Stack<>();
                    Node formulaLeft = new Node(
                            new Token("Non-Terminator", formula.getPrefix(), 0, formula.getPrefix(), -1));
                    List<Node> nodeList = new ArrayList<>();
                    if (!(formulaRight.size() == 1 && formulaRight.get(0).equals(LR1.nullString))) {
                        for (int i = 0; i < formulaRight.size(); i++) {
                            stackFormulaRight.push(symbolStack.pop());
                            stateStack.pop();
                        }
                        for (String str : formulaRight) {
                            Node temp = stackFormulaRight.pop();
                            nodeList.add(temp);
                            if (!str.equals(temp.getToken().getTokenValue())) {
                                throw new Exception("Error occurs when reducing");
                            }
                        }
                    }
                    for (Node node : nodeList) {
                        node.setParent(formulaLeft);
                        formulaLeft.addChild(node);
                    }

                    // semantics analyse
                    int formulaIndex = LR1.grammars.indexOf(formula);
                    int index;
                    Variable variable;
                    String type1, type2, operator;
                    Node root = formulaLeft;
                    Quad quad;
                    Token tempToken;
                    switch (formulaIndex) {
                    case 0:
                        break;
                    case 1: // primary_expression:CONSTANT
                        root.getChildren().get(0).getAttributes().put("type",
                                root.getChildren().get(0).getToken().getType());
                        root.getAttributes().put("type", root.getChildren().get(0).getToken().getType());
                        root.getAttributes().put("table", null);
                        root.getAttributes().put("symbol", root.getChildren().get(0).getToken());
                        break;
                    case 2: // primary_expression:IDENTIFIER
                        variable = Semantics.getVariable(root.getChildren().get(0).getToken().getOriginWord());
                        if (variable == null) {
                            Semantics.addErrorMessage("Error at Line: "
                                    + root.getChildren().get(0).getToken().getLineNumber() + " [The variable "
                                    + root.getChildren().get(0).getToken().getOriginWord() + " has not been declared]");
                        } else {
                            root.getAttributes().put("type", variable.getType());
                            root.getAttributes().put("table", this.variableTable);
                            root.getAttributes().put("symbol", root.getChildren().get(0).getToken());
                        }
                        break;
                    case 3: // primary_expression:L_PAREN expression R_PAREN
                        root.getAttributes().putAll(root.getChildren().get(1).getAttributes());
                        break;
                    case 4: // postfix_expression:primary_expression
                        root.getAttributes().putAll(root.getChildren().get(0).getAttributes());
                        break;
                    case 5: // postfix_expression:postfix_expression L_BRACK expression R_BRACK
                        root.getAttributes().putAll(root.getChildren().get(0).getAttributes());
                        break;
                    case 6: // postfix_expression:postfix_expression L_PAREN R_PAREN
                        root.getAttributes().putAll(root.getChildren().get(0).getAttributes());
                        break;
                    case 7:
                        break;
                    case 8: // unary_expression:postfix_expression
                        root.getAttributes().putAll(root.getChildren().get(0).getAttributes());
                        break;
                    case 9: // unary_expression:NOT postfix_expression
                        if (!root.getChildren().get(1).getType().equals("boolean")) {
                            Semantics.addErrorMessage(
                                    "Error at Line: " + root.getChildren().get(0).getToken().getLineNumber()
                                            + " [Not operator can't be used to other type]");
                        } else {
                            root.getAttributes().put("type", "boolean");
                            index = Semantics.getTempVariableTable().size();
                            // generate temp variable
                            variable = new Variable(this.tempSymbol + index, "boolean", null, 4, 4 * index);
                            Semantics.addTempVariable(variable);
                            root.getAttributes().put("table", this.tempTable);
                            // generate token
                            tempToken = new Token("temp", this.tempSymbol + index, 0, this.tempSymbol + index, -1);
                            root.getAttributes().put("symbol", tempToken);
                            // generate code
                            quad = new Quad("!", root.getChildren().get(1).getSymbol(), null, tempToken);
                            root.getAttributes().put("codes", new ArrayList<Quad>());
                            root.getCodes().add(quad);
                        }
                        break;
                    case 10: // unary_expression:SUB postfix_expression
                        if (!root.getChildren().get(1).getType().equals("integer")
                                && !root.getChildren().get(1).getType().equals("float")) {
                            Semantics.addErrorMessage(
                                    "Error at Line: " + root.getChildren().get(0).getToken().getLineNumber()
                                            + " [Sub operator can't be used to other type]");
                        } else {
                            root.getAttributes().put("type", root.getChildren().get(1).getType());
                            index = Semantics.getTempVariableTable().size();
                            // generate temp variable
                            variable = new Variable(this.tempSymbol + index, root.getChildren().get(1).getType(), null,
                                    4, 4 * index);
                            Semantics.addTempVariable(variable);
                            root.getAttributes().put("table", this.tempTable);
                            // generate token
                            tempToken = new Token("temp", this.tempSymbol + index, 0, this.tempSymbol + index, -1);
                            root.getAttributes().put("symbol", tempToken);
                            // generate code
                            quad = new Quad("-", root.getChildren().get(1).getSymbol(), null, tempToken);
                            root.getAttributes().put("codes", new ArrayList<Quad>());
                            root.getCodes().add(quad);
                        }
                        break;
                    case 11: // multiplicative_expression:unary_expression
                        root.getAttributes().putAll(root.getChildren().get(0).getAttributes());
                        break;
                    case 12: // multiplicative_expression:multiplicative_expression MUL postfix_expression
                        type1 = root.getChildren().get(0).getType();
                        type2 = root.getChildren().get(2).getType();
                        if ((!type1.equals("integer") && !type1.equals("float"))
                                || (!type2.equals("integer") && !type2.equals("float"))) {
                            Semantics.addErrorMessage(
                                    "Error at Line: " + root.getChildren().get(1).getToken().getLineNumber()
                                            + " [Multiply operator can't be used to other type]");
                        } else {
                            root.getAttributes().put("type", type1.equals(type2) ? type1 : "float");
                            index = Semantics.getTempVariableTable().size();
                            // generate temp variable
                            variable = new Variable(this.tempSymbol + index, type1.equals(type2) ? type1 : "float",
                                    null, 4, 4 * index);
                            Semantics.addTempVariable(variable);
                            root.getAttributes().put("table", this.tempTable);
                            // generate token
                            tempToken = new Token("temp", this.tempSymbol + index, 0, this.tempSymbol + index, -1);
                            root.getAttributes().put("symbol", tempToken);
                            // generate code
                            quad = new Quad("*", root.getChildren().get(0).getSymbol(),
                                    root.getChildren().get(2).getSymbol(), tempToken);
                            root.getAttributes().put("codes", new ArrayList<Quad>());
                            if (root.getChildren().get(0).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(0).getCodes());
                            if (root.getChildren().get(2).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(2).getCodes());
                            root.getCodes().add(quad);
                        }
                        break;
                    case 13: // multiplicative_expression:multiplicative_expression DIV postfix_expression
                        type1 = root.getChildren().get(0).getType();
                        type2 = root.getChildren().get(2).getType();
                        if ((!type1.equals("integer") && !type1.equals("float"))
                                || (!type2.equals("integer") && !type2.equals("float"))) {
                            Semantics.addErrorMessage(
                                    "Error at Line: " + root.getChildren().get(1).getToken().getLineNumber()
                                            + " [Division operator can't be used to other type]");
                        } else {
                            root.getAttributes().put("type", type1.equals(type2) ? type1 : "float");
                            index = Semantics.getTempVariableTable().size();
                            // generate temp variable
                            variable = new Variable(this.tempSymbol + index, type1.equals(type2) ? type1 : "float",
                                    null, 4, 4 * index);
                            Semantics.addTempVariable(variable);
                            root.getAttributes().put("table", this.tempTable);
                            // generate token
                            tempToken = new Token("temp", this.tempSymbol + index, 0, this.tempSymbol + index, -1);
                            root.getAttributes().put("symbol", tempToken);
                            // generate code
                            quad = new Quad("/", root.getChildren().get(0).getSymbol(),
                                    root.getChildren().get(2).getSymbol(), tempToken);
                            root.getAttributes().put("codes", new ArrayList<Quad>());
                            if (root.getChildren().get(0).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(0).getCodes());
                            if (root.getChildren().get(2).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(2).getCodes());
                            root.getCodes().add(quad);
                        }
                        break;
                    case 14: // multiplicative_expression:multiplicative_expression MOD postfix_expression
                        type1 = root.getChildren().get(0).getType();
                        type2 = root.getChildren().get(2).getType();
                        if (!type1.equals("integer") && !type2.equals("integer")) {
                            Semantics.addErrorMessage(
                                    "Error at Line: " + root.getChildren().get(1).getToken().getLineNumber()
                                            + " [Modulo operator can't be used to other type]");
                        } else {
                            root.getAttributes().put("type", "integer");
                            index = Semantics.getTempVariableTable().size();
                            // generate temp variable
                            variable = new Variable(this.tempSymbol + index, "integer", null, 4, 4 * index);
                            Semantics.addTempVariable(variable);
                            root.getAttributes().put("table", this.tempTable);
                            // generate token
                            tempToken = new Token("temp", this.tempSymbol + index, 0, this.tempSymbol + index, -1);
                            root.getAttributes().put("symbol", tempToken);
                            // generate code
                            quad = new Quad("%", root.getChildren().get(0).getSymbol(),
                                    root.getChildren().get(2).getSymbol(), tempToken);
                            root.getAttributes().put("codes", new ArrayList<Quad>());
                            if (root.getChildren().get(0).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(0).getCodes());
                            if (root.getChildren().get(2).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(2).getCodes());
                            root.getCodes().add(quad);
                        }
                        break;
                    case 15: // additive_expression:multiplicative_expression
                        root.getAttributes().putAll(root.getChildren().get(0).getAttributes());
                        break;
                    case 16: // additive_expression:additive_expression ADD multiplicative_expression
                        type1 = root.getChildren().get(0).getType();
                        type2 = root.getChildren().get(2).getType();
                        if ((!type1.equals("integer") && !type1.equals("float"))
                                || (!type2.equals("integer") && !type2.equals("float"))) {
                            Semantics.addErrorMessage(
                                    "Error at Line: " + root.getChildren().get(1).getToken().getLineNumber()
                                            + " [Add operator can't be used to other type]");
                        } else {
                            root.getAttributes().put("type", type1.equals(type2) ? type1 : "float");
                            index = Semantics.getTempVariableTable().size();
                            // generate temp variable
                            variable = new Variable(this.tempSymbol + index, type1.equals(type2) ? type1 : "float",
                                    null, 4, 4 * index);
                            Semantics.addTempVariable(variable);
                            root.getAttributes().put("table", this.tempTable);
                            // generate token
                            tempToken = new Token("temp", this.tempSymbol + index, 0, this.tempSymbol + index, -1);
                            root.getAttributes().put("symbol", tempToken);
                            // generate code
                            quad = new Quad("+", root.getChildren().get(0).getSymbol(),
                                    root.getChildren().get(2).getSymbol(), tempToken);
                            root.getAttributes().put("codes", new ArrayList<Quad>());
                            if (root.getChildren().get(0).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(0).getCodes());
                            if (root.getChildren().get(2).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(2).getCodes());
                            root.getCodes().add(quad);
                        }
                        break;
                    case 17: // additive_expression:additive_expression SUB multiplicative_expression
                        type1 = root.getChildren().get(0).getType();
                        type2 = root.getChildren().get(2).getType();
                        if ((!type1.equals("integer") && !type1.equals("float"))
                                || (!type2.equals("integer") && !type2.equals("float"))) {
                            Semantics.addErrorMessage(
                                    "Error at Line: " + root.getChildren().get(1).getToken().getLineNumber()
                                            + " [Sub operator can't be used to other type]");
                        } else {
                            root.getAttributes().put("type", type1.equals(type2) ? type1 : "float");
                            root.getAttributes().put("type", type1.equals(type2) ? type1 : "float");
                            index = Semantics.getTempVariableTable().size();
                            // generate temp variable
                            variable = new Variable(this.tempSymbol + index, type1.equals(type2) ? type1 : "float",
                                    null, 4, 4 * index);
                            Semantics.addTempVariable(variable);
                            root.getAttributes().put("table", this.tempTable);
                            // generate token
                            tempToken = new Token("temp", this.tempSymbol + index, 0, this.tempSymbol + index, -1);
                            root.getAttributes().put("symbol", tempToken);
                            // generate code
                            quad = new Quad("-", root.getChildren().get(0).getSymbol(),
                                    root.getChildren().get(2).getSymbol(), tempToken);
                            root.getAttributes().put("codes", new ArrayList<Quad>());
                            if (root.getChildren().get(0).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(0).getCodes());
                            if (root.getChildren().get(2).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(2).getCodes());
                            root.getCodes().add(quad);
                        }
                        break;
                    case 18: // relational_expression:additive_expression
                        root.getAttributes().putAll(root.getChildren().get(0).getAttributes());
                        break;
                    case 19: // relational_expression:relational_expression LT additive_expression
                        operator = "<";
                        type1 = root.getChildren().get(0).getType();
                        type2 = root.getChildren().get(2).getType();
                        if ((!type1.equals("integer") && !type1.equals("float"))
                                || (!type2.equals("integer") && !type2.equals("float"))) {
                            Semantics.addErrorMessage(
                                    "Error at Line: " + root.getChildren().get(1).getToken().getLineNumber() + " ["
                                            + operator + " operator can't be used to other type]");
                        } else {
                            root.getAttributes().put("type", "boolean");
                            index = Semantics.getTempVariableTable().size();
                            // generate temp variable
                            variable = new Variable(this.tempSymbol + index, "boolean", null, 4, 4 * index);
                            Semantics.addTempVariable(variable);
                            root.getAttributes().put("table", this.tempTable);
                            // generate token
                            tempToken = new Token("temp", this.tempSymbol + index, 0, this.tempSymbol + index, -1);
                            root.getAttributes().put("symbol", tempToken);
                            // generate code
                            quad = new Quad(operator, root.getChildren().get(0).getSymbol(),
                                    root.getChildren().get(2).getSymbol(), tempToken);
                            root.getAttributes().put("codes", new ArrayList<Quad>());
                            if (root.getChildren().get(0).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(0).getCodes());
                            if (root.getChildren().get(2).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(2).getCodes());
                            root.getCodes().add(quad);
                        }
                        break;
                    case 20: // relational_expression:relational_expression GT additive_expression
                        operator = ">";
                        type1 = root.getChildren().get(0).getType();
                        type2 = root.getChildren().get(2).getType();
                        if ((!type1.equals("integer") && !type1.equals("float"))
                                || (!type2.equals("integer") && !type2.equals("float"))) {
                            Semantics.addErrorMessage(
                                    "Error at Line: " + root.getChildren().get(1).getToken().getLineNumber() + " ["
                                            + operator + " operator can't be used to other type]");
                        } else {
                            root.getAttributes().put("type", "boolean");
                            index = Semantics.getTempVariableTable().size();
                            // generate temp variable
                            variable = new Variable(this.tempSymbol + index, "boolean", null, 4, 4 * index);
                            Semantics.addTempVariable(variable);
                            root.getAttributes().put("table", this.tempTable);
                            // generate token
                            tempToken = new Token("temp", this.tempSymbol + index, 0, this.tempSymbol + index, -1);
                            root.getAttributes().put("symbol", tempToken);
                            // generate code
                            quad = new Quad(operator, root.getChildren().get(0).getSymbol(),
                                    root.getChildren().get(2).getSymbol(), tempToken);
                            root.getAttributes().put("codes", new ArrayList<Quad>());
                            if (root.getChildren().get(0).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(0).getCodes());
                            if (root.getChildren().get(2).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(2).getCodes());
                            root.getCodes().add(quad);
                        }
                        break;
                    case 21: // relational_expression:relational_expression LE additive_expression
                        operator = "<=";
                        type1 = root.getChildren().get(0).getType();
                        type2 = root.getChildren().get(2).getType();
                        if ((!type1.equals("integer") && !type1.equals("float"))
                                || (!type2.equals("integer") && !type2.equals("float"))) {
                            Semantics.addErrorMessage(
                                    "Error at Line: " + root.getChildren().get(1).getToken().getLineNumber() + " ["
                                            + operator + " operator can't be used to other type]");
                        } else {
                            root.getAttributes().put("type", "boolean");
                            index = Semantics.getTempVariableTable().size();
                            // generate temp variable
                            variable = new Variable(this.tempSymbol + index, "boolean", null, 4, 4 * index);
                            Semantics.addTempVariable(variable);
                            root.getAttributes().put("table", this.tempTable);
                            // generate token
                            tempToken = new Token("temp", this.tempSymbol + index, 0, this.tempSymbol + index, -1);
                            root.getAttributes().put("symbol", tempToken);
                            // generate code
                            quad = new Quad(operator, root.getChildren().get(0).getSymbol(),
                                    root.getChildren().get(2).getSymbol(), tempToken);
                            root.getAttributes().put("codes", new ArrayList<Quad>());
                            if (root.getChildren().get(0).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(0).getCodes());
                            if (root.getChildren().get(2).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(2).getCodes());
                            root.getCodes().add(quad);
                        }
                        break;
                    case 22: // relational_expression:relational_expression GE additive_expression
                        operator = ">=";
                        type1 = root.getChildren().get(0).getType();
                        type2 = root.getChildren().get(2).getType();
                        if ((!type1.equals("integer") && !type1.equals("float"))
                                || (!type2.equals("integer") && !type2.equals("float"))) {
                            Semantics.addErrorMessage(
                                    "Error at Line: " + root.getChildren().get(1).getToken().getLineNumber() + " ["
                                            + operator + " operator can't be used to other type]");
                        } else {
                            root.getAttributes().put("type", "boolean");
                            index = Semantics.getTempVariableTable().size();
                            // generate temp variable
                            variable = new Variable(this.tempSymbol + index, "boolean", null, 4, 4 * index);
                            Semantics.addTempVariable(variable);
                            root.getAttributes().put("table", this.tempTable);
                            // generate token
                            tempToken = new Token("temp", this.tempSymbol + index, 0, this.tempSymbol + index, -1);
                            root.getAttributes().put("symbol", tempToken);
                            // generate code
                            quad = new Quad(operator, root.getChildren().get(0).getSymbol(),
                                    root.getChildren().get(2).getSymbol(), tempToken);
                            root.getAttributes().put("codes", new ArrayList<Quad>());
                            if (root.getChildren().get(0).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(0).getCodes());
                            if (root.getChildren().get(2).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(2).getCodes());
                            root.getCodes().add(quad);
                        }
                        break;
                    case 23: // equality_expression:relational_expression
                        root.getAttributes().putAll(root.getChildren().get(0).getAttributes());
                        break;
                    case 24: // equality_expression:equality_expression EQ relational_expression
                        operator = "==";
                        type1 = root.getChildren().get(0).getType();
                        type2 = root.getChildren().get(2).getType();
                        if ((!type1.equals("integer") && !type1.equals("float"))
                                || (!type2.equals("integer") && !type2.equals("float"))) {
                            Semantics.addErrorMessage(
                                    "Error at Line: " + root.getChildren().get(1).getToken().getLineNumber() + " ["
                                            + operator + " operator can't be used to other type]");
                        } else {
                            root.getAttributes().put("type", "boolean");
                            index = Semantics.getTempVariableTable().size();
                            // generate temp variable
                            variable = new Variable(this.tempSymbol + index, "boolean", null, 4, 4 * index);
                            Semantics.addTempVariable(variable);
                            root.getAttributes().put("table", this.tempTable);
                            // generate token
                            tempToken = new Token("temp", this.tempSymbol + index, 0, this.tempSymbol + index, -1);
                            root.getAttributes().put("symbol", tempToken);
                            // generate code
                            quad = new Quad(operator, root.getChildren().get(0).getSymbol(),
                                    root.getChildren().get(2).getSymbol(), tempToken);
                            root.getAttributes().put("codes", new ArrayList<Quad>());
                            if (root.getChildren().get(0).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(0).getCodes());
                            if (root.getChildren().get(2).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(2).getCodes());
                            root.getCodes().add(quad);
                        }
                        break;
                    case 25: // equality_expression:equality_expression NEQ relational_expression
                        operator = "!=";
                        type1 = root.getChildren().get(0).getType();
                        type2 = root.getChildren().get(2).getType();
                        if ((!type1.equals("integer") && !type1.equals("float"))
                                || (!type2.equals("integer") && !type2.equals("float"))) {
                            Semantics.addErrorMessage(
                                    "Error at Line: " + root.getChildren().get(1).getToken().getLineNumber() + " ["
                                            + operator + " operator can't be used to other type]");
                        } else {
                            root.getAttributes().put("type", "boolean");
                            index = Semantics.getTempVariableTable().size();
                            // generate temp variable
                            variable = new Variable(this.tempSymbol + index, "boolean", null, 4, 4 * index);
                            Semantics.addTempVariable(variable);
                            root.getAttributes().put("table", this.tempTable);
                            // generate token
                            tempToken = new Token("temp", this.tempSymbol + index, 0, this.tempSymbol + index, -1);
                            root.getAttributes().put("symbol", tempToken);
                            // generate code
                            quad = new Quad(operator, root.getChildren().get(0).getSymbol(),
                                    root.getChildren().get(2).getSymbol(), tempToken);
                            root.getAttributes().put("codes", new ArrayList<Quad>());
                            if (root.getChildren().get(0).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(0).getCodes());
                            if (root.getChildren().get(2).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(2).getCodes());
                            root.getCodes().add(quad);
                        }
                        break;
                    case 26: // logical_and_expression:equality_expression
                        root.getAttributes().putAll(root.getChildren().get(0).getAttributes());
                        break;
                    case 27: // logical_and_expression:logical_and_expression AND equality_expression
                        operator = "&";
                        type1 = root.getChildren().get(0).getType();
                        type2 = root.getChildren().get(2).getType();
                        if (!type1.equals("boolean") || !type2.equals("boolean")) {
                            Semantics.addErrorMessage(
                                    "Error at Line: " + root.getChildren().get(1).getToken().getLineNumber() + " ["
                                            + operator + " operator can't be used to other type]");
                        } else {
                            root.getAttributes().put("type", "boolean");
                            index = Semantics.getTempVariableTable().size();
                            // generate temp variable
                            variable = new Variable(this.tempSymbol + index, "boolean", null, 4, 4 * index);
                            Semantics.addTempVariable(variable);
                            root.getAttributes().put("table", this.tempTable);
                            // generate token
                            tempToken = new Token("temp", this.tempSymbol + index, 0, this.tempSymbol + index, -1);
                            root.getAttributes().put("symbol", tempToken);
                            // generate code
                            quad = new Quad(operator, root.getChildren().get(0).getSymbol(),
                                    root.getChildren().get(2).getSymbol(), tempToken);
                            root.getAttributes().put("codes", new ArrayList<Quad>());
                            if (root.getChildren().get(0).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(0).getCodes());
                            if (root.getChildren().get(2).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(2).getCodes());
                            root.getCodes().add(quad);
                        }
                        break;
                    case 28: // logical_or_expression:logical_and_expression
                        root.getAttributes().putAll(root.getChildren().get(0).getAttributes());
                        break;
                    case 29: // logical_or_expression:logical_or_expression OR logical_and_expression
                        operator = "|";
                        type1 = root.getChildren().get(0).getType();
                        type2 = root.getChildren().get(2).getType();
                        if (!type1.equals("boolean") || !type2.equals("boolean")) {
                            Semantics.addErrorMessage(
                                    "Error at Line: " + root.getChildren().get(1).getToken().getLineNumber() + " ["
                                            + operator + " operator can't be used to other type]");
                        } else {
                            root.getAttributes().put("type", "boolean");
                            index = Semantics.getTempVariableTable().size();
                            // generate temp variable
                            variable = new Variable(this.tempSymbol + index, "boolean", null, 4, 4 * index);
                            Semantics.addTempVariable(variable);
                            root.getAttributes().put("table", this.tempTable);
                            // generate token
                            tempToken = new Token("temp", this.tempSymbol + index, 0, this.tempSymbol + index, -1);
                            root.getAttributes().put("symbol", tempToken);
                            // generate code
                            quad = new Quad(operator, root.getChildren().get(0).getSymbol(),
                                    root.getChildren().get(2).getSymbol(), tempToken);
                            root.getAttributes().put("codes", new ArrayList<Quad>());
                            if (root.getChildren().get(0).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(0).getCodes());
                            if (root.getChildren().get(2).getCodes() != null)
                                root.getCodes().addAll(root.getChildren().get(2).getCodes());
                            root.getCodes().add(quad);
                        }
                        break;
                    case 30: // assignment_expression:logical_or_expression
                        root.getAttributes().putAll(root.getChildren().get(0).getAttributes());
                        break;
                    case 31: // assignment_expression:postfix_expression assignment_operator
                             // assignment_expression
                        operator = "=";
                        type2 = root.getChildren().get(2).getType();
                        if (!numType.contains(type2)) {
                            Semantics.addErrorMessage(
                                    "Error at Line: " + root.getChildren().get(0).getSymbol().getLineNumber() + " ["
                                            + operator + " operator can't be used to other type]");
                        } else {
                            if (root.getChildren().get(0).getSymbol() != null) {
                                if (root.getChildren().get(0).getTable() == null) {
                                    Semantics.addErrorMessage(
                                            "Error at Line: " + root.getChildren().get(0).getSymbol().getLineNumber()
                                                    + " [Can't assign value to constant]");
                                } else {
                                    variable = Semantics
                                            .getVariable(root.getChildren().get(0).getSymbol().getOriginWord());
                                    if (variable != null) {
                                        variable.setType(type2);
                                    }
                                    root.getChildren().get(0).getAttributes().put("type",
                                            root.getChildren().get(2).getType());
                                    // generate code
                                    quad = new Quad(operator, root.getChildren().get(2).getSymbol(), null,
                                            root.getChildren().get(0).getSymbol());
                                    root.getAttributes().put("codes", new ArrayList<Quad>());
                                    if (root.getChildren().get(0).getCodes() != null)
                                        root.getCodes().addAll(root.getChildren().get(0).getCodes());
                                    if (root.getChildren().get(2).getCodes() != null)
                                        root.getCodes().addAll(root.getChildren().get(2).getCodes());
                                    root.getCodes().add(quad);
                                }
                            }
                        }
                        break;
                    case 32: // argument_expression_list:assignment_expression
                        root.getAttributes().putAll(root.getChildren().get(0).getAttributes());
                        break;
                    case 33:
                        break;
                    case 34: // argument_expression_list:assignment_expression
                        root.getAttributes().putAll(root.getChildren().get(0).getAttributes());
                        break;
                    case 35:
                        break;
                    case 36:
                        break;
                    case 37:
                        break;
                    case 38:
                        break;
                    case 39:
                        break;
                    case 40:
                        break;
                    case 41:
                        break;
                    case 42: // expression:assignment_expression
                        root.getAttributes().putAll(root.getChildren().get(0).getAttributes());
                        break;
                    case 43:
                        break;
                    case 44:
                        break;
                    case 45: // declaration:VAL init_declarator_list SEMICOLON
                        root.getAttributes().putAll(root.getChildren().get(1).getAttributes());
                        break;
                    case 46: // init_declarator_list:init_declarator
                        root.getAttributes().putAll(root.getChildren().get(0).getAttributes());
                        break;
                    case 47: // init_declarator_list:init_declarator_list COMMA init_declarator
                        root.getAttributes().put("codes", new ArrayList<Quad>());
                        if (root.getChildren().get(0).getCodes() != null)
                            root.getCodes().addAll(root.getChildren().get(0).getCodes());
                        if (root.getChildren().get(2).getCodes() != null)
                            root.getCodes().addAll(root.getChildren().get(2).getCodes());
                        break;
                    case 48: // init_declarator:IDENTIFIER
                        variable = new Variable(root.getChildren().get(0).getToken().getOriginWord(), "", null, 4, 0);
                        index = Semantics.addVariable(variable);
                        if (index == -1) {
                            Semantics.addErrorMessage(
                                    "Error at Line: " + root.getChildren().get(0).getToken().getLineNumber()
                                            + " [Variable " + root.getChildren().get(0).getToken().getOriginWord()
                                            + " has been declared before]");
                        }
                        variable.setOffset(index * 4);
                        break;
                    case 49: // init_declarator:IDENTIFIER ASSIGN initializer
                        operator = "=";
                        variable = new Variable(root.getChildren().get(0).getToken().getOriginWord(),
                                root.getChildren().get(2).getType(),
                                root.getChildren().get(2).getToken().getOriginWord(), 4, 0);
                        index = Semantics.addVariable(variable);
                        if (index == -1) {
                            Semantics.addErrorMessage(
                                    "Error at Line: " + root.getChildren().get(0).getToken().getLineNumber()
                                            + " [Variable has been declared before]");
                        }
                        variable.setOffset(index * 4);
                        // generate code
                        quad = new Quad(operator, root.getChildren().get(2).getSymbol(), null,
                                root.getChildren().get(0).getToken());
                        root.getAttributes().put("codes", new ArrayList<Quad>());
                        root.getCodes().add(quad);
                        break;
                    case 50: // identifier_list:IDENTIFIER
                        root.getAttributes().putAll(root.getChildren().get(0).getAttributes());
                        break;
                    case 51:
                        break;
                    case 52: // initializer:assignment_expression
                        root.getAttributes().putAll(root.getChildren().get(0).getAttributes());
                        break;
                    case 53:
                        break;
                    case 54:
                        break;
                    case 55:
                        break;
                    case 56:
                        break;
                    case 57:
                        break;
                    case 58: // statement:compound_statement
                    case 59: // statement:expression_statement
                    case 60: // statement:selection_statement
                    case 61: // statement:iteration_statement
                    case 62: // statement:jump_statement
                    case 63: // statement_block:statement
                    case 64: // statement_block:declaration
                        root.getAttributes().putAll(root.getChildren().get(0).getAttributes());
                        break;
                    case 65: // statement_block:statement_block statement
                        root.getAttributes().put("codes", new ArrayList<Quad>());
                        if (root.getChildren().get(0).getCodes() != null)
                            root.getCodes().addAll(root.getChildren().get(0).getCodes());
                        if (root.getChildren().get(1).getCodes() != null)
                            root.getCodes().addAll(root.getChildren().get(1).getCodes());
                        break;
                    case 66: // statement_block:statement_block declaration
                        root.getAttributes().put("codes", new ArrayList<Quad>());
                        if (root.getChildren().get(0).getCodes() != null)
                            root.getCodes().addAll(root.getChildren().get(0).getCodes());
                        if (root.getChildren().get(1).getCodes() != null)
                            root.getCodes().addAll(root.getChildren().get(1).getCodes());
                        break;
                    case 67:
                        break;
                    case 68: // compound_statement:L_CURLY statement_block R_CURLY
                        root.getAttributes().putAll(root.getChildren().get(1).getAttributes());
                        break;
                    case 69:
                        break;
                    case 70: // expression_statement:expression SEMICOLON
                        root.getAttributes().putAll(root.getChildren().get(0).getAttributes());
                        break;
                    case 71:
                        break;
                    case 72:
                        break;
                    case 73: // selection_statement:IF L_PAREN expression R_PAREN compound_statement
                        // if goto +2
                        tempToken = new Token("goto", String.valueOf(2), 0, "", -1);
                        // generate code
                        quad = new Quad("if", root.getChildren().get(2).getSymbol(), null, tempToken);
                        root.getAttributes().put("codes", new ArrayList<Quad>());
                        if (root.getChildren().get(2).getCodes() != null)
                            root.getCodes().addAll(root.getChildren().get(2).getCodes());
                        root.getCodes().add(quad);
                        if (root.getChildren().get(4).getCodes() != null) {
                            // if goto +2
                            tempToken = new Token("goto",
                                    String.valueOf(root.getChildren().get(4).getCodes().size() + 1), 0, "", -1);
                            quad = new Quad("goto", null, null, tempToken);
                            root.getCodes().add(quad);
                            root.getCodes().addAll(root.getChildren().get(4).getCodes());
                        }
                        break;
                    case 74:
                        break;
                    case 75: // selection_statement:IF L_PAREN expression R_PAREN compound_statement ELSE
                             // compound_statement
                        // if goto +2
                        tempToken = new Token("goto", String.valueOf(2), 0, "", -1);
                        // generate code
                        quad = new Quad("if", root.getChildren().get(2).getSymbol(), null, tempToken);
                        root.getAttributes().put("codes", new ArrayList<Quad>());
                        if (root.getChildren().get(2).getCodes() != null)
                            root.getCodes().addAll(root.getChildren().get(2).getCodes());
                        root.getCodes().add(quad);
                        if (root.getChildren().get(4).getCodes() != null) {
                            // if goto (if statement size) +2
                            tempToken = new Token("goto",
                                    String.valueOf(root.getChildren().get(4).getCodes().size() + 2), 0, "", -1);
                            quad = new Quad("goto", null, null, tempToken);
                            root.getCodes().add(quad);
                            root.getCodes().addAll(root.getChildren().get(4).getCodes());
                            // if goto (else statement size) +1
                            tempToken = new Token("goto",
                                    String.valueOf(root.getChildren().get(6).getCodes().size() + 1), 0, "", -1);
                            quad = new Quad("goto", null, null, tempToken);
                            root.getCodes().add(quad);
                            root.getCodes().addAll(root.getChildren().get(6).getCodes());
                        }
                        break;
                    case 76:
                        break;
                    case 77: // iteration_statement:WHILE L_PAREN expression R_PAREN compound_statement
                        // while goto +2
                        tempToken = new Token("goto", String.valueOf(2), 0, "", -1);
                        // generate code
                        quad = new Quad("if", root.getChildren().get(2).getSymbol(), null, tempToken);
                        root.getAttributes().put("codes", new ArrayList<Quad>());
                        if (root.getChildren().get(2).getCodes() != null)
                            root.getCodes().addAll(root.getChildren().get(2).getCodes());
                        root.getCodes().add(quad);
                        if (root.getChildren().get(4).getCodes() != null) {
                            // while not goto size + 2
                            tempToken = new Token("goto",
                                    String.valueOf(root.getChildren().get(4).getCodes().size() + 2), 0, "", -1);
                            quad = new Quad("goto", null, null, tempToken);
                            root.getCodes().add(quad);
                            root.getCodes().addAll(root.getChildren().get(4).getCodes());
                            // goto while condition
                            tempToken = new Token("goto",
                                    "-" + String.valueOf(root.getChildren().get(2).getCodes().size()
                                            + root.getChildren().get(4).getCodes().size() + 2),
                                    0, "", -1);
                            quad = new Quad("goto", null, null, tempToken);
                            root.getCodes().add(quad);
                        }
                        break;
                    case 78:
                        break;
                    case 79:
                        break;
                    case 80:
                        break;
                    case 81:
                        break;
                    case 82:
                        break;
                    case 83:
                        break;
                    case 84:
                        break;
                    case 85:
                        break;
                    case 86: // translation_unit:external_declaration
                        root.getAttributes().putAll(root.getChildren().get(0).getAttributes());
                        break;
                    case 87: // translation_unit:translation_unit external_declaration
                        root.getAttributes().put("codes", new ArrayList<Quad>());
                        if (root.getChildren().get(0).getCodes() != null)
                            root.getCodes().addAll(root.getChildren().get(0).getCodes());
                        if (root.getChildren().get(1).getCodes() != null)
                            root.getCodes().addAll(root.getChildren().get(1).getCodes());
                        break;
                    case 88:
                        break;
                    case 89: // external_declaration:declaration
                        root.getAttributes().putAll(root.getChildren().get(0).getAttributes());
                        break;
                    case 90: // external_declaration:expression_statement
                        root.getAttributes().putAll(root.getChildren().get(0).getAttributes());
                        break;
                    case 91: // external_declaration:selection_statement
                        root.getAttributes().putAll(root.getChildren().get(0).getAttributes());
                        break;
                    case 92: // external_declaration:iteration_statement
                        root.getAttributes().putAll(root.getChildren().get(0).getAttributes());
                        break;
                    case 93:
                        break;
                    case 94:
                        break;
                    default:
                        break;
                    }
                    symbolStack.push(formulaLeft);
                    processMessage.add("Action " + action + "  " + formula.toString());
                    symbolStackState.add(symbolStack.toString());
                    stateStackState.add(stateStack.toString());
                    if (LR1.gotoTable.get(stateStack.peek())
                            .containsKey(symbolStack.peek().getToken().getTokenValue())) {
                        int oldState = stateStack.peek();
                        stateStack.push(LR1.gotoTable.get(stateStack.peek())
                                .get(symbolStack.peek().getToken().getTokenValue()));
                        processMessage
                                .add("goto(" + oldState + "," + symbolStack.peek().getToken().getTokenValue() + ")");
                        symbolStackState.add(symbolStack.toString());
                        stateStackState.add(stateStack.toString());
                    }
                } else if (action.equals("acc")) {
                    processMessage.add("Accepted");
                    this.root = symbolStack.pop();
                    break;
                } else {
                    throw new Exception("Unsupported action type");
                }
            } else {
                Semantics.addErrorMessage("Error at Line: " + analyse.getTokenList().get(top).getLineNumber()
                        + " [Semantics analyse error near word: " + analyse.getTokenList().get(top).getOriginWord()
                        + "]");
                return;
            }
        }
    }
}