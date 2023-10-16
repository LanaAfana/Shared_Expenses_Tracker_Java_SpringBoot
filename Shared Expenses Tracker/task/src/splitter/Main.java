package splitter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import splitter.presentation.SharedExpensesOperator;

import java.util.*;

@SpringBootApplication
public class Main implements CommandLineRunner {
    private final SharedExpensesOperator operator;

    @Autowired
    public Main(SharedExpensesOperator operator) {
        this.operator = operator;
    }

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        while (true) {
            try (Scanner scanner = new Scanner(System.in)) {
                String cmd = scanner.nextLine().trim();
                if (cmd.equals("help")) {
                    System.out.println("""
                             balance
                             borrow
                             cashBack
                             exit
                             group
                             help
                             purchase
                             repay
                             secretSanta
                             writeOff""");
                    continue;
                }
                if (cmd.equals("exit")) {
                    break;
                }

                if (cmd.contains("group ")) {
                    if (cmd.contains("create ")) {
                        operator.createGroup(cmd);
                        continue;
                    } else if (cmd.contains("show ")) {
                        operator.showGroup(cmd);
                        continue;
                    } else if (cmd.contains("add ")) {
                        operator.addToGroup(cmd);
                        continue;
                    } else if (cmd.contains("remove ")) {
                        operator.deleteFromGroup(cmd);
                        continue;
                    }
                }

                String[] cmdList = cmd.split("\\s+");
                if (cmd.contains("purchase ")) {
                    operator.doPurchase(cmd);
                    continue;
                }
                if (cmd.contains("balance")) {
                    operator.doBalance(cmdList);
                    continue;
                }
                if (cmd.contains("borrow ")) {
                    operator.doTransaction(cmdList, "borrow");
                    continue;
                }
                if (cmd.contains("repay ")) {
                    operator.doTransaction(cmdList, "repay");
                    continue;
                }
                if (cmd.contains("secretSanta ")) {
                    operator.doSecretSanta(cmdList);
                    continue;
                }
                if (cmd.contains("cashBack ")) {
                    operator.doCashback(cmd);
                    continue;
                }
                if (cmd.contains("writeOff")) {
                    operator.doWriteOff(cmdList);
                    continue;
                }
                System.out.println("Unknown command. Print help to show commands list");
            }
        }
    }
}
