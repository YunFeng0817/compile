package Semantics;

public class Variable {
    private String symbol;
    private String type;
    private Object value;
    private int length;
    private int offset;

    public Variable(String symbol, String type, Object value, int length, int offset) {
        this.symbol = symbol;
        this.type = type;
        this.value = value;
        this.length = length;
        this.offset = offset;
    }

    public Variable() {

    }

    /**
     * @return the symbol
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * @return the length
     */
    public int getLength() {
        return length;
    }

    /**
     * @return the offset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @return the value
     */
    public Object getValue() {
        return value;
    }

    /**
     * @param length the length to set
     */
    public void setLength(int length) {
        this.length = length;
    }

    /**
     * @param offset the offset to set
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @param value the value to set
     */
    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Variable) {
            Variable objVariable = (Variable) obj;
            return objVariable.getSymbol().equals(this.symbol);
        }
        return false;
    }
}