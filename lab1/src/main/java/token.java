public class token {
    private String type;
    private String originWord;
    private int tableIndex;

    token(String type, String word, int index) {
        this.type = type;
        this.originWord = word;
        this.tableIndex = index;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the tableIndex
     */
    public int getTableIndex() {
        return tableIndex;
    }

    /**
     * @return the originWord
     */
    public String getOriginWord() {
        return originWord;
    }

    /**
     * @param originWord the originWord to set
     */
    public void setOriginWord(String originWord) {
        this.originWord = originWord;
    }
}
// reserved, operator, punctuation, variable, number, comment