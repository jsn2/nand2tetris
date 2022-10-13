import java.io.*;
import java.nio.file.Files;

class CompilationEngine {

    private JackTokenizer tokenizer;
    private BufferedWriter writer;
    private StringBuilder indentation;

    CompilationEngine(JackTokenizer tokenizer, BufferedWriter writer) throws Exception {
        this.tokenizer = tokenizer;
        this.writer = writer;
        this.indentation = new StringBuilder();
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

        eat("class");
        eat(TokenType.IDENTIFIER);
        eat("{");

        while (tokenizer.tokenType() == TokenType.KEYWORD && (tokenizer.keyword() == Keyword.STATIC || tokenizer.keyword() == Keyword.FIELD)) {
            compileClassVarDec();
        }

        while (tokenizer.tokenType() == TokenType.KEYWORD && (tokenizer.keyword() == Keyword.CONSTRUCTOR || tokenizer.keyword() == Keyword.METHOD || tokenizer.keyword() == Keyword.FUNCTION)) {
            compileSubroutineDec();
        }

        eat("}");

        indentation.delete(0, 4);
        writeCloseTag(TAG);
    }

    /* classVarDec: ('static' | 'field') type varName (',' varName)* ';' */
    private void compileClassVarDec() throws Exception {
        final String TAG = "classVarDec";

        writeOpenTag(TAG);
        indentation.append("    ");

        try { eat("static"); }
        catch (Exception e) {
            eat("field");
        }

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

        eat(TokenType.IDENTIFIER);

        while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
            eat(",");
            eat(TokenType.IDENTIFIER);
        }

        eat(";");

