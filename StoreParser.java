import com.google.gson.Gson;
import java.io.FileReader;

public class StoreParser {
    public static Store parse(String filePath) {
        try {
            Gson gson = new Gson();
            return gson.fromJson(new FileReader(filePath), Store.class);
        } catch (Exception e) {
            System.out.println("❌ Σφάλμα κατά την ανάγνωση του αρχείου JSON.");
            e.printStackTrace();
            return null;
        }
    }
}
