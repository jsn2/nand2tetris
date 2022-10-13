import java.io.*;
import java.nio.file.Path;
import java.nio.file.Files;

class CompilationEngine {

    private JackTokenizer tokenizer;
    private VMWriter vmWriter;

    private StringBuilder indentation;

    private SymbolTable classSymTab, subroutineSymTab;
    private String className;
    private int ifLabelIndex, whileLabelIndex;

    CompilationEngine(JackTokenizer tokenizer, Path path) throws Exception {
        this.tokenizer = tokenizer;
        this.vmWriter = new VMWriter(path);
        
        this.classSymTab = new SymbolTable();
        this.subroutineSymTab = new SymbolTable();
    }

    /* class: 'class' className '{' classVarDec* subroutineDec* '}' */
    void compileClass() throws Exception {
        if (tokenizer.hasMoreTokens())
            tokenizer.advance();
        else
            return;

        eat("class");
        className = eat(TokenType.IDENTIFIER);
        eat("{");
        while (tokenizer.tokenType() == TokenType.KEYWORD && (tokenizer.keyword() == Keyword.STATIC || tokenizer.keyword() == Keyword.FIELD)) {
            compileClassVarDec();
        }
        while (tokenizer.tokenType() == TokenType.KEYWORD && (tokenizer.keyword() == Keyword.CONSTRUCTOR || tokenizer.keyword() == Keyword.METHOD || tokenizer.keyword() == Keyword.FUNCTION)) {
            subroutineSymTab.startSubroutine();
            compileSubroutineDec();
        }
        eat("}");

        vmWriter.close();
    }

