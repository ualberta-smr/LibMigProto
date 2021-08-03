import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;
import org.apache.commons.csv.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

public class CsvMigration {
    String csv = "Taqee;\"Jan 1, 2020\"";

    public List<String> readFirstRow() throws IOException {
        var builder = new CsvPreference.Builder('"', ';', "\n");

        var preferences = builder.build();

        var beanReader = new CsvListReader(new StringReader(csv), preferences);
        var records = beanReader.read();
        return records;
    }
}