import java.io.*;
import java.net.*;
import java.util.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


public class Worker implements Runnable {
    private int workerId;
    private int port;
    private Map<String, String> stores = new HashMap<>(); // Key: storeName, Value: storeData
    private Map<String, Integer> sales = new HashMap<>();  // Κρατάει τις συνολικές πωλήσεις ανά προϊόν


    public Worker(int workerId, int port) {
        this.workerId = workerId;
        this.port = port;
        System.out.println("Worker " + workerId + " initialized on port " + port);
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new WorkerHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class WorkerHandler extends Thread {
        private Socket socket;
        

        public WorkerHandler(Socket socket) {
            this.socket = socket;
        }
        
        private synchronized String searchWithFilters(String jsonQuery) {
            Gson gson = new Gson();
        
            try {
                // 1. Φόρτωση φίλτρων
                FilterRequest filter = gson.fromJson(jsonQuery, FilterRequest.class);
        
                List<String> results = new ArrayList<>();
        
                for (String storeJson : stores.values()) {
                    Store store = gson.fromJson(storeJson, Store.class);
        
                    // --- ΦΙΛΤΡΟ 1: minStars ---
                    if (store.Stars < filter.minStars) continue;
        
                    // --- ΦΙΛΤΡΟ 2: priceCategory μέσα σε priceLevels ---
                    if (!filter.priceLevels.contains(store.PriceCategory)) continue;
        
                    // --- ΦΙΛΤΡΟ 3: κάποιο προϊόν έχει το ζητούμενο category ---
                    boolean found = false;
                    for (Product p : store.Products) {
                        if (!p.hidden && p.ProductType.equalsIgnoreCase(filter.category)) {
                            found = true;
                            break;
                        }
                    }
                    
                    if (!found) continue;
        
                    // Όλα τα φίλτρα πέρασαν:
                    results.add(storeJson);
                }
        
                if (results.isEmpty()) return "NO_RESULTS";
                return String.join(";", results);
        
            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR: Invalid JSON query.";
            }

        }
        
       
  @Override
public void run() {
    try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

        String request;
        while ((request = in.readLine()) != null) {
            System.out.println("Worker " + workerId + " received request: " + request);
            String response = handleRequest(request);
            out.println(response);  // 
        }

        System.out.println("Worker " + workerId + ": Η σύνδεση έκλεισε από Master.");

    } catch (IOException e) {
        System.out.println("❌ Worker " + workerId + " αντιμετώπισε σφάλμα: " + e.getMessage());
    }
}


        private synchronized String handleRating(String data) {
            String[] parts = data.split(",", 3);
            if (parts.length < 2) return "ERROR: Invalid RATING format";
        
            String storeName = parts[0].trim();
            int rating;
            String comment = parts.length == 3 ? parts[2].trim() : "";
        
            try {
                rating = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                return "ERROR: Invalid rating number";
            }
        
            if (!stores.containsKey(storeName)) {
                return "ERROR: Store not found";
            }
        
            Gson gson = new Gson();
            Store store = gson.fromJson(stores.get(storeName), Store.class);
        
            // Ενημέρωση βαθμολογίας:
            int oldTotal = store.Stars * store.NoOfVotes;
            store.NoOfVotes += 1;
            store.Stars = (int) Math.round((oldTotal + rating) / (double) store.NoOfVotes);
        
            stores.put(storeName, gson.toJson(store));
        
            System.out.println("⭐ Καταχωρήθηκε βαθμολογία " + rating + " για κατάστημα " + storeName + " (νέος μέσος όρος: " + store.Stars + ")");
            return "RATING_SUCCESSFUL";
        }
        
        private synchronized String handleRequest(String request) {
            if (request == null) {
                System.out.println("⚠️ Connection closed by Master (received null request).");
                return "ERROR: Empty request received";
            }
            
            String[] parts = request.split(" ", 2);
            String command = parts[0];

            switch (command) {
                case "SEARCH":
                    return searchStores(parts[1]);
                case "ADD_STORE":
                    return addStore(parts[1]);
                case "BUY":
                    return handleBuy(parts[1]);
                 case "SALES_REPORT":
                    return reportSales();
                    case "RATE":
                    return handleRating(parts[1]);
                case "GET_STORE":
                    return getStoreByName(parts[1]);
                case "AGGREGATE_BY_STORE_TYPE":
                    return aggregateByStoreType();
                case "AGGREGATE_BY_CATEGORY":
                    return aggregateByCategory();
                
                default:
                    return "ERROR: Unknown command";
            }
        }
        private synchronized String searchStores(String input) {
            if (input.trim().startsWith("{")) {
                // Αν ξεκινά με { θεωρείται JSON query → χρήση φίλτρων
                return searchWithFilters(input);
            } else {
                // Αλλιώς θεωρείται απλό keyword search
                return simpleKeywordSearch(input);
            }
        }
        

        private String reportSales() {
            if (Worker.this.sales.isEmpty()) {
                return "NO_SALES";
            }
        
            StringBuilder report = new StringBuilder();
            for (Map.Entry<String, Integer> entry : Worker.this.sales.entrySet()) {
                report.append(entry.getKey())
                      .append(": ")
                      .append(entry.getValue())
                      .append(" sold; ");
            }
            return report.toString();
        }
        private synchronized String aggregateByStoreType() {
            Map<String, Double> result = new HashMap<>();
            Gson gson = new Gson();
            for (String storeJson : stores.values()) {
                Store store = gson.fromJson(storeJson, Store.class);
                double total = 0.0;
                for (Product p : store.Products) {
                    int sold = sales.getOrDefault(p.ProductName, 0);
                    total += sold * p.Price;
                }
                result.put(store.FoodCategory,result.getOrDefault(store.FoodCategory, 0.0) + total);
            }
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Double> entry : result.entrySet()) {
                sb.append(entry.getKey()).append(",").append(entry.getValue()).append(";");
            }
            return sb.toString();
        }
        