    /* classVarDec: ('static' | 'field') type varName (',' varName)* ';' */
    private void compileClassVarDec() throws Exception {
        Kind kind;
        String type;

        try { eat("static"); kind = Kind.STATIC; }
        catch (Exception e) {
            eat("field");
            kind = Kind.FIELD;
        }
        try { type = eat("int"); }
        catch (Exception e) {
            try { type = eat("char"); }
            catch (Exception e1) {
                try { type = eat("boolean"); }
                catch (Exception e2) {
                    type = eat(TokenType.IDENTIFIER);
                }
            }
        }
        {
            String identifier = eat(TokenType.IDENTIFIER);
            classSymTab.define(identifier, kind, type);
        }
        while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
            eat(",");
            {
                String identifier = eat(TokenType.IDENTIFIER);
                classSymTab.define(identifier, kind, type);
            }
        }
        eat(";");
    }

    /* subroutineDec: ('constructor' | 'function' | 'method') ('void' | type) subroutineName '(' parameterList ')' subroutineBody */
    private void compileSubroutineDec() throws Exception {
        String subroutineName, subroutineType;
        try { subroutineType = eat("constructor"); }
        catch (Exception e) {
            try { subroutineType = eat("function"); }
            catch (Exception e1) {
                subroutineType = eat("method");
                subroutineSymTab.define("this", Kind.ARG, className);
            }
        }
        if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == Keyword.VOID) {
            eat("void");
        }
        else {
            try { eat("int"); }
            catch (Exception e) {
                try { eat("char"); }
                catch (Exception e1) {
                    try { eat("boolean"); }
                    catch (Exception e2) {
                        eat(TokenType.IDENTIFIER);
                    }
                }
            }
        }
        subroutineName = eat(TokenType.IDENTIFIER);
        eat("(");
        compileParameterList();
        eat(")");
        ifLabelIndex = 0;
        whileLabelIndex = 0;
        compileSubroutineBody(subroutineName, subroutineType);
    }

    /* parameterList: ((type varName) (',' type varName)*)? */
    private void compileParameterList() throws Exception {
        String type;

        if (tokenizer.tokenType() == TokenType.IDENTIFIER || (tokenizer.tokenType() == TokenType.KEYWORD && (tokenizer.keyword() == Keyword.INT || tokenizer.keyword() == Keyword.BOOLEAN || tokenizer.keyword() == Keyword.CHAR))) {
            try { type = eat("int"); }
            catch (Exception e) {
                try { type = eat("char"); }
                catch (Exception e1) {
                    try { type = eat("boolean"); }
                    catch (Exception e2) {
                        type = eat(TokenType.IDENTIFIER);
                    }
                }
            }
            {
                String identifier = eat(TokenType.IDENTIFIER);
                subroutineSymTab.define(identifier, Kind.ARG, type);
            }
            while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
                eat(",");
                try { type = eat("int"); }
                catch (Exception e) {
                    try { type = eat("char"); }
                    catch (Exception e1) {
                        try { type = eat("boolean"); }
                        catch (Exception e2) {
                            type = eat(TokenType.IDENTIFIER);
                        }
                    }
                }
                {
                    String identifier = eat(TokenType.IDENTIFIER);
                    subroutineSymTab.define(identifier, Kind.ARG, type);
                }
            }
        }
    }

    /* subroutineBody: '{' varDec* statements '}' */
    private void compileSubroutineBody(String subroutineName, String subroutineType) throws Exception {
        eat("{");
        while (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == Keyword.VAR) {
            compileVarDec();
        }
        vmWriter.writeFunction(String.format("%s.%s", className, subroutineName), subroutineSymTab.varCount(Kind.VAR));
        if (subroutineType.equals("constructor")) {
            vmWriter.writePush(Segment.CONST, classSymTab.varCount(Kind.FIELD));
            vmWriter.writeCall("Memory.alloc", 1);
            vmWriter.writePop(Segment.POINTER, 0);
        }
        else if (subroutineType.equals("method")) {
            vmWriter.writePush(Segment.ARG, 0);
            vmWriter.writePop(Segment.POINTER, 0);
        }
        compileStatements();
        eat("}");
    }

    /* varDec: 'var' type varName (',' varName)* ';' */
    private void compileVarDec() throws Exception {
        String type;

        eat("var");
        try { type = eat("int"); }
        catch (Exception e) {
            try { type = eat("char"); }
            catch (Exception e1) {
                try { type = eat("boolean"); }
                catch (Exception e2) {
                    type = eat(TokenType.IDENTIFIER);
                }
            }
        }
        {
            String identifier = eat(TokenType.IDENTIFIER);
            subroutineSymTab.define(identifier, Kind.VAR, type);
        }
        while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
            eat(",");
            {
                String identifier = eat(TokenType.IDENTIFIER);
                subroutineSymTab.define(identifier, Kind.VAR, type);
            }
        }
        eat(";");
    }

    /*
    statements: statement*
    statement: letStatement | ifStatement | whileStatement | doStatement | returnStatement
    */
    private void compileStatements() throws Exception {
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
    }

    /* letStatement: 'let' varName ('[' expression ']')? '=' expression ';' */
    private void compileLet() throws Exception {
        String identifier;
        SymbolTable symTab;

        eat("let");
        identifier = eat(TokenType.IDENTIFIER);
        symTab = subroutineSymTab.kindOf(identifier) != Kind.NONE ? subroutineSymTab : classSymTab.kindOf(identifier) != Kind.NONE ? classSymTab : null;
        if (symTab == null) {
            throw new Exception(String.format("Unknown identifier found: %s.", identifier));
        }
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '[') {
            vmWriter.writePush(kindToSegment(symTab.kindOf(identifier)), symTab.indexOf(identifier));
            eat("[");
            compileExpression();
            vmWriter.writeArithmetic(Command.ADD);
            eat("]");
            eat("=");
            compileExpression();
            vmWriter.writePop(Segment.TEMP, 0);
            vmWriter.writePop(Segment.POINTER, 1);
            vmWriter.writePush(Segment.TEMP, 0);
            vmWriter.writePop(Segment.THAT, 0);
            eat(";");
        }
        else {
            eat("=");
            compileExpression();
            eat(";");
            vmWriter.writePop(kindToSegment(symTab.kindOf(identifier)), symTab.indexOf(identifier));
        }
    }

    /* ifStatement: 'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}')? */
    private void compileIf() throws Exception {
        final int index = ifLabelIndex++;
        final String IF_TRUE = "IF_TRUE_" + index, IF_FALSE = "IF_FALSE_" + index, IF_END = "IF_END_" + index;

        eat("if");
        eat("(");
        compileExpression();
        vmWriter.writeIf(IF_TRUE);
        vmWriter.writeGoto(IF_FALSE);
        vmWriter.writeLabel(IF_TRUE);
        eat(")");
        eat("{");
        compileStatements();
        eat("}");
        if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == Keyword.ELSE) {
            vmWriter.writeGoto(IF_END);
            vmWriter.writeLabel(IF_FALSE);
            eat("else");
            eat("{");
            compileStatements();
            eat("}");
            vmWriter.writeLabel(IF_END);
        }
        else {
            vmWriter.writeLabel(IF_FALSE);
        }
    }

    /* whileStatement: 'while' '(' expression ')' '{' statements '}' */
    private void compileWhile() throws Exception {
        final int index = whileLabelIndex++;
        final String WHILE_BEGIN = "WHILE_BEGIN_" + index, WHILE_END = "WHILE_END_" + index;

        eat("while");
        eat("(");
        vmWriter.writeLabel(WHILE_BEGIN);
        compileExpression();
        vmWriter.writeArithmetic(Command.NOT);
        vmWriter.writeIf(WHILE_END);
        eat(")");
        eat("{");
        compileStatements();
        vmWriter.writeGoto(WHILE_BEGIN);
        eat("}");
        vmWriter.writeLabel(WHILE_END);
    }

    /*
    doStatement: do subroutineCall ';'
    subroutineCall: subroutineName '(' expressionList ')' | (className | varName) '.' subroutineName '(' expressionList ')'
    */
    private void compileDo() throws Exception {
        String identifier, className, subroutineName;
        int nArgs = 0;

        eat("do");
        identifier = eat(TokenType.IDENTIFIER);
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '(') {
            // Method of current class.
            className = this.className;
            subroutineName = identifier;
            vmWriter.writePush(Segment.POINTER, 0);
            nArgs++;
            eat("(");
            nArgs += compileExpressionList();
            eat(")");
        }
        else {
            SymbolTable symTab = subroutineSymTab.kindOf(identifier) != Kind.NONE ? subroutineSymTab : classSymTab.kindOf(identifier) != Kind.NONE ? classSymTab : null;
            if (symTab != null) {
                // Method with object name prefix.
                className = symTab.typeOf(identifier);
                vmWriter.writePush(kindToSegment(symTab.kindOf(identifier)), symTab.indexOf(identifier));
                nArgs++;
            }
            else {
                // Function.
                className = identifier;
            }
            eat(".");
            subroutineName = eat(TokenType.IDENTIFIER);
            eat("(");
            nArgs += compileExpressionList();
            eat(")");
        }
        eat(";");
        vmWriter.writeCall(String.format("%s.%s", className, subroutineName), nArgs);
        vmWriter.writePop(Segment.TEMP, 0);
    }

    /* expressionList: (expression (',' expression)*)? */
    /* term: integerConstant | stringConstant | keywordConstant | varName | varName '[' expression ']' | subroutineCall | '(' expression ')' | unaryOp term */
    private int compileExpressionList() throws Exception {
        int nArgs = 0;

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
                nArgs++;
                while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
                    eat(",");
                    compileExpression();
                    nArgs++;
                }
                break;
        }

        return nArgs;
    }

    /*
    expression: term (op term)*
    op: '+', '-', '*', '/', '&', '|', '<', '>', '='
    */
    private void compileExpression() throws Exception {
        final char[] operators = { '+', '-', '&', '|', '<', '>', '=', '*', '/' };
        final Command[] commands = { Command.ADD, Command.SUB, Command.AND, Command.OR, Command.LT, Command.GT, Command.EQ };
        boolean operatorFound = false;

        compileTerm();
        do {
            if (tokenizer.tokenType() == TokenType.SYMBOL) {
                for (int i = 0; i < operators.length; i++) {
                    operatorFound = tokenizer.symbol() == operators[i];
                    if (operatorFound) {
                        eat(tokenizer.stringVal());
                        compileTerm();
                        if (operators[i] == '*')
                            vmWriter.writeCall("Math.multiply", 2);
                        else if (operators[i] == '/')
                            vmWriter.writeCall("Math.divide", 2);
                        else
                            vmWriter.writeArithmetic(commands[i]);
                        break;
                    }
                }
            }
            else {
                break;
            }
        }
        while (operatorFound);
    }

    /* term: integerConstant | stringConstant | keywordConstant | varName | varName '[' expression ']' | subroutineCall | '(' expression ')' | unaryOp term */
    private void compileTerm() throws Exception {
        if (tokenizer.tokenType() == TokenType.INT_CONST) {
            vmWriter.writePush(Segment.CONST, Integer.parseInt(eat(TokenType.INT_CONST)));
        }
        else if (tokenizer.tokenType() == TokenType.STRING_CONST) {
            String str = eat(TokenType.STRING_CONST);
            vmWriter.writePush(Segment.CONST, str.length());
            vmWriter.writeCall("String.new", 1);
            for (int i = 0; i < str.length(); i++) {
                vmWriter.writePush(Segment.CONST, str.charAt(i));
                vmWriter.writeCall("String.appendChar", 2);
            }
        }
        else if (tokenizer.tokenType() == TokenType.KEYWORD && (tokenizer.keyword() == Keyword.TRUE || tokenizer.keyword() == Keyword.FALSE || tokenizer.keyword() == Keyword.NULL || tokenizer.keyword() == Keyword.THIS)) {
            switch (tokenizer.keyword()) {
                case TRUE:
                    vmWriter.writePush(Segment.CONST, 1);
                    vmWriter.writeArithmetic(Command.NEG);
                    break;
                case FALSE:
                case NULL:
                    vmWriter.writePush(Segment.CONST, 0);
                    break;
                case THIS:
                    vmWriter.writePush(Segment.POINTER, 0);
                    break;
            }
            eat(TokenType.KEYWORD);
        }
        else if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '(') {
            eat("(");
            compileExpression();
            eat(")");
        }
        else if (tokenizer.tokenType() == TokenType.SYMBOL && (tokenizer.symbol() == '-' || tokenizer.symbol() == '~')) {
            char symbol = eat(tokenizer.stringVal()).charAt(0);
            compileTerm();
            vmWriter.writeArithmetic(symbol == '-' ? Command.NEG : Command.NOT);
        }
        else if (tokenizer.tokenType() == TokenType.IDENTIFIER) {
            int nArgs = 0;
            String className, subroutineName, identifier = eat(TokenType.IDENTIFIER);
            SymbolTable symTab = subroutineSymTab.kindOf(identifier) != Kind.NONE ? subroutineSymTab : classSymTab.kindOf(identifier) != Kind.NONE ? classSymTab : null;
            
            if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '[') {
                // Array.
                if (symTab == null) {
                    throw new Exception(String.format("Unknown identifier found: %s.", identifier));
                }
                eat("[");
                vmWriter.writePush(kindToSegment(symTab.kindOf(identifier)), symTab.indexOf(identifier));
                compileExpression();
                vmWriter.writeArithmetic(Command.ADD);
                vmWriter.writePop(Segment.POINTER, 1);
                vmWriter.writePush(Segment.THAT, 0);
                eat("]");
            }
            else if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '(') {
                // Method without object name prefix.
                className = this.className;
                subroutineName = identifier;
                vmWriter.writePush(Segment.POINTER, 0);
                nArgs++;
                eat("(");
                nArgs += compileExpressionList();
                eat(")");
                vmWriter.writeCall(String.format("%s.%s", className, subroutineName), nArgs);
            }
            else if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '.') {
                if (symTab != null) {
                    // Method with object name prefix.
                    className = symTab.typeOf(identifier);
                    vmWriter.writePush(kindToSegment(symTab.kindOf(identifier)), symTab.indexOf(identifier));
                    nArgs++;
                }
                else {
                    // Function.
                    className = identifier;
                }
                eat(".");
                subroutineName = eat(TokenType.IDENTIFIER);
                eat("(");
                nArgs += compileExpressionList();
                eat(")");
                vmWriter.writeCall(String.format("%s.%s", className, subroutineName), nArgs);
            }
            else {
                // varName.
                vmWriter.writePush(kindToSegment(symTab.kindOf(identifier)), symTab.indexOf(identifier));
            }
        }
        else {
            throw new Exception("A valid token was expected but not found.");
        }
    }

    /* returnStatement: 'return' expression? ';' */
    private void compileReturn() throws Exception {
        eat("return");
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ';') {
            vmWriter.writePush(Segment.CONST, 0);
        }
        else {
            compileExpression();
        }
        eat(";");
        vmWriter.writeReturn();
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

    private static Segment kindToSegment(Kind kind) throws Exception {
        switch (kind) {
            case FIELD:
                return Segment.THIS;
            case STATIC:
                return Segment.STATIC;
            case ARG:
                return Segment.ARG;
            case VAR:
                return Segment.LOCAL;
            default:
                throw new Exception(String.format("An unexpected error occurred."));
        }
    }

    // private String writeSymbol(String symbol) throws Exception {
    //     symbol = symbol.replaceAll("\\&", "&amp;").replaceAll("\\<", "&lt;").replaceAll("\\>", "&gt;").replaceAll("\\\"", "&quot;").replaceAll("\\'", "&apos;");
    //     final String line = String.format("%s<symbol>%s</symbol>", indentation.toString(), symbol);
    //     writer.write(line, 0, line.length());
    //     writer.newLine();
    //     return symbol;
    // }

    // private String writeIntConst(String intConst) throws Exception {
    //     final String line = String.format("%s<integerConstant>%s</integerConstant>", indentation.toString(), intConst);
    //     writer.write(line, 0, line.length());
    //     writer.newLine();
    //     return intConst;
    // }

    // private String writeStrConst(String strConst) throws Exception {
    //     strConst = strConst.replaceAll("\\&", "&amp;").replaceAll("\\<", "&lt;").replaceAll("\\>", "&gt;").replaceAll("\\\"", "&quot;").replaceAll("\\'", "&apos;");
    //     final String line = String.format("%s<stringConstant>%s</stringConstant>", indentation.toString(), strConst);
    //     writer.write(line, 0, line.length());
    //     writer.newLine();
    //     return strConst;
    // }

    // private String writeKeyword(String keyword) throws Exception {
    //     final String line = String.format("%s<keyword>%s</keyword>", indentation.toString(), keyword);
    //     writer.write(line, 0, line.length());
    //     writer.newLine();
    //     return keyword;
    // }

    // private String writeIdentifier(String identifier, Kind kind, String type, int index, boolean defined) throws Exception {
    //     identifier = identifier.replaceAll("\\&", "&amp;").replaceAll("\\<", "&lt;").replaceAll("\\>", "&gt;").replaceAll("\\\"", "&quot;").replaceAll("\\'", "&apos;");
    //     final String line = String.format("%s<identifier>%s</identifier>", indentation.toString(), identifier);
    //     writer.write(line, 0, line.length());
    //     writer.newLine();
    //     return identifier;
    // }

    // private String writeIdentifier(String identifier, Kind kind, String type, int index, boolean defined) throws Exception {
    //     String line = String.format("%s<identifier", indentation.toString());

    //     line += String.format(" category=\"%s\"", kind.toString());
    //     if (type != null) {
    //         line += String.format(" type=\"%s\"", type);
    //     }
    //     if (index > -1) {
    //         line += String.format(" index=\"%d\"", index);
    //     }
    //     line += String.format(" context=\"%s\"", defined ? "defined" : "used");

    //     identifier = identifier.replaceAll("\\&", "&amp;").replaceAll("\\<", "&lt;").replaceAll("\\>", "&gt;").replaceAll("\\\"", "&quot;").replaceAll("\\'", "&apos;");
    //     line += String.format(">%s</identifier>", identifier);

    //     writer.write(line, 0, line.length());
    //     writer.newLine();
    //     return identifier;
    // }
}