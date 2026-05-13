import java.io.*;
import java.net.*;
import java.util.*;

public class MapReducer {
    public static void main(String[] args) {
        try (
            Socket socket = new Socket("localhost", 5050);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner scanner = new Scanner(System.in)
        ) {
            System.out.println("🔌 Συνδέθηκε με Master!");

            System.out.print("Δώσε όρο αναζήτησης (π.χ. pizza): ");
            String query = scanner.nextLine();

            out.println("SEARCH " + query);  // στέλνει εντολή στον Master
            String response = in.readLine();

            if (response == null || response.equals("NO_RESULTS")) {
                System.out.println("❌ Δεν βρέθηκαν αποτελέσματα.");
            } else {
                System.out.println("✅ Αποτελέσματα:");
                System.out.println(response.replace(";", "\n"));
            }

        } catch (IOException e) {
            System.err.println("❌ Αποτυχία σύνδεσης με τον Master: " + e.getMessage());
        }
    }
}
