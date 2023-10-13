package splitter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

public class Main {
    static List<Transaction> transactions = new ArrayList<>();
    static List<Group> groups = new ArrayList<>();
    static List<Transaction> result = new ArrayList<>();

    public static void main(String[] args) {

        while (true) {
            try (Scanner scanner = new Scanner(System.in)) {
                String cmd = scanner.nextLine().trim();
                if (cmd.equals("help")) {
                    System.out.println("""
                            balance
                            borrow
                            exit
                            group
                            help
                            purchase
                            repay""");
                    continue;
                }
                if (cmd.equals("exit")) {
                    break;
                }

                if (cmd.contains("group ")) {
                    if (cmd.contains("create ")) {
                        createGroup(cmd);
                        continue;
                    } else if (cmd.contains("show ")) {
                        showGroup(cmd);
                        continue;
                    } else if (cmd.contains("add ")) {
                        addToGroup(cmd);
                        continue;
                    } else if (cmd.contains("remove ")) {
                        deleteFromGroup(cmd);
                        continue;
                    }
                }

                String[] cmdList = cmd.split("\\s+");
                if (cmd.contains("purchase ")) {
                    doPurchase(cmd);
                    continue;
                }
                if (cmd.contains("balance")) {
                    doBalance(cmdList);
                    continue;
                }
                if (cmd.contains("borrow ")) {
                    doTransaction(cmdList, "borrow");
                    continue;
                }
                if (cmd.contains("repay ")) {
                    doTransaction(cmdList, "repay");
                    continue;
                }
                System.out.println("Unknown command. Print help to show commands list");
            }
        }
    }

    private static String getGroupName(String str) {
        return str.split("\\s+")[2];
    }

    private static Group getGroupByName(String name) {
        return groups.stream()
                .filter(x -> x.getName().equals(name))
                .findFirst()
                .get();
    }

    private static List<String> getAllParticipants(String str) {
        ArrayList<String> list = new ArrayList<>(Arrays.stream(str.substring(str.indexOf('(') + 1, str.indexOf(')'))
                .replace("+", "")
                .split(",\\s+"))
                .toList());
        ListIterator<String> iterator = list.listIterator();
        while (iterator.hasNext()) {
            String elem = iterator.next();
            boolean isRemoved = elem.charAt(0) == '-';
            String name = elem.replace("-", "");
            if (Group.isAGroup(name)) {
                iterator.remove();
                for (String el : getGroupByName(name).getParticipants()) {
                    if (isRemoved) {
                        iterator.add("-"+ el);
                    } else {
                        iterator.add(el);
                    }
                }
            }

        }
        System.out.println(list);
        Set<String> removeParticipants = new HashSet<>(list.stream()
                .filter(x -> x.contains("-"))
                .map(x -> x.replace("-", ""))
                .toList());
        Set<String> addParticipants = new HashSet<>(list.stream()
                .filter(x -> !x.contains("-"))
                .toList());

        addParticipants.removeAll(removeParticipants);
        return addParticipants.stream()
                .sorted()
                .toList();
    }

    private static void showGroup(String cmdStr) {
        if (Group.isValidOperation(cmdStr, "show")) {
            String name = getGroupName(cmdStr);
            if (groups.stream()
                    .anyMatch(x -> x.getName().equals(name))) {
                List<String> participantsList = getGroupByName(name)
                        .getParticipants();
                if (participantsList.isEmpty()) {
                    System.out.println("Group is empty");
                } else {
                    participantsList
                        .stream().sorted()
                        .forEach(System.out::println);
                }
            } else {
                System.out.println("Unknown group");
            }
        } else {
            printCmdError();
        }
    }

    private static void createGroup(String cmdStr) {
        if (Group.isValidOperation(cmdStr, "create")) {
            String name = getGroupName(cmdStr);
            if (groups.stream().anyMatch(x -> x.getName().equals(name))) {
                ListIterator<Group> iterator = groups.listIterator();
                while (iterator.hasNext()) {
                    if (iterator.next().getName().equals(name)) {
                        iterator.remove();
                        break;
                    }
                }
            }
            groups.add(new Group(name, getAllParticipants(cmdStr)));
        } else {
            printCmdError();
        }
    }

    private static void addToGroup(String cmdStr) {
        if (Group.isValidOperation(cmdStr, "add")) {
            String name = getGroupName(cmdStr);
            Group group = getGroupByName(name);
            group.setParticipants(getAllParticipants(cmdStr
                    .replace(")", ", " + name + ")")));
        }
    }

    private static void deleteFromGroup(String cmdStr) {
        if (Group.isValidOperation(cmdStr, "remove")) {
            String name = getGroupName(cmdStr);
            Group group = getGroupByName(name);
            Group tempGroup = new Group("TEMP",
                    getAllParticipants(cmdStr));
            Set<String> groupSet = new HashSet<String>(group.getParticipants());
            groupSet.removeAll(new HashSet<String>(tempGroup.getParticipants()));
            group.setParticipants(groupSet.stream().toList());
        }
    }

