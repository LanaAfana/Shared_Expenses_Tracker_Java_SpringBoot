package splitter.presentation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import splitter.Group;
import splitter.Transaction;
import splitter.business.SharedExpensesService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Component
public class SharedExpensesOperator {

    private final SharedExpensesService service;

    private final List<Transaction> result;

    @Autowired
    public SharedExpensesOperator(SharedExpensesService service) {
        this.service = service;
        this.result = new ArrayList<>();
    }

    public void doSecretSanta(String[] cmdList) {
        if (Group.isAGroup(cmdList[1])) {
            List<String> list = Arrays.stream(service.findGroupByName(cmdList[1])
                    .get().getParticipants()
                    .get(0)
                    .split(","))
                    .toList();
            List<String> listTo = new ArrayList<>();
            Map<String, String> map = new HashMap<>();

            for (String participant : list.stream().sorted().toList()) {
                map.put(participant, "");
                listTo.add(participant);
            }

            Random random = new Random();
            int idx;
            for (int i = 0; i < map.size(); i++) {

                if (i == 0) {
                    idx = random.nextInt(map.size() - 1) + 1;
                    map.replace(list.get(i), listTo.get(idx));
                    listTo.remove(idx);
                } else {
                    do {
                        idx = random.nextInt(listTo.size());
                        // not equals to self and isn't reciprocal
                    } while(map.get(listTo.get(idx)).equals(listTo.get(idx)) || listTo.get(idx).equals(list.get(i)));
                    map.replace(list.get(i), listTo.get(idx));
                    listTo.remove(idx);
                }
            }
            map.forEach((k, v) -> System.out.printf("%s gift to %s%n", k, v));
        }
    }

    private String getGroupName(String str) {
        return str.split("\\s+")[2];
    }

    private List<String> getAllParticipants(String str) {
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
                for (String el : service.findGroupByName(name).get().getParticipants()) {
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

    public void showGroup(String cmdStr) {
        if (Group.isValidOperation(cmdStr, "show")) {
            String name = getGroupName(cmdStr);

            if (service.findGroupByName(name).isPresent()) {
                List<String> participantsList = service.findGroupByName(name).get()
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

    public void createGroup(String cmdStr) {
        if (Group.isValidOperation(cmdStr, "create")) {
            String name = getGroupName(cmdStr);
            if (service.findGroupByName(name).isPresent()) {
                service.deleteGroupByName(name);
            }
            service.saveGroup(new Group(name, getAllParticipants(cmdStr)));
        } else {
            printCmdError();
        }
    }

    public void addToGroup(String cmdStr) {
        if (Group.isValidOperation(cmdStr, "add")) {
            String name = getGroupName(cmdStr);
            service.saveGroup(new Group(name,
                    getAllParticipants(cmdStr.replace(")", ", " + name + ")"))));
        }
    }

    public void deleteFromGroup(String cmdStr) {
        if (Group.isValidOperation(cmdStr, "remove")) {
            String name = getGroupName(cmdStr);
            Group group = service.findGroupByName(name).get();
            Group tempGroup = new Group("TEMP",
                    getAllParticipants(cmdStr));
            Set<String> groupSet = new HashSet<>(group.getParticipants());
            groupSet.removeAll(new HashSet<>(tempGroup.getParticipants()));
            service.saveGroup(new Group(name,
                    groupSet.stream().toList()));

        }
    }

    public void doPurchase(String cmd) {
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
                double sum = Double.parseDouble(cmdList[shift + 3].trim())
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

                    service.saveTransaction(new Transaction(date,
                            participant,
                            acc_credit,
                            sum));
                }
            }
        } else {
            printCmdError();
        }
    }

    public void doTransaction(String[] cmdList, String cmd) {
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

            service.saveTransaction(new Transaction(date,
                    cmdList[shift + 1],
                    cmdList[shift + 2],
                    sum));
        } else {
            printCmdError();
        }
    }

    public void doBalance(String[] cmdList) {

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
            for (Transaction trsc : service.getAllTransactions()) {
                if (trsc.getDate().isAfter(date)) {
                    continue;
                }
                if (result.isEmpty()) {
                    result.add(new Transaction(trsc.getId(),
                            trsc.getDate(),
                            trsc.getAcc_debit(),
                            trsc.getAcc_credit(),
                            trsc.getSum()));
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
                    iterator.add(new Transaction(trsc.getId(),
                            trsc.getDate(),
                            trsc.getAcc_debit(),
                            trsc.getAcc_credit(),
                            trsc.getSum()));
                }
            }
            outputBalance();
        } else {
            printCmdError();
        }
    }

    private void outputBalance() {
        boolean hasRepayments = false;
        List<Transaction> resList = new ArrayList<>();

        for (Transaction rec : result.stream().toList()) {
            double sum = BigDecimal.valueOf(rec.getSum())
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
            if (sum > 0.00) {
                hasRepayments = true;
                resList.add(rec);
            } else if (sum < 0.00){
                hasRepayments = true;
                resList.add(new Transaction(rec.getId(), rec.getDate(),
                        rec.getAcc_credit(),
                        rec.getAcc_debit(),
                        -1 * sum));
            }
        }
        if (hasRepayments) {
            resList.stream()
                    .sorted(Comparator.comparing(Transaction::getAcc_credit))
                    .sorted(Comparator.comparing(Transaction::getAcc_debit))
                    .forEach(x -> System.out.printf("%s owes %s %.2f%n",
                            x.getAcc_debit(),
                            x.getAcc_credit(),
                            x.getSum()));
        } else {
            System.out.println("No repayments");
        }
    }

    private void printCmdError() {
        System.out.println("Illegal command arguments");
    }

    public void doWriteOff(String[] cmdList) {
        LocalDate date = cmdList.length == 1 ? LocalDate.now() :
                LocalDate.parse(cmdList[0].replace('.', '-'));
        service.deleteTransactionAfterDate(date);
    }

    public void doCashback(String cmd) {
        if (Transaction.isValidP2G(cmd, "writeOff")) {
            String[] cmdList = cmd.substring(0, cmd.indexOf("(")).split("\\s+");
            int shift = cmdList.length == 4 ? 0 : 1;
            LocalDate date = cmdList[0].matches("writeOff") ?
                    LocalDate.now() :
                    LocalDate.parse(cmdList[0].replace('.', '-'));
            String acc_debet = cmdList[shift + 1].trim();
            List<String> participantsList = getAllParticipants(cmd);
            int qtyParticipants = participantsList.size();
            if (qtyParticipants == 0) {
                System.out.println("Group is empty");
            } else {
                double sum = Double.parseDouble(cmdList[shift + 3].trim())
                        / qtyParticipants;
                for (int i = 0; i < qtyParticipants; i++) {
                    String participant = participantsList.get(i);

                    if (i == qtyParticipants - 1) {
                        sum = Double.parseDouble(cmdList[shift + 3].trim()) - new BigDecimal(sum)
                                .setScale(2, RoundingMode.HALF_UP)
                                .doubleValue() * (qtyParticipants - 1);
                    } else {
                        sum = new BigDecimal(sum)
                                .setScale(2, RoundingMode.HALF_UP)
                                .doubleValue();
                    }

                    service.saveTransaction(new Transaction(date,
                            acc_debet,
                            participant,
                            sum));
                }
            }
        } else {
            printCmdError();
        }
    }
}
