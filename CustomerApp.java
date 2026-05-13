
import java.io.*;
import java.net.Socket;
import java.util.*;
import com.google.gson.Gson;


public class CustomerApp {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in); 
        scanner.useLocale(Locale.US);  // Ρύθμιση locale για να επιτρέψει σωστή χρήση δεκαδικών αριθμών
       

        System.out.println("===== CUSTOMER APP =====");
        while (true) {
        System.out.println("\n===== MENU =====");
        System.out.println("1. Αναζήτηση καταστημάτων με φίλτρα");
        System.out.println("2. Αγορά προϊόντος");
        System.out.println("3. Βαθμολόγηση καταστήματος");
        System.out.println("0. Έξοδος");
        System.out.print("Επιλογή: ");
    
        int choice = scanner.nextInt();
        scanner.nextLine(); // consume newline
    
        switch (choice) {
            case 1:
                searchStores(scanner);
                break;
            case 2:
                makePurchase(scanner);
                break;
            case 3:
                rateStore(scanner);  // θα το προσθέσουμε στο επόμενο βήμα
                break;
            case 0:
                System.out.println("Έξοδος...");
                return;
            default:
                System.out.println("Άκυρη επιλογή.");
        }
    }
}  


public static void searchStores(Scanner scanner) {
    double latitude = 37.9838;
    double longitude = 23.7275;

    System.out.print("Κατηγορία φαγητού (π.χ. pizza): ");
    String category = scanner.nextLine();

    int minStars;
    do {
        System.out.print("Ελάχιστη βαθμολογία (1–5): ");
        while (!scanner.hasNextInt()) {
            System.out.print("Μη έγκυρος αριθμός. Ξαναπροσπάθησε: ");
            scanner.next();
        }
        minStars = scanner.nextInt();
        scanner.nextLine();
    } while (minStars < 1 || minStars > 5);

    System.out.print("Τιμές (χώρισε με κόμμα, π.χ. $, $$): ");
    String[] prices = scanner.nextLine().split(",");

    StringBuilder json = new StringBuilder();
    json.append("{");
    json.append("\"latitude\":").append(latitude).append(",");
    json.append("\"longitude\":").append(longitude).append(",");
    json.append("\"category\":\"").append(category).append("\",");
    json.append("\"minStars\":").append(minStars).append(",");
    json.append("\"priceLevels\":[");
    for (int i = 0; i < prices.length; i++) {
        json.append("\"").append(prices[i].trim()).append("\"");
        if (i < prices.length - 1) json.append(",");
    }
    json.append("]}");

    try (Socket socket = new Socket("localhost", 5050)) {
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println("SEARCH " + json.toString());

        String response = in.readLine();

        if (response == null || response.equals("NO_RESULTS")) {
            System.out.println("❌ Δεν βρέθηκαν καταστήματα με αυτά τα φίλτρα.");
        } else {
            Gson gson = new Gson();
            String[] storeJsons = response.split(";");

            for (String storeJson : storeJsons) {
                Store store = gson.fromJson(storeJson, Store.class);

                System.out.println("\n Kατάστημα: " + store.StoreName);
                System.out.println("Αξιολόγηση: " + store.Stars + " (" + store.NoOfVotes + " ψήφοι)");
                System.out.println("Τοποθεσία: " + store.Latitude + ", " + store.Longitude);
                System.out.println("Κατηγορία: " + store.FoodCategory);
                System.out.println("Τιμή: " + store.PriceCategory);
                System.out.println("Προϊόντα:");
                for (Product p : store.Products) {
                    if (!p.hidden) {
                        System.out.println("- " + p.ProductName + " (" + p.Available_Amount + ") - " + p.Price + "€");
                    }
                }
            }
        }

    } catch (IOException e) {
        System.out.println("Σφάλμα σύνδεσης με τον Master.");
        e.printStackTrace();
    }
}


    public static void makePurchase(Scanner scanner) {
        System.out.print("Δώσε όνομα καταστήματος για αγορά προϊόντος: ");
        String storeName = scanner.nextLine();
        
        System.out.print("Δώσε όνομα προϊόντος: ");
        String productName = scanner.nextLine();

        int quantity;
        do {
            System.out.print("Δώσε ποσότητα για αγορά: ");
            while (!scanner.hasNextInt()) {
                System.out.print("Μη έγκυρος αριθμός. Ξαναπροσπάθησε: ");
                scanner.next();
            }
            quantity = scanner.nextInt();
            scanner.nextLine();
        } while (quantity < 0);

        String purchaseInfo = storeName + "," + productName + "," + quantity;

        try (Socket socket = new Socket("localhost", 5050)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            out.println("BUY " + purchaseInfo);
            String response = in.readLine();
            if (response.startsWith("✅")) {
                System.out.println(response);
            } else {
                System.out.println("Σφάλμα κατά την αγορά: " + response);
            }
            
        } catch (IOException e) {
            System.out.println("Σφάλμα σύνδεσης με Master.");
            e.printStackTrace();
        }
    }

    public static void rateStore(Scanner scanner) {
        System.out.print("Δώσε όνομα καταστήματος για βαθμολογία: ");
        String storeName = scanner.nextLine();

        int rating;
        do {
            System.out.print("Δώσε βαθμολογία (1 εως 5): ");
            while (!scanner.hasNextInt()) {
                System.out.print("Μη έγκυρος αριθμός. Ξαναπροσπάθησε: ");
                scanner.next();
            }
            rating = scanner.nextInt();
            scanner.nextLine();
        } while (rating < 1 || rating > 5);

        System.out.print("Σχόλιο (προαιρετικό): ");
        String comment = scanner.nextLine();

        String ratingInfo = storeName + "," + rating + "," + comment;

        try (Socket socket = new Socket("localhost", 5050)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("RATE " + ratingInfo);
            String response = in.readLine();

            if (response.startsWith("✅") || response.contains("RATING_SUCCESSFUL")) {
                System.out.println(response);
            } else {
                System.out.println("Σφάλμα κατά την καταχώρηση της βαθμολογίας: " + response);
            }
            
        } catch (IOException e) {
            System.out.println("Σφάλμα σύνδεσης με Master.");
            e.printStackTrace();
        }
    }
}
