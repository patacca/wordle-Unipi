package edu.riccardomori.wordle.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import com.google.gson.stream.JsonReader;
import edu.riccardomori.wordle.utils.LRUCache;

// Simple tanslation server that internally uses a LRU cache. This is a singleton class.
// It is thread-safe.
public class TranslationServer {
    private static TranslationServer instance; // Singleton instance

    private static final int TRANSLATION_CACHE = 512; // The LRU cache size for translations

    // Cache holding the translations of the words
    private LRUCache<String, String> translationCache;
    private Logger logger;

    private TranslationServer() {
        this.logger = Logger.getLogger("Wordle");
        this.translationCache = new LRUCache<String, String>(TranslationServer.TRANSLATION_CACHE);
    }

    /**
     * Get the singleton object
     * 
     * @return The singleton instance
     */
    public static TranslationServer getInstance() {
        if (TranslationServer.instance == null)
            TranslationServer.instance = new TranslationServer();

        return TranslationServer.instance;
    }

    /**
     * Returns the italian translation of a word
     * 
     * @param word The word to be translated
     * @return the italian transaltion of {@code word}. In case of an error the empty string is
     *         returned
     */
    public String get(String word) {
        // Cache lookup first
        synchronized (this.translationCache) {
            if (this.translationCache.containsKey(word))
                return this.translationCache.get(word);
        }

        // HTTP request to mymemory
        try {
            URL url = new URL(String
                    .format("https://api.mymemory.translated.net/get?q=%s&langpair=en|it", word));
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

            // TODO consider setting some headers

            // Parse json
            try (JsonReader reader = new JsonReader(
                    new BufferedReader(new InputStreamReader(connection.getInputStream())))) {

                String translation = null;

                // @formatter:off
                // The message has this format:
                //   {"responseData": {"translatedText": String, ...}, ...}
                // @formatter:on
                reader.beginObject();
                while (reader.hasNext()) { // Whole response object
                    String name = reader.nextName();

                    if (name.equals("responseData")) {
                        reader.beginObject();
                        while (reader.hasNext()) { // Whole responseData object
                            name = reader.nextName();
                            if (name.equals("translatedText"))
                                translation = reader.nextString();
                            else
                                reader.skipValue();
                        }
                        reader.endObject();
                    } else // Ignore anything else
                        reader.skipValue();
                }
                reader.endObject();

                if (translation == null) {
                    this.logger.severe("Cannot parse the json response from the mymemory server");
                    return "";
                }

                synchronized (this.translationCache) {
                    this.translationCache.put(word, translation);
                }
                return translation;
            } catch (IOException e) {
                e.printStackTrace();
                return "";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}
