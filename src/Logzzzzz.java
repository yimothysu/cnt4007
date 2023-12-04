import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.io.IOException;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Handler;


public class Logzzzzz {
    private static Logger logger = Logger.getLogger(Logzzzzz.class.getName());

    public static class OneLineFormatter extends Formatter {
        private static final String PATTERN = "yyyy-MM-dd HH:mm:ss";

        @Override
        public String format(LogRecord record) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(PATTERN);
            String formattedDate = simpleDateFormat.format(new Date(record.getMillis()));
            return String.format("%s %s %s%n", formattedDate, record.getLevel(), formatMessage(record));
        }
    }


    public static void log(String message) {
        logger.info(message);
    }

    public static void initWithPeerId(String peerId) {
        logger = Logger.getLogger("Logger");
        FileHandler fh;

        try {
            // Remove all existing handlers
            Handler[] handlers = logger.getHandlers();
            for (Handler handler : handlers) {
                logger.removeHandler(handler);
            }

            // Set to false to prevent using parent handlers
            logger.setUseParentHandlers(false);

            fh = new FileHandler("../log_peer_" + peerId + ".log");
            logger.addHandler(fh);
            fh.setFormatter(new OneLineFormatter());
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
    }

}
