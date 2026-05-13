import java.util.*;
import java.io.*;
import java.net.Socket;
import com.google.gson.Gson;
import java.util.Locale;
import java.util.Scanner;

public class ManagerApp {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        scanner.useLocale(Locale.US);

        try (
            Socket socket = new Socket("localhost", 5050);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            List<Store> allStores = new ArrayList<>();
            System.out.print("Δώσε το αρχείο JSON καταστήματος: ");
            String file = scanner.nextLine();
            Store firstStore = StoreParser.parse(file);

            if (firstStore == null) {
                System.out.println("Αποτυχία φόρτωσης καταστήματος.");
                return;
            } else {
                allStores.add(firstStore);
            }

            while (true) {
                System.out.println("\n===== MENU =====");
                System.out.println("1. Προβολή προϊόντων");
                System.out.println("2. Προσθήκη προϊόντος");
                System.out.println("3. Αφαίρεση προϊόντος");
                System.out.println("4. Αλλαγή αποθέματος");
                System.out.println("5. Αποστολή στο Master");
                System.out.println("6. Προβολή συνολικών πωλήσεων ανά προϊόν");
                System.out.println("7. Φόρτωση νέου καταστήματος");
                System.out.println("8. Συνολικές πωλήσεις ανά τύπο καταστήματος");
                System.out.println("9. Συνολικές πωλήσεις ανά κατηγορία προϊόντος");
                System.out.println("0. Έξοδος");
                System.out.print("Επιλογή: ");

                int choice = scanner.nextInt();
                scanner.nextLine(); // consume newline

                switch (choice) {
                    case 1:
                        for (int i = 0; i < allStores.size(); i++) {
                            Store refreshed = getStoreFromMaster(allStores.get(i).StoreName);
                            if (refreshed != null) {
                                allStores.set(i, refreshed);
                            }
                        }
                        System.out.println("Προϊόντα από όλα τα καταστήματα:");
                        for (Store s : allStores) {
                            System.out.println("Κατάστημα: " + s.StoreName);
                            for (Product p : s.Products) {
                                if (!p.hidden) {
                                    System.out.println("- " + p.ProductName + " (" + p.Available_Amount + ") - " + p.Price + "€");
                                }
                            }
                        }
                        break;

                    case 5:
                        for (Store s : allStores) {
                            sendStoreToMaster(s);
                        }
                        break;

                    case 6:
                        out.println("SALES_REPORT");
                        String response = in.readLine();
                        System.out.println("Συνολικές πωλήσεις ανά προϊόν:\n" + response.replace(";", "\n"));
                        break;

                    case 8:
                        out.println("AGGREGATE_SALES_BY_STORE_TYPE");
                        String line8;
                        while ((line8 = in.readLine()) != null) {
                            if (line8.equals("END")) break;
                            System.out.println("👉 " + line8);
                        }
                        break;

                    case 9:
                        out.println("AGGREGATE_SALES_BY_CATEGORY");
                        String line9;
                        while ((line9 = in.readLine()) != null) {
                            if (line9.equals("END")) break;
                            System.out.println("👉 " + line9);
                        }
                        break;

                    case 0:
                        System.out.println("Έξοδος...");
                        return;

                    default:
                        System.out.println("❌ Άκυρη επιλογή.");
                }
            }

        } catch (IOException e) {
            System.out.println("❌ Σφάλμα σύνδεσης με τον Master.");
            e.printStackTrace();
        }
    }

    public static Store getStoreFromMaster(String storeName) {
        try (
            Socket socket = new Socket("localhost", 5050);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out.println("GET_STORE " + storeName);
            String json = in.readLine();
            if (json == null || json.startsWith("ERROR")) return null;
            Gson gson = new Gson();
            return gson.fromJson(json, Store.class);
        } catch (IOException e) {
            System.out.println("❌ Σφάλμα σύνδεσης για GET_STORE");
            return null;
        }
    }

    public static void sendStoreToMaster(Store store) {
        try (
            Socket socket = new Socket("localhost", 5050);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            Gson gson = new Gson();
            String json = gson.toJson(store);
            out.println("ADD_STORE " + json);
            System.out.println("✅ Κατάστημα στάλθηκε στον Master!");
        } catch (IOException e) {
            System.out.println("❌ Σφάλμα σύνδεσης με Master.");
        }
    }
}
