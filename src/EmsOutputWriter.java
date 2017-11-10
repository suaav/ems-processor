import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class EmsOutputWriter {
    private static final String CSV_HEADER = "Subject,Start date,Start time,End date,End time,Description,Location";
    private static final String DAY_DATE_FORMAT = "MM/dd/yyyy";
    private static final String TIME_DATE_FORMAT = "h:mm a";
    private File outputFile;

    EmsOutputWriter(File outputFile) {
        Preconditions.checkArgument(outputFile != null && outputFile.getAbsolutePath().endsWith(".csv"),
                "Output file must be a .csv file");
        this.outputFile = outputFile;
    }

    void writeHeader() {
        writeToFile(CSV_HEADER, StandardOpenOption.WRITE);
    }
    
    void write(Event event) {
        writeToFile(toCsvFormat(event), StandardOpenOption.APPEND);
    }
    
    private void writeToFile(String stringToWrite, StandardOpenOption writeOption) {
        try {
            Files.write(outputFile.toPath(), (stringToWrite+"\n").getBytes(), writeOption);
        }
        catch (IOException e) {
            System.err.println("Could not write to " + outputFile.getName() + " - " + e.toString());
        }
    }

    private static String toCsvFormat(Event event) {
        List<String> rawFields = Arrays.asList(
                event.getSubject(),
                getDay(event.getStartTime()),
                getTime(event.getStartTime()),
                getDay(event.getEndTime()),
                getTime(event.getEndTime()),
                event.getDescription(),
                event.getLocation());

        // TODO: Looks horrible - fix?
        List<String> cleanFields = rawFields.stream().map(EmsOutputWriter::clean).collect(Collectors.toList());

        return Joiner.on(",").join(cleanFields);
    }

    /**
     * Clean subject and description so they can safely be output to CSV
     */
    private static String clean(String field) {
        // double quotes can only be used to wrap an entire field, so they need to be replaced
        String cleanField = field.replaceAll("\"", "''");
        return "\"" + cleanField + "\"";
    }

    private static String getDay(LocalDateTime date) {
        return date.toLocalDate().format(DateTimeFormatter.ofPattern(DAY_DATE_FORMAT));
    }

    private static String getTime(LocalDateTime date) {
        return date.toLocalTime().format(DateTimeFormatter.ofPattern(TIME_DATE_FORMAT));
    }
}
