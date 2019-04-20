import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.Stack;

public class Main {
    public static void main(String[] args) throws Exception {
        String grammarInputFile = "grammar_rules.txt";
        new LR1(grammarInputFile);
        // String inputFilePath = "input.txt";
        // BufferedReader bufferedReader = new BufferedReader(new
        // FileReader(inputFilePath));
        // String line;
        // String content = "";
        // while ((line = bufferedReader.readLine()) != null) {
        // content += line;
        // }
        // bufferedReader.close();
        // int top = 0;
        // Stack<String> symbolStack = new Stack<>();
        // Stack<Integer> stateStack = new Stack<>();
        // symbolStack.push(LR1.endString);
        // stateStack.push(0);
        // while (true) {
        // String symbol = String.valueOf(content.charAt(top));
        // if (LR1.actionTable.get(stateStack.peek()).containsKey(symbol)) {
        // String action = LR1.actionTable.get(stateStack.peek()).get(symbol);
        // String actionType = String.valueOf(action.charAt(0));
        // final String SHIFT = "s";
        // final String REDUCE = "r";
        // Integer actionState = null;
        // if (!action.equals("acc")) {
        // actionState = Integer.parseInt(action.substring(1, action.length()));
        // }
        // if (actionType.equals(SHIFT)) {
        // symbolStack.push(symbol);
        // stateStack.push(actionState);
        // System.out.println("Action " + action);
        // System.out.println(symbolStack);
        // System.out.println(stateStack);
        // System.out.println();
        // top += 1;
        // } else if (actionType.equals(REDUCE)) {
        // Formula formula = LR1.grammars.get(actionState);
        // List<String> formulaRight = formula.getSymbols();
        // Stack<String> stackFormulaRight = new Stack<>();
        // if (!(formulaRight.size() == 1 &&
        // formulaRight.get(0).equals(LR1.nullString))) {
        // for (int i = 0; i < formulaRight.size(); i++) {
        // stackFormulaRight.push(symbolStack.pop());
        // stateStack.pop();
        // }
        // for (String str : formulaRight) {
        // if (!str.equals(stackFormulaRight.pop())) {
        // throw new Exception("Error occurs when reducing");
        // }
        // }
        // }
        // symbolStack.push(formula.getPrefix());
        // System.out.println("Action " + action);
        // System.out.println(symbolStack);
        // System.out.println(stateStack);
        // System.out.println();
        // if (LR1.gotoTable.get(stateStack.peek()).containsKey(symbolStack.peek())) {
        // int oldState = stateStack.peek();
        // stateStack.push(LR1.gotoTable.get(stateStack.peek()).get(symbolStack.peek()));
        // System.out.println("goto(" + oldState + "," + symbolStack.peek() + ")");
        // System.out.println(symbolStack);
        // System.out.println(stateStack);
        // System.out.println();
        // }
        // } else if (action.equals("acc")) {
        // break;
        // } else {
        // throw new Exception("Unsupported action type");
        // }
        // }
        // }
        // System.out.println("accepted");
    }
}