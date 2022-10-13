import java.io.*;
import java.nio.file.Files;

class CompilationEngine {

    private JackTokenizer tokenizer;
    private BufferedWriter writer;
    private StringBuilder indentation;
    private SymbolTable classSymTab, subroutineSymTab;
    private String className;

    CompilationEngine(JackTokenizer tokenizer, BufferedWriter writer) throws Exception {
        this.tokenizer = tokenizer;
        this.writer = writer;
        this.indentation = new StringBuilder();
        classSymTab = new SymbolTable();
        subroutineSymTab = new SymbolTable();
    }

    private void writeOpenTag(String str) throws Exception {
        str = indentation + "<" + str + ">";
        writer.write(str, 0, str.length());
        writer.newLine();
    }

    private void writeCloseTag(String str) throws Exception {
        str = indentation + "</" + str + ">";
        writer.write(str, 0, str.length());
        writer.newLine();
    }

    /* class: 'class' className '{' classVarDec* subroutineDec* '}' */
    void compileClass() throws Exception {
        final String TAG = "class";

        if (tokenizer.hasMoreTokens())
            tokenizer.advance();
        else
            return;

        writeOpenTag(TAG);
        indentation.append("    ");

        writeKeyword(eat("class"));
        className = writeIdentifier(eat(TokenType.IDENTIFIER), Kind.CLASS, null, -1, true);
        writeSymbol(eat("{"));

        while (tokenizer.tokenType() == TokenType.KEYWORD && (tokenizer.keyword() == Keyword.STATIC || tokenizer.keyword() == Keyword.FIELD)) {
            compileClassVarDec();
        }

        while (tokenizer.tokenType() == TokenType.KEYWORD && (tokenizer.keyword() == Keyword.CONSTRUCTOR || tokenizer.keyword() == Keyword.METHOD || tokenizer.keyword() == Keyword.FUNCTION)) {
            subroutineSymTab.startSubroutine();
            compileSubroutineDec();
        }

        writeSymbol(eat("}"));

        indentation.delete(0, 4);
        writeCloseTag(TAG);
    }

    /* classVarDec: ('static' | 'field') type varName (',' varName)* ';' */
    private void compileClassVarDec() throws Exception {
        final String TAG = "classVarDec";
        Kind kind;
        String type;

        writeOpenTag(TAG);
        indentation.append("    ");

        try { writeKeyword(eat("static")); kind = Kind.STATIC; }
        catch (Exception e) {
            writeKeyword(eat("field"));
            kind = Kind.FIELD;
        }

        try { type = writeKeyword(eat("int")); }
        catch (Exception e) {
            try { type = writeKeyword(eat("char")); }
            catch (Exception e1) {
                try { type = writeKeyword(eat("boolean")); }
                catch (Exception e2) {
                    type = writeIdentifier(eat(TokenType.IDENTIFIER), Kind.CLASS, null, -1, false);
                }
            }
        }
        
        {
            String identifier = eat(TokenType.IDENTIFIER);
            classSymTab.define(identifier, kind, type);
            writeIdentifier(identifier, classSymTab.kindOf(identifier), classSymTab.typeOf(identifier), classSymTab.indexOf(identifier), true);
        }

        while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
            writeSymbol(eat(","));
            {
                String identifier = eat(TokenType.IDENTIFIER);
                classSymTab.define(identifier, kind, type);
                writeIdentifier(identifier, classSymTab.kindOf(identifier), classSymTab.typeOf(identifier), classSymTab.indexOf(identifier), true);
            }
        }

        writeSymbol(eat(";"));

