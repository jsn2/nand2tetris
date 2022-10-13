import java.io.*;
import java.nio.file.Files;

class CompilationEngine {

    private JackTokenizer tokenizer;
    private BufferedWriter writer;

    CompilationEngine(JackTokenizer tokenizer, BufferedWriter writer) throws Exception {
        this.tokenizer = tokenizer;
        this.writer = writer;
    }

    void writeAllTokens() throws Exception {
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