        indentation.delete(0, 4);
        writeCloseTag(TAG);
    }

    /* subroutineDec: ('constructor' | 'function' | 'method') ('void' | type) subroutineName '(' parameterList ')' subroutineBody */
    private void compileSubroutineDec() throws Exception {
        final String TAG = "subroutineDec";

        writeOpenTag(TAG);
        indentation.append("    ");

        try { eat("constructor"); }
        catch (Exception e) {
            try { eat("function"); }
            catch (Exception e1) {
                eat("method");
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

        eat(TokenType.IDENTIFIER);
        eat("(");
        compileParameterList();
        eat(")");
        compileSubroutineBody();

        indentation.delete(0, 4);
        writeCloseTag(TAG);
    }

    /* parameterList: ((type varName) (',' type varName)*)? */
    private void compileParameterList() throws Exception {
        final String TAG = "parameterList";

        writeOpenTag(TAG);
        indentation.append("    ");

        if (tokenizer.tokenType() == TokenType.IDENTIFIER || (tokenizer.tokenType() == TokenType.KEYWORD && (tokenizer.keyword() == Keyword.INT || tokenizer.keyword() == Keyword.BOOLEAN || tokenizer.keyword() == Keyword.CHAR))) {
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
            eat(TokenType.IDENTIFIER);
            while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
                eat(",");
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
                eat(TokenType.IDENTIFIER);
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

        eat("{");
        while (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == Keyword.VAR) {
            compileVarDec();
        }
        compileStatements();
        eat("}");

        indentation.delete(0, 4);
        writeCloseTag(TAG);
    }

    /* varDec: 'var' type varName (',' varName)* ';' */
    private void compileVarDec() throws Exception {
        final String TAG = "varDec";

        writeOpenTag(TAG);
        indentation.append("    ");

        eat("var");
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
        eat(TokenType.IDENTIFIER);
        while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
            eat(",");
            eat(TokenType.IDENTIFIER);
        }
        eat(";");

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

        eat("let");
        eat(TokenType.IDENTIFIER);
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '[') {
            eat("[");
            compileExpression();
            eat("]");
        }
        eat("=");
        compileExpression();
        eat(";");

        indentation.delete(0, 4);
        writeCloseTag(TAG);
    }

    /* ifStatement: 'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}')? */
    private void compileIf() throws Exception {
        final String TAG = "ifStatement";

        writeOpenTag(TAG);
        indentation.append("    ");

        eat("if");
        eat("(");
        compileExpression();
        eat(")");
        eat("{");
        compileStatements();
        eat("}");
        if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == Keyword.ELSE) {
            eat("else");
            eat("{");
            compileStatements();
            eat("}");
        }

        indentation.delete(0, 4);
        writeCloseTag(TAG);
    }

    /* whileStatement: 'while' '(' expression ')' '{' statements '}' */
    private void compileWhile() throws Exception {
        final String TAG = "whileStatement";

        writeOpenTag(TAG);
        indentation.append("    ");

        eat("while");
        eat("(");
        compileExpression();
        eat(")");
        eat("{");
        compileStatements();
        eat("}");

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

        eat("do");

        eat(TokenType.IDENTIFIER);
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '(') {
            eat("(");
            compileExpressionList();
            eat(")");
        }
        else {
            eat(".");
            eat(TokenType.IDENTIFIER);
            eat("(");
            compileExpressionList();
            eat(")");
        }
        
        eat(";");

        indentation.delete(0, 4);
        writeCloseTag(TAG);
    }

    /* returnStatement: 'return' expression? ';' */
    private void compileReturn() throws Exception {
        final String TAG = "returnStatement";

        writeOpenTag(TAG);
        indentation.append("    ");

        eat("return");
        if (!(tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ';')) {
            compileExpression();
        }
        eat(";");

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
                        eat(tokenizer.stringVal());
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
            eat(TokenType.INT_CONST);
        }
        else if (tokenizer.tokenType() == TokenType.STRING_CONST) {
            eat(TokenType.STRING_CONST);
        }
        else if (tokenizer.tokenType() == TokenType.KEYWORD) {
            eat(TokenType.KEYWORD);
        }
        else if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '(') {
            eat("(");
            compileExpression();
            eat(")");
        }
        else if (tokenizer.tokenType() == TokenType.SYMBOL && (tokenizer.symbol() == '-' || tokenizer.symbol() == '~')) {
            eat(tokenizer.stringVal());
            compileTerm();
        }
        else if (tokenizer.tokenType() == TokenType.IDENTIFIER) {
            eat(TokenType.IDENTIFIER);
            if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '[') {
                eat("[");
                compileExpression();
                eat("]");
            }
            else if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '(') {
                eat("(");
                compileExpressionList();
                eat(")");
            }
            else if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '.') {
                eat(".");
                eat(TokenType.IDENTIFIER);
                eat("(");
                compileExpressionList();
                eat(")");
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
                    eat(",");
                    compileExpression();
                }
                break;
        }

        indentation.delete(0, 4);
        writeCloseTag(TAG);
    }

    private void eat(String token) throws Exception {
        if (tokenizer.stringVal().equals(token)) {
            writeToken();
            if (tokenizer.hasMoreTokens()) {
                tokenizer.advance();
            }
        }
        else {
            throw new Exception(String.format("Expected token %s but encountered %s.", token.toString(), tokenizer.stringVal()));
        }
    }

    private void eat(TokenType tokenType) throws Exception {
        if (tokenizer.tokenType() == tokenType) {
            writeToken();
            if (tokenizer.hasMoreTokens()) {
                tokenizer.advance();
            }
        }
        else {
            throw new Exception(String.format("Expected token of type %s but encountered %s.", tokenType.toString(), tokenizer.stringVal()));
        }
    }

    private void writeToken() throws Exception {
        final String KEYWORD_OPEN_TAG = "<keyword>";
        final String SYMBOL_OPEN_TAG = "<symbol>";
        final String IDENTIFIER_OPEN_TAG = "<identifier>";
        final String INT_CONST__OPEN_TAG = "<integerConstant>";
        final String STRING_CONST_OPEN_TAG = "<stringConstant>";
        String line = indentation.toString();

        switch (tokenizer.tokenType()) {
            case KEYWORD:
                line += KEYWORD_OPEN_TAG + tokenizer.stringVal() + KEYWORD_OPEN_TAG.replaceFirst("\\<", "</");
                break;
            case IDENTIFIER:
                line += IDENTIFIER_OPEN_TAG + tokenizer.stringVal() + IDENTIFIER_OPEN_TAG.replaceFirst("\\<", "</");
                break;
            case INT_CONST:
                line += INT_CONST__OPEN_TAG + tokenizer.stringVal() + INT_CONST__OPEN_TAG.replaceFirst("\\<", "</");
                break;
            case STRING_CONST:
                line += STRING_CONST_OPEN_TAG + tokenizer.stringVal() + STRING_CONST_OPEN_TAG.replaceFirst("\\<", "</");
                break;
            case SYMBOL:
                line += SYMBOL_OPEN_TAG;
                switch (tokenizer.symbol()) {
                    case '<':
                        line += "&lt;";
                        break;
                    case '>':
                        line += "&gt;";
                        break;
                    case '"':
                        line += "&quot;";
                        break;
                    case '&':
                        line += "&amp;";
                        break;
                    default:
                        line += tokenizer.stringVal();
                        break;
                }
                line += SYMBOL_OPEN_TAG.replaceFirst("\\<", "</");
                break;
        }
        writer.write(line, 0, line.length());
        writer.newLine();
    }

    private void writeAllTokens() throws Exception {
        final String ROOT_OPEN_TAG = "<tokens>";
        final String KEYWORD_OPEN_TAG = "<keyword>";
        final String SYMBOL_OPEN_TAG = "<symbol>";
        final String IDENTIFIER_OPEN_TAG = "<identifier>";
        final String INT_CONST__OPEN_TAG = "<integerConstant>";
        final String STRING_CONST_OPEN_TAG = "<stringConstant>";

        String line;

        writer.write(ROOT_OPEN_TAG, 0, ROOT_OPEN_TAG.length());
        writer.newLine();
        while (tokenizer.hasMoreTokens()) {
            line = "    ";
            tokenizer.advance();
            switch (tokenizer.tokenType()) {
                case KEYWORD:
                    line += KEYWORD_OPEN_TAG + tokenizer.stringVal() + KEYWORD_OPEN_TAG.replaceFirst("\\<", "</");
                    break;
                case SYMBOL:
                    line += SYMBOL_OPEN_TAG;
                    switch (tokenizer.symbol()) {
                        case '<':
                            line += "&lt;";
                            break;
                        case '>':
                            line += "&gt;";
                            break;
                        case '"':
                            line += "&quot;";
                            break;
                        case '&':
                            line += "&amp;";
                            break;
                        default:
                            line += tokenizer.symbol();
                            break;
                    }
                    line += SYMBOL_OPEN_TAG.replaceFirst("\\<", "</");
                    break;
                case IDENTIFIER:
                    line += IDENTIFIER_OPEN_TAG + tokenizer.stringVal() + IDENTIFIER_OPEN_TAG.replaceFirst("\\<", "</");
                    break;
                case INT_CONST:
                    line += INT_CONST__OPEN_TAG + tokenizer.stringVal() + INT_CONST__OPEN_TAG.replaceFirst("\\<", "</");
                    break;
                case STRING_CONST:
                    line += STRING_CONST_OPEN_TAG + tokenizer.stringVal() + STRING_CONST_OPEN_TAG.replaceFirst("\\<", "</");
                    break;
            }
            writer.write(line, 0, line.length());
            writer.newLine();
        }
        writer.write(ROOT_OPEN_TAG.replaceFirst("\\<", "</"), 0, ROOT_OPEN_TAG.replaceFirst("\\<", "</").length());
        writer.newLine();
    }
}