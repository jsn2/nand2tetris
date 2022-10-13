import java.util.Hashtable;

class SymbolTable {

    private Hashtable<String, Identifier> symbols;
    private Hashtable<String, Integer> indexCounts;

    /** Represents the values of the symbol table. */
    private class Identifier {
        private Kind kind;
        private String type;
        private int index;

        Identifier(Kind kind, String type, int index) {
            this.kind = kind;
            this.type = type;
            this.index = index;
        }

        Kind getKind() { return kind; }

        String getType() { return type; }

        int getIndex() { return index; }
    }

    SymbolTable() {
        symbols = new Hashtable<String, Identifier>();
        indexCounts = new Hashtable<String, Integer>();
        resetIndexCounts();
    }
    
    /** Starts a new subroutine scope (resets the symbol table). */
    void startSubroutine() {
        symbols.clear();
        resetIndexCounts();
    }

    /** Defines a new identifier of the given name, type and kind, and assigns a running index.
    static and field identifiers have a class scope.
    arg and var identifiers have a subroutine scope. */
    void define(String name, Kind kind, String type) throws Exception {
        int count = varCount(kind);
        symbols.put(name, new Identifier(kind, type, count));
        indexCounts.put(kind.toString(), ++count);
    }

    /** Returns the number of variables of the given kind already defined in the current scope. */
    int varCount(Kind kind) throws Exception {
        return indexCounts.get(kind.toString()).intValue();
    }

    /** Returns the kind of the named identifier in the current scope.
    If the identifier is unknown in the current scope, returns NONE. */
    Kind kindOf(String name) {
        if (symbols.containsKey(name)) {
            return symbols.get(name).getKind();
        }
        else {
            return Kind.NONE;
        }
    }

    /** Returns the type of the named identifier in the current scope. */
    String typeOf(String name) {
        return symbols.get(name).getType();
    }

    /** Returns the index assigned to the named identifier. */
    int indexOf(String name) {
        return symbols.get(name).getIndex();
    }

    private void resetIndexCounts() {
        indexCounts.put(Kind.STATIC.toString(), Integer.valueOf(0));
        indexCounts.put(Kind.FIELD.toString(), Integer.valueOf(0));
        indexCounts.put(Kind.ARG.toString(), Integer.valueOf(0));
        indexCounts.put(Kind.VAR.toString(), Integer.valueOf(0));
    }
}