        indentation.delete(0, 4);
        writeCloseTag(TAG);
    }

    /* subroutineDec: ('constructor' | 'function' | 'method') ('void' | type) subroutineName '(' parameterList ')' subroutineBody */
    private void compileSubroutineDec() throws Exception {
        final String TAG = "subroutineDec";

        writeOpenTag(TAG);
        indentation.append("    ");

        try { writeKeyword(eat("constructor")); }
        catch (Exception e) {
            try { writeKeyword(eat("function")); }
            catch (Exception e1) {
                writeKeyword(eat("method"));
                subroutineSymTab.define("this", Kind.ARG, className);
            }
        }

        if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == Keyword.VOID) {
            writeKeyword(eat("void"));
        }
        else {
            try { writeKeyword(eat("int")); }
            catch (Exception e) {
                try { writeKeyword(eat("char")); }
                catch (Exception e1) {
                    try { writeKeyword(eat("boolean")); }
                    catch (Exception e2) {
                        writeIdentifier(eat(TokenType.IDENTIFIER), Kind.CLASS, null, -1, false);
                    }
                }
            }
        }

        writeIdentifier(eat(TokenType.IDENTIFIER), Kind.SUBROUTINE, null, -1, true);
        writeSymbol(eat("("));
        compileParameterList();
        writeSymbol(eat(")"));
        compileSubroutineBody();

        indentation.delete(0, 4);
        writeCloseTag(TAG);
    }

    /* parameterList: ((type varName) (',' type varName)*)? */
    private void compileParameterList() throws Exception {
        final String TAG = "parameterList";
        String type;

        writeOpenTag(TAG);
        indentation.append("    ");

        if (tokenizer.tokenType() == TokenType.IDENTIFIER || (tokenizer.tokenType() == TokenType.KEYWORD && (tokenizer.keyword() == Keyword.INT || tokenizer.keyword() == Keyword.BOOLEAN || tokenizer.keyword() == Keyword.CHAR))) {
            try { type = writeKeyword(eat("int")); }
            catch (Exception e) {
                try { type = writeKeyword(eat("char")); }
                catch (Exception e1) {
                    try { type = writeKeyword(eat("boolean")); }
                    catch (Exception e2) {
                        type = writeIdentifier(eat(TokenType.IDENTIFIER), Kind.CLASS, null, -1, false);
                    }
                }
            }
            {
                String identifier = eat(TokenType.IDENTIFIER);
                subroutineSymTab.define(identifier, Kind.ARG, type);
                writeIdentifier(identifier, subroutineSymTab.kindOf(identifier), subroutineSymTab.typeOf(identifier), subroutineSymTab.indexOf(identifier), true);
            }
            while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
                writeSymbol(eat(","));
                try { type = writeKeyword(eat("int")); }
                catch (Exception e) {
                    try { type = writeKeyword(eat("char")); }
                    catch (Exception e1) {
                        try { type = writeKeyword(eat("boolean")); }
                        catch (Exception e2) {
                            type = writeIdentifier(eat(TokenType.IDENTIFIER), Kind.CLASS, null, -1, false);
                        }
                    }
                }
                {
                    String identifier = eat(TokenType.IDENTIFIER);
                    subroutineSymTab.define(identifier, Kind.ARG, type);
                    writeIdentifier(identifier, subroutineSymTab.kindOf(identifier), subroutineSymTab.typeOf(identifier), subroutineSymTab.indexOf(identifier), true);
                }
            }
        }

        indentation.delete(0, 4);
        writeCloseTag(TAG);
    }

    /* subroutineBody: '{' varDec* statements '}' */
    private void compileSubroutineBody() throws Exception {
        final String TAG = "subroutineBody";

        writeOpenTag(TAG);
        indentation.append("    ");

        writeSymbol(eat("{"));
        while (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == Keyword.VAR) {
            compileVarDec();
        }
        compileStatements();
        writeSymbol(eat("}"));

        indentation.delete(0, 4);
        writeCloseTag(TAG);
    }

    /* varDec: 'var' type varName (',' varName)* ';' */
    private void compileVarDec() throws Exception {
        final String TAG = "varDec";
        String type;

        writeOpenTag(TAG);
        indentation.append("    ");

        writeKeyword(eat("var"));
        try { type = writeKeyword(eat("int")); }
        catch (Exception e) {
            try { type = writeKeyword(eat("char")); }
            catch (Exception e1) {
                try { type = writeKeyword(eat("boolean")); }
                catch (Exception e2) {
                    type = writeIdentifier(eat(TokenType.IDENTIFIER), Kind.CLASS, null, -1, false);
                }
            }
        }
        {
            String identifier = eat(TokenType.IDENTIFIER);
            subroutineSymTab.define(identifier, Kind.VAR, type);
            writeIdentifier(identifier, subroutineSymTab.kindOf(identifier), subroutineSymTab.typeOf(identifier), subroutineSymTab.indexOf(identifier), true);
        }
        while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
            writeSymbol(eat(","));
            {
                String identifier = eat(TokenType.IDENTIFIER);
                subroutineSymTab.define(identifier, Kind.VAR, type);
                writeIdentifier(identifier, subroutineSymTab.kindOf(identifier), subroutineSymTab.typeOf(identifier), subroutineSymTab.indexOf(identifier), true);
            }
        }
        writeSymbol(eat(";"));

        indentation.delete(0, 4);
        writeCloseTag(TAG);
    }

    /*
    statements: statement*
    statement: letStatement | ifStatement | whileStatement | doStatement | returnStatement
    */
    private void compileStatements() throws Exception {
        final String TAG = "statements";

        writeOpenTag(TAG);
        indentation.append("    ");

        loop:
        while (tokenizer.tokenType() == TokenType.KEYWORD) {
            switch (tokenizer.keyword()) {
                case LET:
                    compileLet();
                    break;
                case IF:
                    compileIf();
                    break;
                case WHILE:
                    compileWhile();
                    break;
                case DO:
                    compileDo();
                    break;
                case RETURN:
                    compileReturn();
                    break;
                default:
                    break loop;
            }
        }

        indentation.delete(0, 4);
        writeCloseTag(TAG);
    }

    /* letStatement: 'let' varName ('[' expression ']')? '=' expression ';' */
    private void compileLet() throws Exception {
        final String TAG = "letStatement";

        writeOpenTag(TAG);
        indentation.append("    ");

        writeKeyword(eat("let"));
        {
            String identifier = eat(TokenType.IDENTIFIER);
            SymbolTable symTab = subroutineSymTab.kindOf(identifier) != Kind.NONE ? subroutineSymTab : classSymTab.kindOf(identifier) != Kind.NONE ? classSymTab : null;
            if (symTab != null) {
                writeIdentifier(identifier, symTab.kindOf(identifier), symTab.typeOf(identifier), symTab.indexOf(identifier), false);
            }
            else {
                throw new Exception(String.format("Unknown identifier found: %s.", identifier));
            }
            
        }
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '[') {
            writeSymbol(eat("["));
            compileExpression();
            writeSymbol(eat("]"));
        }
        writeSymbol(eat("="));
        compileExpression();
        writeSymbol(eat(";"));

        indentation.delete(0, 4);
        writeCloseTag(TAG);
    }

    /* ifStatement: 'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}')? */
    private void compileIf() throws Exception {
        final String TAG = "ifStatement";

        writeOpenTag(TAG);
        indentation.append("    ");

        writeKeyword(eat("if"));
        writeSymbol(eat("("));
        compileExpression();
        writeSymbol(eat(")"));
        writeSymbol(eat("{"));
        compileStatements();
        writeSymbol(eat("}"));
        if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == Keyword.ELSE) {
            writeKeyword(eat("else"));
            writeSymbol(eat("{"));
            compileStatements();
            writeSymbol(eat("}"));
        }

        indentation.delete(0, 4);
        writeCloseTag(TAG);
    }

    /* whileStatement: 'while' '(' expression ')' '{' statements '}' */
    private void compileWhile() throws Exception {
        final String TAG = "whileStatement";

        writeOpenTag(TAG);
        indentation.append("    ");

        writeKeyword(eat("while"));
        writeSymbol(eat("("));
        compileExpression();
        writeSymbol(eat(")"));
        writeSymbol(eat("{"));
        compileStatements();
        writeSymbol(eat("}"));

        indentation.delete(0, 4);
        writeCloseTag(TAG);
    }

    /*
    doStatement: do subroutineCall ';'
    subroutineCall: subroutineName '(' expressionList ')' | (className | varName) '.' subroutineName '(' expressionList ')'
    */
    private void compileDo() throws Exception {
        final String TAG = "doStatement";

        writeOpenTag(TAG);
        indentation.append("    ");

        writeKeyword(eat("do"));

        String identifier = eat(TokenType.IDENTIFIER);
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '(') {
            writeIdentifier(identifier, Kind.SUBROUTINE, null, -1, false);
            writeSymbol(eat("("));
            compileExpressionList();
            writeSymbol(eat(")"));
        }
        else {
            SymbolTable symTab = subroutineSymTab.kindOf(identifier) != Kind.NONE ? subroutineSymTab : classSymTab.kindOf(identifier) != Kind.NONE ? classSymTab : null;
            if (symTab != null) {
                writeIdentifier(identifier, symTab.kindOf(identifier), symTab.typeOf(identifier), symTab.indexOf(identifier), false);
            }
            else {
                writeIdentifier(identifier, Kind.CLASS, null, -1, false);
            }
            writeSymbol(eat("."));
            writeIdentifier(eat(TokenType.IDENTIFIER), Kind.SUBROUTINE, null, -1, false);
            writeSymbol(eat("("));
            compileExpressionList();
            writeSymbol(eat(")"));
        }
        
        writeSymbol(eat(";"));

        indentation.delete(0, 4);
        writeCloseTag(TAG);
    }

    /* returnStatement: 'return' expression? ';' */
    private void compileReturn() throws Exception {
        final String TAG = "returnStatement";

        writeOpenTag(TAG);
        indentation.append("    ");

        writeKeyword(eat("return"));
        if (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ';')) {
            compileExpression();
        }
        writeSymbol(eat(";"));

        indentation.delete(0, 4);
        writeCloseTag(TAG);
    }

    /*
    expression: term (op term)*
    op: '+', '-', '*', '/', '&', '|', '<', '>', '='
    */
    private void compileExpression() throws Exception {
        final String TAG = "expression";
        final char[] operators = { '+', '-', '*', '/', '&', '|', '<', '>', '=' };
        boolean operatorFound = false;

        writeOpenTag(TAG);
        indentation.append("    ");

        compileTerm();
        do {
            if (tokenizer.tokenType() == TokenType.SYMBOL) {
                for (char operator : operators) {
                    operatorFound = tokenizer.symbol() == operator;
                    if (operatorFound) {
                        writeSymbol(eat(tokenizer.stringVal()));
                        compileTerm();
                        break;
                    }
                }
            }
            else {
                break;
            }
        }
        while (operatorFound);

        indentation.delete(0, 4);
        writeCloseTag(TAG);
    }

    /* term: integerConstant | stringConstant | keywordConstant | varName | varName '[' expression ']' | subroutineCall | '(' expression ')' | unaryOp term */
    private void compileTerm() throws Exception {
        final String TAG = "term";

        writeOpenTag(TAG);
        indentation.append("    ");

        if (tokenizer.tokenType() == TokenType.INT_CONST) {
            writeIntConst(eat(TokenType.INT_CONST));
        }
        else if (tokenizer.tokenType() == TokenType.STRING_CONST) {
            writeStrConst(eat(TokenType.STRING_CONST));
        }
        else if (tokenizer.tokenType() == TokenType.KEYWORD) {
            writeKeyword(eat(TokenType.KEYWORD));
        }
        else if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '(') {
            writeSymbol(eat("("));
            compileExpression();
            writeSymbol(eat(")"));
        }
        else if (tokenizer.tokenType() == TokenType.SYMBOL && (tokenizer.symbol() == '-' || tokenizer.symbol() == '~')) {
            writeSymbol(eat(tokenizer.stringVal()));
            compileTerm();
        }
        else if (tokenizer.tokenType() == TokenType.IDENTIFIER) {
            String identifier = eat(TokenType.IDENTIFIER);
            SymbolTable symTab = subroutineSymTab.kindOf(identifier) != Kind.NONE ? subroutineSymTab : classSymTab.kindOf(identifier) != Kind.NONE ? classSymTab : null;
            if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '[') {
                if (symTab != null) {
                    writeIdentifier(identifier, symTab.kindOf(identifier), symTab.typeOf(identifier), symTab.indexOf(identifier), false);
                }
                else {
                    throw new Exception(String.format("Unknown identifier found: %s.", identifier));
                }
                writeSymbol(eat("["));
                compileExpression();
                writeSymbol(eat("]"));
            }
            else if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '(') {
                writeIdentifier(identifier, Kind.SUBROUTINE, null, -1, false);
                writeSymbol(eat("("));
                compileExpressionList();
                writeSymbol(eat(")"));
            }
            else if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '.') {
                if (symTab != null) {
                    writeIdentifier(identifier, symTab.kindOf(identifier), symTab.typeOf(identifier), symTab.indexOf(identifier), false);
                }
                else {
                    writeIdentifier(identifier, Kind.CLASS, null, -1, false);
                }
                writeSymbol(eat("."));
                writeIdentifier(eat(TokenType.IDENTIFIER), Kind.SUBROUTINE, null, -1, false);
                writeSymbol(eat("("));
                compileExpressionList();
                writeSymbol(eat(")"));
            }
            else {
                writeIdentifier(identifier, symTab.kindOf(identifier), symTab.typeOf(identifier), symTab.indexOf(identifier), false);
            }
        }
        else {
            throw new Exception("A valid token was expected but not found.");
        }

        indentation.delete(0, 4);
        writeCloseTag(TAG);
    }

    /* expressionList: (expression (',' expression)*)? */
    /* term: integerConstant | stringConstant | keywordConstant | varName | varName '[' expression ']' | subroutineCall | '(' expression ')' | unaryOp term */
    private void compileExpressionList() throws Exception {
        final String TAG = "expressionList";

        writeOpenTag(TAG);
        indentation.append("    ");

        switch (tokenizer.tokenType()) {
            case SYMBOL:
                if (!(tokenizer.symbol() == '(' || tokenizer.symbol() == '-' || tokenizer.symbol() == '~')) {
                    break;
                }
            case INT_CONST:
            case STRING_CONST:
            case KEYWORD:
            case IDENTIFIER:
                compileExpression();
                while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
                    writeSymbol(eat(","));
                    compileExpression();
                }
                break;
        }

        indentation.delete(0, 4);
        writeCloseTag(TAG);
    }

    private String eat(String token) throws Exception {
        if (tokenizer.stringVal().equals(token)) {
            if (tokenizer.hasMoreTokens()) {
                tokenizer.advance();
            }
        }
        else {
            throw new Exception(String.format("Expected token %s but encountered %s.", token.toString(), tokenizer.stringVal()));
        }
        return token;
    }

    private String eat(TokenType tokenType) throws Exception {
        String token = tokenizer.stringVal();
        if (tokenizer.tokenType() == tokenType) {
            if (tokenizer.hasMoreTokens()) {
                tokenizer.advance();
            }
        }
        else {
            throw new Exception(String.format("Expected token of type %s but encountered %s.", tokenType.toString(), tokenizer.stringVal()));
        }
        return token;
    }

    private String writeSymbol(String symbol) throws Exception {
        symbol = symbol.replaceAll("\\&", "&amp;").replaceAll("\\<", "&lt;").replaceAll("\\>", "&gt;").replaceAll("\\\"", "&quot;").replaceAll("\\'", "&apos;");
        final String line = String.format("%s<symbol>%s</symbol>", indentation.toString(), symbol);
        writer.write(line, 0, line.length());
        writer.newLine();
        return symbol;
    }

    private String writeIntConst(String intConst) throws Exception {
        final String line = String.format("%s<integerConstant>%s</integerConstant>", indentation.toString(), intConst);
        writer.write(line, 0, line.length());
        writer.newLine();
        return intConst;
    }

    private String writeStrConst(String strConst) throws Exception {
        strConst = strConst.replaceAll("\\&", "&amp;").replaceAll("\\<", "&lt;").replaceAll("\\>", "&gt;").replaceAll("\\\"", "&quot;").replaceAll("\\'", "&apos;");
        final String line = String.format("%s<stringConstant>%s</stringConstant>", indentation.toString(), strConst);
        writer.write(line, 0, line.length());
        writer.newLine();
        return strConst;
    }

    private String writeKeyword(String keyword) throws Exception {
        final String line = String.format("%s<keyword>%s</keyword>", indentation.toString(), keyword);
        writer.write(line, 0, line.length());
        writer.newLine();
        return keyword;
    }

    // private String writeIdentifier(String identifier, Kind kind, String type, int index, boolean defined) throws Exception {
    //     identifier = identifier.replaceAll("\\&", "&amp;").replaceAll("\\<", "&lt;").replaceAll("\\>", "&gt;").replaceAll("\\\"", "&quot;").replaceAll("\\'", "&apos;");
    //     final String line = String.format("%s<identifier>%s</identifier>", indentation.toString(), identifier);
    //     writer.write(line, 0, line.length());
    //     writer.newLine();
    //     return identifier;
    // }

    private String writeIdentifier(String identifier, Kind kind, String type, int index, boolean defined) throws Exception {
        String line = String.format("%s<identifier", indentation.toString());

        line += String.format(" category=\"%s\"", kind.toString());
        if (type != null) {
            line += String.format(" type=\"%s\"", type);
        }
        if (index > -1) {
            line += String.format(" index=\"%d\"", index);
        }
        line += String.format(" context=\"%s\"", defined ? "defined" : "used");

        identifier = identifier.replaceAll("\\&", "&amp;").replaceAll("\\<", "&lt;").replaceAll("\\>", "&gt;").replaceAll("\\\"", "&quot;").replaceAll("\\'", "&apos;");
        line += String.format(">%s</identifier>", identifier);

        writer.write(line, 0, line.length());
        writer.newLine();
        return identifier;
    }
}