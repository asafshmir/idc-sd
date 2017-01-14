package crypto;

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

    /**
     * Convert a byte array into its hex deump representation
     * @param array The byte[] to hex dump
     * @return String
     */
    public static String getHexDump(byte[] array){
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] hexChars = new char[array.length * 2];
        int v;
        for ( int j = 0; j < array.length; j++ ) {
            v = array[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
