package crypt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by shmir on 1/14/2017.
 */
public class Utils {

    /**
     * Read all the bytes of a given
     * @param path The path to the file to read
     * @return the read data bytes
     * @throws IOException on any read problem
     */
    public static byte[] readFile(String path) throws IOException {
        File readFrom = new File(path);
        byte[] data = new byte[(int)readFrom.length()];
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(readFrom);
            fis.read(data);
            return data;
        } finally {
            if(fis != null) {
                fis.close();
            }
        }
    }
}
