import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

enum Type { INCOME, EXPENSE; }

class Transaction {
    private LocalDate date;
    private Type type;
    private String category;
    private double amount;
    private String note;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    public Transaction(LocalDate date, Type type, String category, double amount, String note) {
        this.date = date;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.note = note;
    }

    public LocalDate getDate() { return date; }
    public Type getType()      { return type; }
    public String getCategory(){ return category; }
    public double getAmount()  { return amount; }

    /** CSV format: YYYY-MM-DD,INCOME,Salary,5000.00,July salary */
    public String toCsvLine() {
        return String.join(",",
                date.format(FMT),
                type.name(),
                category,
                String.valueOf(amount),
                note.replace(",", " ")
        );
    }

    public static Transaction fromCsvLine(String line) throws IllegalArgumentException {
        String[] parts = line.split(",", 5);
        if (parts.length < 4) throw new IllegalArgumentException("Bad CSV: " + line);
        LocalDate dt    = LocalDate.parse(parts[0], FMT);
        Type t           = Type.valueOf(parts[1].toUpperCase());
        String cat       = parts[2];
        double amt       = Double.parseDouble(parts[3]);
        String note      = parts.length==5 ? parts[4] : "";
        return new Transaction(dt, t, cat, amt, note);
    }
}

public class ExpenseTracker {
    private List<Transaction> transactions = new ArrayList<>();
    private Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        ExpenseTracker app = new ExpenseTracker();
        if (args.length > 0) {
            app.loadFromFile(args[0]);
        }
        app.run();
    }

    private void run() {
        while (true) {
            System.out.println("\n1) Add Transaction   2) Monthly Summary   3) Save & Exit");
            System.out.print("Choose an option: ");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> addTransaction();
                case "2" -> showMonthlySummary();
                case "3" -> { saveAndExit(); return; }
                default -> System.out.println("Invalid, try again.");
            }
        }
    }

    private void addTransaction() {
        System.out.print("Enter date (YYYY-MM-DD): ");
        LocalDate date = LocalDate.parse(sc.nextLine().trim());
        System.out.print("Type (income/expense): ");
        Type type = Type.valueOf(sc.nextLine().trim().toUpperCase());
        System.out.print("Category (e.g. Salary, Food, Rent, Travel): ");
        String category = sc.nextLine().trim();
        System.out.print("Amount: ");
        double amt = Double.parseDouble(sc.nextLine().trim());
        System.out.print("Note (optional): ");
        String note = sc.nextLine().trim();
        transactions.add(new Transaction(date, type, category, amt, note));
        System.out.println("✅ Added!");
    }

    private void showMonthlySummary() {
        System.out.print("Enter month to view (YYYY-MM): ");
        String[] ym = sc.nextLine().trim().split("-");
        int year = Integer.parseInt(ym[0]), month = Integer.parseInt(ym[1]);

        List<Transaction> monthTxns = transactions.stream()
                .filter(tx -> tx.getDate().getYear()==year && tx.getDate().getMonthValue()==month).collect(Collectors.toList());

        double totalIncome = monthTxns.stream().filter(tx -> tx.getType()==Type.INCOME)
                .mapToDouble(Transaction::getAmount).sum();

        double totalExpense = monthTxns.stream()
                .filter(tx -> tx.getType()==Type.EXPENSE)
                .mapToDouble(Transaction::getAmount).sum();

        System.out.printf("\nSummary for %04d-%02d:\n", year, month);
        System.out.printf("  Total Income : %.2f\n", totalIncome);
        System.out.printf("  Total Expense: %.2f\n", totalExpense);
        System.out.printf("  Net Balance  : %.2f\n\n", totalIncome - totalExpense);

        System.out.println("  Breakdown by category:");
        Map<String, Double> byCat = new TreeMap<>();
        for (Transaction tx : monthTxns) {
            String key = tx.getType() + " / " + tx.getCategory();
            byCat.put(key, byCat.getOrDefault(key, 0.0) + tx.getAmount());
        }
        byCat.forEach((k,v) -> System.out.printf("    %-20s : %.2f\n", k, v));
    }

    private void saveAndExit() {
        System.out.print("Enter filename to save CSV to: ");
        String fname = sc.nextLine().trim();
        try (PrintWriter pw = new PrintWriter(new FileWriter(fname))) {
            for (Transaction tx : transactions) {
                pw.println(tx.toCsvLine());
            }
            System.out.println("✅ Saved to " + fname);
        } catch (IOException e) {
            System.err.println("❌ Error saving: " + e.getMessage());
        }
    }

    private void loadFromFile(String fname) {
        try (BufferedReader br = new BufferedReader(new FileReader(fname))) {
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    transactions.add(Transaction.fromCsvLine(line));
                } catch (IllegalArgumentException ex) {
                    System.err.println("Skipping bad line: " + line);
                }
            }
            System.out.println("Loaded " + transactions.size() + " transactions from " + fname);
        } catch (IOException e) {
            System.err.println("Could not load file: " + e.getMessage());
        }
    }
}
