package at.bitfire.davdroid.resource;

/**
 * Created by jonatan on 18/11/15.
 */
public class EncryptedEvent extends Event {
    private final static String TAG = "davdroid.EncryptedEvent";




    public EncryptedEvent(String name, String ETag,byte[] key) {
        super(name, ETag);
        this.key = key ;
    }

    public EncryptedEvent(long localID, String name, String ETag, byte[] key) {
        super(localID, name, ETag);
        this.key = key;
    }



}
