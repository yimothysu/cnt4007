import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.io.IOException;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logzzzzz {
    private static Logger logger = Logger.getLogger(Logzzzzz.class.getName());

    public static class OneLineFormatter extends Formatter {
        private static final String PATTERN = "yyyy-MM-dd HH:mm:ss";

        @Override
        public String format(LogRecord record) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(PATTERN);
            String formattedDate = simpleDateFormat.format(new Date(record.getMillis()));
            return String.format("%s: %s%n", formattedDate, formatMessage(record));
        }
    }

    public static void log(String message) {
        logger.info(message);
//        try {
//            Thread.sleep(10); // 100 milliseconds = 0.1 seconds
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//            // Handle the interruption appropriately
//        }
    }

    public static void initWithPeerId(String peerId) {
        logger = Logger.getLogger("Logzzzzz");
        FileHandler fh;
        try {
            fh = new FileHandler("../log_peer_" + peerId + ".log");
            logger.addHandler(fh);
            fh.setFormatter(new OneLineFormatter());
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
    }
}
