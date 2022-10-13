import java.io.*;

class VMTranslator {
    public static void main(String args[]) {
        if (args.length != 1) {
            System.out.println("Usage: java VMTranslator filename");
            System.exit(1);
        }

        Parser parser = null;
        CodeWriter codeWriter = null;

        try {
            // Check if filename has .vm extension
            if (!args[0].endsWith(".vm"))
                throw new Exception("File must have '.vm' extension.");

            // Check if file exists
            File file = new File(args[0]);
            if (!file.exists() || file.isDirectory())
                throw new Exception("File '" + args[0] + "' not found.");
            
            parser = new Parser(file);
            codeWriter = new CodeWriter(file.getPath().substring(0, file.getPath().lastIndexOf('.')) + ".asm");

            // Start translating
            for (int i = 1; parser.hasMoreCommands(); i++) {
                // Move to next command 
                parser.advance();

                // Write translation of command to output file
                switch (parser.getCommandType()) {
                    case C_ARITHMETIC:
                        codeWriter.writeArithmetic(parser.getArg1());
                        break;
                    case C_PUSH:
                    case C_POP:
                        codeWriter.writePushPop(parser.getCommandType(), parser.getArg1(), parser.getArg2());
                        break;
                    default:
                        throw new Exception("Invalid command type.");
                }
            }
        }
        catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        finally {
            if (parser != null)
                try {
                    parser.close();
                }
                catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    e.printStackTrace();
                }
            if (codeWriter != null)
                try {
                    codeWriter.close();
                }
                catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    e.printStackTrace();
                }
        }
    }
}