    private static void doPurchase(String cmd) {
        if (Transaction.isValidP2G(cmd, "purchase")) {
            String[] cmdList = cmd.substring(0, cmd.indexOf("(")).split("\\s+");
            int shift = cmdList.length == 4 ? 0 : 1;
            LocalDate date = cmdList[0].matches("purchase") ?
                    LocalDate.now() :
                    LocalDate.parse(cmdList[0].replace('.', '-'));
            String acc_credit = cmdList[shift + 1].trim();
            List<String> participantsList = getAllParticipants(cmd);
            int qtyParticipants = participantsList.size();
            if (qtyParticipants == 0) {
                System.out.println("Group is empty");
            } else {
                Double sum = Double.parseDouble(cmdList[shift + 3].trim())
                        / qtyParticipants;
                for (int i = 0; i < qtyParticipants; i++) {
                    String participant = participantsList.get(i);
                    if (participant.equals(acc_credit)) {
                        continue;
                    }
                    if (i == qtyParticipants - 1) {
                        sum = Double.parseDouble(cmdList[shift + 3].trim()) - new BigDecimal(sum)
                                .setScale(2, RoundingMode.HALF_UP)
                                .doubleValue() * (qtyParticipants - 1);
                    } else {
                        sum = new BigDecimal(sum)
                                .setScale(2, RoundingMode.HALF_UP)
                                .doubleValue();
                    }
                    transactions.add(new Transaction(date,
                            participant,
                            acc_credit,
                            sum));
                }
            }
        } else {
            printCmdError();
        }
    }

    private static void doTransaction(String[] cmdList, String cmd) {
        if (Transaction.isValidP2P(cmdList, "borrow")
        || Transaction.isValidP2P(cmdList, "repay")) {
            int shift = cmdList.length == 4 ? 0 : 1;

            LocalDate date = cmdList[0].matches("borrow|repay") ?
                    LocalDate.now() :
                    LocalDate.parse(cmdList[0].replace('.', '-'));
            double sum = Double.parseDouble(cmdList[shift + 3]);
            sum = cmd.equals("borrow")
                    ? sum
                    : new BigDecimal(-1.00 * sum)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();

            transactions.add(new Transaction(date,
                    cmdList[shift + 1],
                    cmdList[shift + 2],
                    sum));
        } else {
            printCmdError();
        }
    }

    private static void doBalance(String[] cmdList) {

        if (Transaction.isValidBalance(cmdList)) {
            LocalDate date;
            if (cmdList[1].equals("balance")) {
                date = LocalDate.parse(cmdList[0].replace('.', '-'));
                if (cmdList.length == 3 && cmdList[2].equals("open"))
                    date = date.withDayOfMonth(1).minusDays(1);
            } else {
                date = LocalDate.now();
                if (cmdList.length == 2 && cmdList[1].equals("open")) {
                    date = date.withDayOfMonth(1).minusDays(1);
                }
            }

            result.clear();
            for (Transaction trsc : transactions) {
                if (trsc.getDate().isAfter(date)) {
                    continue;
                }
                if (result.isEmpty()) {
                    result.add(new Transaction(trsc.getDate(), trsc.getAcc_debit(), trsc.getAcc_credit(), trsc.getSum()));
                    continue;
                }
                ListIterator<Transaction> iterator = result.listIterator();
                boolean isExistsRecord = false;
                do {
                        Transaction curTransaction = iterator.next();
                        if (curTransaction.getAcc_debit().equals(trsc.getAcc_debit())
                                && curTransaction.getAcc_credit().equals(trsc.getAcc_credit())) {
                            curTransaction.setSum(curTransaction.getSum() + trsc.getSum());
                            isExistsRecord = true;
                        } else if (curTransaction.getAcc_debit().equals(trsc.getAcc_credit())
                                && curTransaction.getAcc_credit().equals(trsc.getAcc_debit())) {
                            curTransaction.setSum(curTransaction.getSum() - trsc.getSum());
                            isExistsRecord = true;
                        }
                } while (iterator.hasNext());

                if (!isExistsRecord) {
                    iterator.add(new Transaction(trsc.getDate(), trsc.getAcc_debit(), trsc.getAcc_credit(), trsc.getSum()));
                }
            }
            outputBalance();
        } else {
            printCmdError();
        }
    }

    private static void outputBalance() {
        boolean hasRepayments = false;
        List<Transaction> resList = result.stream()
                .sorted(Comparator.comparing(Transaction::getAcc_credit))
                .sorted(Comparator.comparing(Transaction::getAcc_debit))
                .toList();
        for (Transaction rec : resList) {
            double sum = new BigDecimal(rec.getSum())
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
            if (sum > 0.00) {
                hasRepayments = true;
                System.out.printf("%s owes %s %.2f%n",
                        rec.getAcc_debit(),
                        rec.getAcc_credit(),
                        sum);
            } else if (sum < 0.00){
                hasRepayments = true;
                System.out.printf("%s owes %s %.2f%n",
                        rec.getAcc_credit(),
                        rec.getAcc_debit(),
                        -1 * sum);
            }
        }
        if (!hasRepayments)
            System.out.println("No repayments");
    }

    private static void printCmdError() {
        System.out.println("Illegal command arguments");
    }
}
