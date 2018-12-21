import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.TreeMap;

public class ErrorHandler implements Constants {
    private static final String CNF_NAME = "/error_config.csv";
    private static Map<Integer, String> configMap = new TreeMap<>();

    private static ErrorHandler instance;

    static {
        InputStream configInput = null;
        try {
            configInput = ErrorHandler.class.getResourceAsStream(CNF_NAME);
            BufferedReader reader = new BufferedReader(new InputStreamReader(configInput));
            String line;
            while ((line  = reader.readLine()) != null ) {
                String[] cnfArgs = line.split(":");
                configMap.put(Integer.valueOf(cnfArgs[0]), cnfArgs[1]);
            }
        }
        catch (IOException exception) {
            exception.printStackTrace();
        }
        finally {
            if(configInput != null) {
                try {
                    configInput.close();
                }
                catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    public ErrorHandler() {}

    public static ErrorHandler getInstance() {
        if (instance == null) {
            synchronized (ErrorHandler.class) {
                if (instance == null) {
                    instance = new ErrorHandler();
                }
            }
        }
        return instance;
    }

    public String getError(Integer errorCode) {
        return configMap.get(errorCode);
    }

    public String isValidPort(Integer port) {
        if (port > MIN_PORT_NUM && port < MAX_PORT_NUM) {
            return "Success";
        }
        else {
            return configMap.get(2);
        }
    }

    public String resolveHostName(String host) {
        try {
            InetAddress hostAddress = InetAddress.getByName(host);
            return "Success";
        } catch (UnknownHostException e) {
//            e.printStackTrace();
            return configMap.get(3);
        }
    }
}