        private synchronized String aggregateByCategory() {
            Map<String, Double> result = new HashMap<>();
            Gson gson = new Gson();
            for (String storeJson : stores.values()) {
                Store store = gson.fromJson(storeJson, Store.class);
                for (Product p : store.Products) {
                    int sold = sales.getOrDefault(p.ProductName, 0);
                    double revenue = sold * p.Price;
                    String category = p.ProductType;
if (category == null || category.trim().isEmpty()) {
    category = "ΑΠΡΟΣΔΙΟΡΙΣΤΟ";
}
result.put(category, result.getOrDefault(category, 0.0) + revenue);

                }
            }
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Double> entry : result.entrySet()) {
                sb.append(entry.getKey()).append(",").append(entry.getValue()).append(";");
            }
            return sb.toString();
        }
        
        private synchronized String simpleKeywordSearch(String searchQuery) {
            List<String> results = new ArrayList<>();
            Gson gson = new Gson();
        
            System.out.println("Ξεκίνησε αναζήτηση για: \"" + searchQuery + "\"");
            System.out.println("Αποθηκευμένα καταστήματα: " + stores.keySet());
        
            for (Map.Entry<String, String> entry : stores.entrySet()) {
                String storeName = entry.getKey();
                String storeJson = entry.getValue();
        
                Store store = gson.fromJson(storeJson, Store.class);
        
                boolean found = false;
                for (Product p : store.Products) {
                    if (!p.hidden && p.ProductName.toLowerCase().contains(searchQuery.toLowerCase())) {
                        found = true;
                        break;
                    }
                }
        
                if (found) {
                    System.out.println("✅ Αντιστοιχία στο κατάστημα: " + storeName);
        
                    // 🔍 Φιλτράρουμε τα προϊόντα που δεν είναι κρυφά
                    List<Product> visibleProducts = new ArrayList<>();
                    for (Product p : store.Products) {
                        if (!p.hidden) visibleProducts.add(p);
                    }
        
                    store.Products = visibleProducts; // αντικαθιστούμε με τα "επιτρεπτά"
        
                    String filteredStoreJson = gson.toJson(store);
                    results.add(filteredStoreJson);
                } else {
                    System.out.println("❌ Καμία αντιστοιχία στο: " + storeName);
                }
            }
        
            if (results.isEmpty()) {
                System.out.println("Δεν βρέθηκαν αποτελέσματα.");
                return "NO_RESULTS";
            } else {
                String joined = String.join(";", results);
                System.out.println(" Επιστροφή αποτελεσμάτων: " + joined);
                return joined;
            }
        }
        
        

        private synchronized String addStore(String storeData) {
            Gson gson = new Gson();
            Store store = gson.fromJson(storeData, Store.class);
        
            // 👉 Υπολογισμός μέσης τιμής προϊόντων:
            double sum = 0;
            for (Product p : store.Products) {
                sum += p.Price;
            }
            double avg = sum / store.Products.size();
        
            // 👉 Καθορισμός Price Category:
            if (avg <= 5) {
                store.PriceCategory = "$";
            } else if (avg <= 15) {
                store.PriceCategory = "$$";
            } else {
                store.PriceCategory = "$$$";
            }
        
            // 👉 Serialize ξανά το κατάστημα με το νέο PriceCategory:
            String updatedStoreJson = gson.toJson(store);
            stores.put(store.StoreName, updatedStoreJson);
        
            System.out.println("📦 Προστέθηκε κατάστημα: " + store.StoreName + " με Price Category: " + store.PriceCategory);
            System.out.println("📦 Όλα τα keys: " + stores.keySet());
            return "STORE_ADDED";
        }
        
        
        private synchronized String getStoreByName(String storeName) {
            if (!stores.containsKey(storeName)) return "ERROR: Store not found";
        
            Gson gson = new Gson();
            Store store = gson.fromJson(stores.get(storeName), Store.class);
        
            // Φιλτράρουμε τα κρυφά προϊόντα
            List<Product> visibleProducts = new ArrayList<>();
            for (Product p : store.Products) {
                if (!p.hidden) {
                    visibleProducts.add(p);
                }
            }
        
            store.Products = visibleProducts;
            return gson.toJson(store);
        }
        

        private synchronized String handleBuy(String data) {
            System.out.println(" Worker Store Keys: " + stores.keySet());
            String[] parts = data.split(",", 3);
            if (parts.length != 3) return "ERROR: Invalid BUY format";

            for (int i = 0; i < parts.length; i++) {
                parts[i] = parts[i].trim();
            }

            String storeName = parts[0];
            String productName = parts[1];
            int qty;
           


            try {
                qty = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                return "ERROR: Invalid quantity";
            }
            System.out.println("Ζητήθηκε αγορά από κατάστημα: [" + storeName + "]");
            System.out.println("Worker Store Keys: " + stores.keySet());

            if (!stores.containsKey(storeName)) {
                return "ERROR: Store not found";
            }

            String storeJson = stores.get(storeName);
            Gson gson = new Gson();
            Store store = gson.fromJson(storeJson, Store.class);
            for (Product p : store.Products) {
                System.out.println("🔍 Εξέταση προϊόντος: " + p.ProductName + " (αναζητείται: " + productName + ")");
                
                if (p.ProductName.equalsIgnoreCase(productName)) {
                    System.out.println("✅ Ταιριάστηκε: " + p.ProductName);
                    
                    if (p.Available_Amount >= qty) {
                        p.Available_Amount -= qty;
            
                        // Ενημέρωση των πωλήσεων:
                        int prev = sales.getOrDefault(productName, 0);
                        sales.put(productName, prev + qty);
            
                        System.out.println("📈 Καταχώρηση στο sales: " + productName + " -> από " + prev + " σε " + sales.get(productName));
            
                        stores.put(storeName, gson.toJson(store));
                        return "✅ Αγορά καταχωρήθηκε (" + qty + " x " + productName + " από " + storeName + ")";
                    } else {
                        return "❌ Μη επαρκές απόθεμα.";
                    }
                }
            }
            

            return "❌ Προϊόν δεν βρέθηκε.";
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Worker <workerId>");
            return;
        }

        int workerId = Integer.parseInt(args[0]);
        int port = 5050 + workerId + 1;

        Worker worker = new Worker(workerId, port);
        new Thread(worker).start();
    }
    
}

 class FilterRequest {
    double latitude;
    double longitude;
    String category;
    int minStars;
    List<String> priceLevels;
}


