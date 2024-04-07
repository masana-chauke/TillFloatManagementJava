import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

class Item {
    String description;
    int amount;

    Item(String description, int amount) {
        this.description = description;
        this.amount = amount;
    }
}

class Transaction {
    List<Item> items;
    List<Integer> paid;

    Transaction(List<Item> items, List<Integer> paid) {
        this.items = items;
        this.paid = paid;
    }
}

class TillState {
    Map<Integer, Integer> till;
    int tillStart;

    TillState(Map<Integer, Integer> till, int tillStart) {
        this.till = till;
        this.tillStart = tillStart;
    }
}

public class App {

    public static TillState initializeTill() {
        Map<Integer, Integer> till = new HashMap<>();
        int tillStart = 0;

        String[][] denominations = {
                {"5 x R50", "50"},
                {"5 x R20", "20"},
                {"6 x R10", "10"},
                {"12 x R5", "5"},
                {"10 x R2", "2"},
                {"10 x R1", "1"}
        };

        for (String[] denomination : denominations) {
            String note = denomination[0];
            int currency = Integer.parseInt(denomination[1]);
            String[] splitNote = note.split(" x ");
            int num = Integer.parseInt(splitNote[0]);
            till.put(currency, num);
            tillStart += num * currency;
        }

        return new TillState(till, tillStart);
    }

    public static String calculateChange(int change, Map<Integer, Integer> till) {
        if (change == 0) return "No Change";

        List<Integer> coins = new ArrayList<>(till.keySet());
        coins.removeIf(coin -> coin > change);
        coins.sort(Comparator.reverseOrder());
        List<Integer> usedCoins = new ArrayList<>();
        int remainingChange = change;

        for (int coin : coins) {
            while (remainingChange >= coin && till.getOrDefault(coin, 0) > 0) {
                usedCoins.add(coin);
                remainingChange -= coin;
                till.put(coin, till.get(coin) - 1);
            }
        }

        if (remainingChange != 0) {
            usedCoins.forEach(coin -> till.put(coin, till.get(coin) + Collections.frequency(usedCoins, coin)));
            return "No Change";
        }

        return usedCoins.stream()
                .map(coin -> "R" + coin)
                .collect(Collectors.joining("-"));
    }

    public static TillState processTransaction(Transaction transaction, TillState tillState) {
        Map<Integer, Integer> till = new HashMap<>(tillState.till);
        int tillStart = tillState.tillStart;
        int transactionTotal = transaction.items.stream().mapToInt(item -> item.amount).sum();
        int totalPaid = transaction.paid.stream().mapToInt(Integer::intValue).sum();
        int changeTotal = totalPaid - transactionTotal;
        String changeBreakdown = calculateChange(changeTotal, till);

        System.out.printf("R%d, R%d, R%d, R%d, %s%n", tillStart, transactionTotal, totalPaid, changeTotal, changeBreakdown);

        int updatedTillStart = tillStart + transactionTotal;
        for (Item item : transaction.items) {
            till.put(item.amount, till.getOrDefault(item.amount, 0) - 1);
        }

        return new TillState(till, updatedTillStart);
    }

    public static List<Transaction> parseInput(String input) {
        List<Transaction> transactions = new ArrayList<>();
        String[] lines = input.trim().split("\n");
        for (String line : lines) {
            String[] parts = line.split(",");
            String itemsStr = parts[0];
            String paidStr = parts[1];
            String[] itemsArr = itemsStr.split(";");
            List<Item> items = Arrays.stream(itemsArr)
                    .map(itemStr -> {
                        String[] itemParts = itemStr.trim().split(" R");
                        String description = itemParts[0].trim();
                        int amount = Integer.parseInt(itemParts[1]);
                        return new Item(description, amount);
                    })
                    .collect(Collectors.toList());

            List<Integer> paid = Arrays.stream(paidStr.split("-"))
                    .map(amount -> Integer.parseInt(amount.substring(1).trim()))
                    .collect(Collectors.toList());

            transactions.add(new Transaction(items, paid));
        }
        return transactions;
    }

    public static void main(String[] args) {
        try {
            TillState tillState = initializeTill();
            String inputFile = Files.readString(Paths.get("src/input.txt"));
            List<Transaction> transactions = parseInput(inputFile);

            System.out.println("Transaction Summary:");
            System.out.println("Till Start, Transaction Total, Paid, Change Total, Change Breakdown");

            for (Transaction transaction : transactions) {
                tillState = processTransaction(transaction, tillState);
            }

            System.out.println("Remaining Till Balance: R" + tillState.tillStart);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
