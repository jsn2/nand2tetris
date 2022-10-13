import java.io.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

class JackTokenizer {
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final int EOF = -1;
    private static final char[] SYMBOLS = {'{', '}', '(', ')', '[', ']', '.', ',', ';', '+', '-', '*', '/', '&', '|', '<', '>', '=', '~'};
    
    // Parallel arrays for keywords.
    private static final String[] STR_KEYWORDS = {
        "class", "method", "function", "constructor", "int", "boolean", "char",
        "void", "var", "static", "field", "let", "do", "if", "else",
        "while", "return", "true", "false", "null", "this"
    };
    private static final Keyword[] KEYWORDS = {
        Keyword.CLASS, Keyword.METHOD, Keyword.FUNCTION, Keyword.CONSTRUCTOR, Keyword.INT, Keyword.BOOLEAN, Keyword.CHAR,
        Keyword.VOID, Keyword.VAR, Keyword.STATIC, Keyword.FIELD, Keyword.LET, Keyword.DO, Keyword.IF, Keyword.ELSE,
        Keyword.WHILE, Keyword.RETURN, Keyword.TRUE, Keyword.FALSE, Keyword.NULL, Keyword.THIS
    };

    private String nextToken;
    private TokenType nextTokenType;
    private String currentToken;
    private TokenType currentTokenType;
    private boolean hasMoreTokens;
    private BufferedReader reader;

    JackTokenizer(File file) throws Exception {
        reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);
        getNextToken();
    }

    // Gets the next token from the input stream (after advance() is called, this 'next' token becomes the current token).
    private void getNextToken() throws Exception {
        StringBuilder token = new StringBuilder();
        int c = reader.read();
        int d;

        // Skip comments and whitespace.
        while (c != EOF) {
            // Skip whitespace.
            while (Character.isWhitespace(c))
                c = reader.read();

            // Skip comments.
            reader.mark(2);
            if (c == '/') {
                d = reader.read();
                // Check if start of line comment.
                if (d == '/') {
                    // Skip line.
                    while (c != EOF) {
                        c = reader.read();
                        // Check if reached end of line.
                        if (c == LINE_SEPARATOR.charAt(0)) {
                            int i = 0;
                            do {
                                c = reader.read();
                                i++;
                            }
                            while (i < LINE_SEPARATOR.length() && c == LINE_SEPARATOR.charAt(i));
                            if (i == LINE_SEPARATOR.length())
                                break;
                        }
                    }
                }
                // Check if start of block comment.
                else if (d == '*') {
                    while (c != EOF) {
                        c = reader.read();
                        // Check if reached end of block comment.
                        if (c == '*') {
                            c = reader.read();
                            if (c == '/') {
                                c = reader.read();
                                break;
                            }
                        }
                    }
                }
                else {
                    reader.reset();
                    break;
                }
            }
            else {
                break;
            }
        }

        // Will be set to false only if end of file is found below.
        hasMoreTokens = true;

        // If EOF then return.
        if (c == EOF) {
            nextToken = null;
            nextTokenType = null;
            hasMoreTokens = false;
            return;
        }

        // If token is a symbol then return.
        for (char symbol : SYMBOLS) {
            if (c == symbol) {
                nextToken = Character.toString(symbol);
                nextTokenType = TokenType.SYMBOL;
                return;
            }  
        }

        // If token is number then return.
        if (Character.isDigit(c)) {
            while (Character.isDigit(c)) {
                token.append((char)c);
                reader.mark(2);
                c = reader.read();
            }
            reader.reset();
            int number = Integer.parseInt(token.toString());
            if (number >= 0 && number <= 32767) {
                nextToken = token.toString();
                nextTokenType = TokenType.INT_CONST;
            }
            else {
                throw new Exception("Expected integer constant between 0 and 32767.");
            }
            return;
        }

        // If token is string then return.
        if (c == '"') {
            do {
                c = reader.read();

                // Check if reached end of line.
                if (c == LINE_SEPARATOR.charAt(0)) {
                    int i = 0;
                    while (c == LINE_SEPARATOR.charAt(i)) {
                        token.append((char)c);
                        i++;
                        if (i == LINE_SEPARATOR.length())
                            throw new Exception("Expected '\"' but found new line.");
                        else
                            c = reader.read();
                    }
                }

                // Check if reached end of file.
                if (c == EOF) 
                    throw new Exception("Expected '\"' but encountered end of file.");

                if (c != '"')
                    token.append((char)c);

            }
            while (c != '"');
            nextToken = token.toString();
            nextTokenType = TokenType.STRING_CONST;
            return;
        }

        // If token is keyword or idetifier then return.
        if (Character.isLetter(c) || c == '_') {
            while (Character.isLetter(c) || Character.isDigit(c) || c == '_') {
                token.append((char)c);
                reader.mark(2);
                c = reader.read();
            }
            reader.reset();
            // If token is keyword then return.
            for (String keyword : STR_KEYWORDS) {
                if (token.toString().equals(keyword)) {
                    nextToken = token.toString();
                    nextTokenType = TokenType.KEYWORD;
                    return;
                }
            }
            // Token is identifier so return.
            nextToken = token.toString();
            nextTokenType = TokenType.IDENTIFIER;
            return;
        }

        throw new Exception("Invalid token encountered.");
    }

    // Sets the next token as the current token and then gets the next token from the input stream.
    void advance() throws Exception {
        currentToken = nextToken;
        currentTokenType = nextTokenType;
        getNextToken();
    }

    // Returns true if there are more tokens to process.
    boolean hasMoreTokens() {
        return hasMoreTokens;
    }

    // Returns the type of the current token.
    TokenType tokenType() {
        return currentTokenType;
    }

    // Returns the keyword if the current token has the TokenType of KEYWORD.
    Keyword keyword() {
        Keyword keyword = null;
        for (int i = 0; i < STR_KEYWORDS.length; i++) {
            if (currentToken.equals(STR_KEYWORDS[i])) {
                keyword = KEYWORDS[i];
                break;
            }
        }
        return keyword;
    }

    // Returns the symbol if the current token has the TokenType of SYMBOL.
    char symbol() {
        return currentToken.charAt(0);
    }

    // Returns the identifier if the current token has the TokenType of IDENTIFIER.
    String identifier() {
        return currentToken;
    }

    // Returns the integer value if the current token has the TokenType of INT_CONST.
    int intVal() {
        return Integer.parseInt(currentToken);
    }

    // Returns the string value if the current token has the TokenType of STRING_CONST.
    String stringVal() {
        return currentToken;
    }

    void close() throws Exception {
        if (reader != null) 
            reader.close();
    }
}