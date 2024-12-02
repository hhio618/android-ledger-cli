package net.ilammy.ledger.api;

import org.junit.Test;

import static net.ilammy.ledger.api.Assert.assertThrows;
import static org.junit.Assert.assertNotNull;

public class TestGlobal {
    @Test
    public void runCommand_Success() {
        final String journal = "" +
                "2020-12-17 * Valid transaction\n" +
                "    Assets:Testing       $1000\n" +
                "    Equity\n";
        // Get the application context

        // Create a temporary file in the app's cache directory
        File tempFile = File.createTempFile("test_file", ".txt", "tmp");

        // Write some content to the temporary file
        FileWriter writer = new FileWriter(tempFile);
        writer.write(journal);
        writer.close();

        try (Global global = new Global()) {
            String out = global.runCommand("-f " + tempFile.Path() + " balance");
            assertNotNull(out);
        }
    }

}
