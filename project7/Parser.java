import java.io.*;
import java.util.regex.Pattern;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

class Parser {
    private BufferedReader reader;
    private String nextCommand;
    private String command;
    private boolean hasMoreCommands;
    private CommandType commandType;
    private String arg1;
    private int arg2;

    private static final Pattern regArithmeticLogic = Pattern.compile("^(add|sub|neg|eq|gt|lt|and|or|not)$");
    private static final Pattern regPush = Pattern.compile("^push (((local|argument|this|that|constant|static|temp) ([0-9]|[1-9][0-9]*))|pointer [01])$");
    private static final Pattern regPop = Pattern.compile("^pop (((local|argument|this|that|static|temp) ([0-9]|[1-9][0-9]*))|pointer [01])$");

    Parser(File file) throws Exception {
        commandType = null;
        arg1 = null;
        arg2 = 0;
        reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);
        getNextCommand();
    }

    private void getNextCommand() throws Exception {
        do {
            nextCommand = reader.readLine();
        }
        while (nextCommand != null && (nextCommand.strip() == "" || nextCommand.strip().startsWith("//")));
        hasMoreCommands = nextCommand != null;
    }

    boolean hasMoreCommands() {
        return hasMoreCommands;
    }

    void advance() throws Exception {
        if (hasMoreCommands) {
            command = nextCommand.strip().replaceAll("\\s+", " ").replaceFirst(" ?\\/\\/.*", "");

            // Update state of parser with properties of current command
            if (regArithmeticLogic.matcher(command).matches()) {
                commandType = CommandType.C_ARITHMETIC;
                arg1 = command;
                arg2 = 0;
            }
            else if (regPush.matcher(command).matches()) {
                commandType = CommandType.C_PUSH;
                arg1 = command.split(" ")[1];
                arg2 = Integer.parseInt(command.split(" ")[2]);
            }
            else if (regPop.matcher(command).matches()) {
                commandType = CommandType.C_POP;
                arg1 = command.split(" ")[1];
                arg2 = Integer.parseInt(command.split(" ")[2]);
            }
            else {
                throw new Exception("Invalid command.");
            }
            getNextCommand();
        }
    }

    CommandType getCommandType() {
        return commandType;
    }

    String getArg1() {
        return arg1;
    }

    int getArg2() {
        return arg2;
    }

    void close() throws Exception {
        if (reader != null) 
            reader.close();
    }
}