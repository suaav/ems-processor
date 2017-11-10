import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.parser.Parser;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

class EmsInputParser {
    private static final String INPUT_DAY_DATE_FORMAT = "EEEE, MMMM dd, yyyy";
    private static final String INPUT_TIME_DATE_FORMAT = "h:mm a";
    private static final List<String> AV_DESCRIPTION_HEADINGS =
            Arrays.asList("CMU AV Packages", "AV Requests", "CMU AV Add-On", "Personnel - ");

    private File inputFile;

    EmsInputParser(File inputFile) {
        Preconditions.checkArgument(inputFile != null &&
                        inputFile.exists() && inputFile.getAbsolutePath().endsWith(".html"),
                "Input file must be an existing .html file");
        this.inputFile = inputFile;
    }

    List<Event> parse() {
        List<Event> events = new ArrayList<>();
        try {
            Queue<String> htmlContentList = getCleanData(inputFile);
            events = findEvents(htmlContentList);
        }
        catch (Exception e) {
            System.err.println("Error processing input from from "  + inputFile.getName() + " - " + e.toString());
        }
        return events;
    }

    private static Queue<String> getCleanData(File inputFile) throws IOException{
        Document document = Jsoup.parse(inputFile, null);
        // links and line breaks get removed since they are either empty or not important to this parsing process
        document.getElementsByTag("a").remove();
        document.getElementsByTag("br").remove();

        // get all non-empty data cells
        Elements dataElements = document.getElementsByTag("td");
        dataElements.removeIf(element -> Strings.isNullOrEmpty(element.html()));

        Queue<String> htmlContentList = new LinkedList<>();
        dataElements.forEach(element ->
                htmlContentList.add(
                        // convert HTML entities to Unicode values, trim whitespace,
                        // and convert non-breaking spaces to regular spaces
                        Parser.unescapeEntities(element.html(), false)
                                .replaceAll("\\s+","")
                                .replaceAll("\\u00A0", " ")));

        return htmlContentList;
    }

    private static List<Event> findEvents(Queue<String> dataQueue) throws Exception {
        List<Event> events = new LinkedList<>();
        LocalDate currentDate = null;
        Event currentEvent = new Event();

        String entry = dataQueue.remove();
        while (!dataQueue.isEmpty()) {
//            System.out.println(currentEntry);

            if (isDate(entry)) {
                currentDate = parseDate(entry);
                entry = dataQueue.remove();
            }
            else if (isTime(entry)) {
                assert currentDate != null;
                // get res start time
                LocalTime resStartTime = parseTime(entry);
                currentEvent.setStartTime(currentDate.atTime(resStartTime));

                // ignore event start time
                dataQueue.remove();

                // get event end time
                entry = dataQueue.remove();
                LocalTime eventEndTime = parseTime(entry);
                currentEvent.setEndTime(currentDate.atTime(eventEndTime));

                // ignore res end time
                dataQueue.remove();

                // check if next entry is the arrow (signifying event ends on next day)
                entry = dataQueue.remove();
                if (isEndsOnNextDayArrow(entry))
                {
                    entry = dataQueue.remove();
                    if (isActuallyNextMorning(resStartTime, eventEndTime)) {
                        currentEvent.setEndTime(currentEvent.getEndTime().plusDays(1));
                    }
                }

                // get location
                currentEvent.setLocation(entry);

                // remove unused fields (Setup, Customer, Status, EC, Res ID)
                int entriesToSkip = 5;
                for (int i=0; i < entriesToSkip; i++)
                    dataQueue.remove();

                // get event name
                entry = dataQueue.remove();
                currentEvent.setSubject(entry);

                // remove unused fields until we get to description or next event
                do {
                    entry = dataQueue.remove();
                }
                while (!(isDesc(entry) || isEndOfEvent(entry)));

                // get description
                StringBuilder descriptionBuilder = new StringBuilder();
                do {
                    descriptionBuilder.append(entry).append(" ");
                }
                while (!isEndOfEvent(entry = dataQueue.remove()));
                currentEvent.setDescription(descriptionBuilder.toString());

                events.add(currentEvent);
                currentEvent = new Event();
            }
            // else is a field we don't care about (eg Res ID)
            else
            {
                entry = dataQueue.remove();
            }
        }

        return events;
    }

    // Matches regex for our input date format, matching on any space separator (regular or non-breaking space)
    private static boolean isDate(String entryString) {
        return entryString.matches("((Mon|Tues|Wednes|Thurs|Fri|Satur|Sun)day), " +
                "(January|February|March|April|May|June|July|August|September|October|November|December) " +
                "[0-9]{2}, [0-9]{4}");
    }

    private static LocalDate parseDate(String entryString) throws ParseException {
        return LocalDate.parse(entryString, DateTimeFormatter.ofPattern(INPUT_DAY_DATE_FORMAT));
    }

    // Matches regex for our input time format, matching on any space separator
    private static boolean isTime(String entryString) {
        return entryString.matches("1?[0-9]:[0-9]{2} [AP]M");
    }

    private static LocalTime parseTime(String entryString) throws ParseException {
        return LocalTime.parse(entryString, DateTimeFormatter.ofPattern(INPUT_TIME_DATE_FORMAT));
    }

    // The EMS report displays an arrow indicating that the event ends on the next day.
    // This arrow is simply a '4' in a wingdings-esque font.
    private static boolean isEndsOnNextDayArrow(String entryString) {
        return entryString.equals("4");
    }

    private static boolean isDesc(String entryString) {
        boolean retVal = false;
        for (String heading : AV_DESCRIPTION_HEADINGS)
            // uses startsWith for "Personnel - " to match headings like
            // "Personnel - Client Check In from 9:30 AM to 9:45 PM"
            retVal = retVal || entryString.startsWith(heading);
        return retVal;
    }

    // The footer of each page begins with "EMS Enterprise"
    private static boolean isEndOfPage(String entryString) {
        return entryString.equals("EMS Enterprise") || entryString.equals("34");
    }

    private static boolean isEndOfEvent(String entryString) {
        return isDate(entryString) || isTime(entryString) || isEndOfPage(entryString);
    }

    // The arrow indicating that an event ends on the next day is based on the reservation end time.
    // We want to check if the event we are creating actually ends the next day.
    // Naively checks if the endTime is before the startTime (e.g. 7PM - 12AM would return true).
    // This is not rigorous for events that last longer than 24 hours  (e.g. 5PM - 7PM the next day), but this
    // case is fairly rare.
    private static boolean isActuallyNextMorning(LocalTime startTime, LocalTime endTime) {
        return endTime.compareTo(startTime) < 0;
    }
}