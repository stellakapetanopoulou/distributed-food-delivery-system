import java.io.*;
import java.net.*;
import java.util.*;
import com.google.gson.Gson;

public class Master {
    private static final int PORT = 5050;
    private static List<WorkerConnection> workers = new ArrayList<>();
    private static int numWorkers = 3;

    private static class WorkerConnection {
        private int port;
    
        public WorkerConnection(int id) throws IOException {
            this.port = 5051 + id;
    

        }
    
        public String sendMessage(String message) {
            try (
                Socket socket = new Socket("localhost", this.port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
            ) {
                out.println(message);
                return in.readLine();
            } catch (IOException e) {
                return "ERROR: Worker communication failed";
            }
        }
    }
    
    public static void main(String[] args) {
        System.out.println("Master Server is running on port " + PORT);

        // Εκκίνηση Workers με retry
        for (int i = 0; i < numWorkers; i++) {
            WorkerConnection worker = null;
            while (worker == null) {
                try {
                    worker = new WorkerConnection(i);
                    workers.add(worker);
                    System.out.println("✅ Συνδέθηκε με Worker " + i);
                } catch (IOException e) {
                    System.out.println("❌ Worker " + i + " δεν ανταποκρίνεται. Προσπάθεια ξανά σε 1 δευτερόλεπτο...");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            }
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                String request;
                while ((request = in.readLine()) != null) {
                    System.out.println("Received request: " + request);
                    String response = handleRequest(request);
                    if (response == null) {
                        System.out.println("Προειδοποίηση: Ο Worker δεν έστειλε απάντηση.");
                        out.println("ERROR: No response from Worker");
                    } else {
                        out.println(response);
                    }
                }
                
            } catch (IOException e) {
                if (e.getMessage().contains("Connection reset")) {
                    System.out.println(" Η σύνδεση με τον Worker τερματίστηκε απροσδόκητα (Connection reset).");
                } else {
                    System.out.println(" Σφάλμα κατά την επικοινωνία: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
        }

        private String handleRequest(String request) {
            String[] parts = request.split(" ", 2);
            String command = parts[0];
            if (request.equals("AGGREGATE_SALES_BY_STORE_TYPE")) {
                Map<String, Double> result = new HashMap<>();
                for (WorkerConnection worker : workers) {
                    String res = worker.sendMessage("AGGREGATE_BY_STORE_TYPE");
                    if (res != null && !res.startsWith("ERROR")) {
                        String[] pairs = res.split(";");
                        for (String pair : pairs) {
                            String[] kv = pair.split(",");
                            if (kv.length == 2) {
                                String key = kv[0];
                                double val = Double.parseDouble(kv[1]);
                                result.put(key, result.getOrDefault(key, 0.0) + val);
                            }
                        }
                    }
                }
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, Double> entry : result.entrySet()) {
                    sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("€\n");
                }
                return sb.toString() + "END";
            }
            
            if (request.equals("AGGREGATE_SALES_BY_CATEGORY")) {
                Map<String, Double> result = new HashMap<>();
                for (WorkerConnection worker : workers) {
                    String res = worker.sendMessage("AGGREGATE_BY_CATEGORY");
                    if (res != null && !res.startsWith("ERROR")) {
                        String[] pairs = res.split(";");
                        for (String pair : pairs) {
                            String[] kv = pair.split(",");
                            if (kv.length == 2) {
                                String key = kv[0];
                                double val = Double.parseDouble(kv[1]);
                                result.put(key, result.getOrDefault(key, 0.0) + val);
                            }
                        }
                    }
                }
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, Double> entry : result.entrySet()) {
                    sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("€\n");
                }
                return sb.toString() + "END";
            }
            

            switch (command) {
                case "ADD_STORE":
                    return addStore(parts[1]);
                case "SEARCH":
                    return searchStores(parts[1]);
                case "BUY":
                    return processPurchase(parts[1]);
                case "SALES_REPORT":                                     
                    return collectSalesReport();
                    case "RATE":
                    return processRating(parts[1]);
                case "GET_STORE":
                    return getStoreData(parts[1]);
                    case "AGGREGATE_BY_STORE_TYPE":
    StringBuilder storeTypeResults = new StringBuilder();
    for (WorkerConnection worker : workers) {
        String response = worker.sendMessage("AGGREGATE_BY_STORE_TYPE");
        System.out.println("📥 Απάντηση από Worker: " + response);
        if (response != null && !response.trim().isEmpty()) {
            storeTypeResults.append(response).append("\n");
        }
    }
    return storeTypeResults.toString().trim();

    

            default:
                    return "ERROR: Unknown command";
            }
        }
    }

    private static String getStoreData(String storeName) {
        String normalized = storeName.toLowerCase();
        int hash = normalized.hashCode();
        int workerIndex = Math.abs(hash) % workers.size();
    
        System.out.println(" GET_STORE για: " + storeName + " -> Worker " + workerIndex);
        return workers.get(workerIndex).sendMessage("GET_STORE " + storeName);
    }
    
    private static String processRating(String ratingInfo) {
        String storeName = ratingInfo.split(",")[0].trim();
        String normalized = storeName.toLowerCase();
        int hash = normalized.hashCode();
        int workerIndex = Math.abs(hash) % workers.size();
    
        System.out.println("Καταχώρηση βαθμολογίας για [" + storeName + "] στον Worker " + workerIndex);
        return workers.get(workerIndex).sendMessage("RATE " + ratingInfo);
    }

    
    private static String addStore(String storeData) {
        Gson gson = new Gson();
        Store store = gson.fromJson(storeData, Store.class);
        String normalized = store.StoreName.toLowerCase();
        int hash = normalized.hashCode();
        int workerIndex = Math.abs(hash) % workers.size(); // Worker κατανομής!
    
        System.out.println("Αποθήκευση καταστήματος: [" + store.StoreName + "] στον Worker " + workerIndex);
    
        String response = workers.get(workerIndex).sendMessage("ADD_STORE " + storeData);
        return "STORE_ADDED_TO_WORKER_" + workerIndex + " | Response: " + response;
    }
    
    

    private static String searchStores(String searchQuery) {
        List<String> allResults = new ArrayList<>();
        for (WorkerConnection worker : workers) {
            String response = worker.sendMessage("SEARCH " + searchQuery);
            System.out.println("📥 Απάντηση από Worker: " + response);

            if (response != null && !response.equals("NO_RESULTS")) {
                allResults.add(response);
            }
        }
        return allResults.isEmpty() ? "NO_RESULTS" : String.join(";", allResults);
    }

    private static String processPurchase(String purchaseInfo) {
        String storeName = purchaseInfo.split(",")[0].trim();
        System.out.println("🔍 Ζητήθηκε αγορά από κατάστημα: [" + storeName + "]");
    
        String normalized = storeName.toLowerCase();
        int hash = normalized.hashCode();
        int workerIndex = Math.abs(hash) % workers.size();
    
        System.out.println("Worker Index για αγορά: " + workerIndex);
    
        return workers.get(workerIndex).sendMessage("BUY " + purchaseInfo);
    }

    private static String collectSalesReport() {
        List<String> allReports = new ArrayList<>();
        System.out.println("Συλλογή πωλήσεων από όλους τους Workers...");
        for (WorkerConnection worker : workers) {
            String report = worker.sendMessage("SALES_REPORT");
            if (report != null && !report.equals("NO_SALES")) {
                allReports.add(report);
            }
        }
        return allReports.isEmpty() ? "NO_SALES" : String.join(";", allReports);
    }
}


