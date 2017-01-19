package crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Usefull utils used by the crypto package.
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

    /**
     * Rename a given file path to a new extension.
     * @param source the source file path
     * @param newExtension the new extension
     * @return the new file path
     */
    public static String renameFileExtensionString (String source, String newExtension) {
        String target;
        String currentExtension = getFileExtension(source);

        if (currentExtension.equals("")){
            target = source + "." + newExtension;
        }
        else {
            target = source.replaceFirst(Pattern.quote("." +
                    currentExtension) + "$", Matcher.quoteReplacement("." + newExtension));
        }
        return target;
    }

    /**
     * Return a file extension
     * @param f the file path
     * @return the file extension
     */
    public static String getFileExtension(String f) {
        String ext = "";
        int i = f.lastIndexOf('.');
        if (i > 0 &&  i < f.length() - 1) {
            ext = f.substring(i + 1).toLowerCase();
        }
        return ext;
    }
}
