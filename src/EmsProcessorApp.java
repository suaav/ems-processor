import com.google.common.base.Strings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class EmsProcessorApp {

    public static void main(String[] args) {
        // ~~~ COMMAND INPUT CHECKING ~~~
        if (args.length == 0 || isHelpRequest(args[0])) {
            System.out.println("usage: java EmsProcessorApp inputFileName outputFileName");
            return;
        }

        File inputFile = checkInputFile(args[0]);
        File outputFile = checkOutputFile(args[1]);
        
        if (inputFile == null || outputFile == null) {
            // invalid files
            return;
        }

        // ~~~ INPUT READING ~~~
        EmsInputParser inputParser = new EmsInputParser(inputFile);
        List<Event> events = inputParser.parse();

        // ~~~ OUTPUT WRITING ~~~
        try {
            EmsOutputWriter outputWriter = new EmsOutputWriter(outputFile);
            outputWriter.writeHeader();
            events.forEach(outputWriter::write);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }

        System.out.println("Parsed " + events.size() + " events to " + outputFile.getName());
    }
    
    private static File checkInputFile(String filePath) {
        File file = null;
        if (!Strings.isNullOrEmpty(filePath) && filePath.endsWith(".html")) {
            file = new File(filePath);
            if (!file.exists()) {
                file = null;
            }
        }

        if (file == null)
        {
            System.out.println("Input must be a valid .html file");
            System.out.println("usage: java EmsProcessorApp inputFileName outputFileName");
        }
        return file;
    }

    private static File checkOutputFile(String filePath) {
        File file = null;
        if (Strings.isNullOrEmpty(filePath) || !filePath.endsWith(".csv")) {
            System.out.println("Output must be a valid .csv file");
            System.out.println("usage: java EmsProcessorApp inputFileName outputFileName");
        }
        else {
            file = new File(filePath);
            // TODO: Having problems overwriting file if it exists - trying to delete it first doesn't seem clean
            try {
                Files.delete(file.toPath());
            }
            catch (IOException e) {
                // file does not exist yet
            }
            try {
                Files.createFile(file.toPath());
            }
            catch (IOException e) {
                System.err.println("Could not create output file" + file.getName());
            }
        }
        return file;
    }

    private static boolean isHelpRequest(String argument) {
        return  argument.equalsIgnoreCase("--h") ||
                argument.equalsIgnoreCase("-h") ||
                argument.equalsIgnoreCase("--help") ||
                argument.equalsIgnoreCase("-help");
    }
}
