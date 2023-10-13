package splitter;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class Transaction {
    private LocalDate date;
    private String acc_debit;
    private String acc_credit;
    private Double sum;

    static boolean isValidP2P(String[] cmdList, String command) {
        return cmdList.length >= 4
                && (cmdList.length != 4 || cmdList[0].equals(command))
                && (cmdList.length != 5 || (cmdList[1].equals(command) || cmdList[0].equals(command)))
                && (cmdList.length != 6 || cmdList[1].equals(command));
    }

    static boolean isValidP2G(String cmd, String command) {
//        String[] cmdParts = cmd.split("\\(");
//        String[] cmdList = cmdParts[0].trim().split("\\s+");
        return true;
    }



    static boolean isValidBalance(String[] cmdList) {
        return (cmdList.length != 1 || cmdList[0].equals("balance"))
                && (cmdList.length != 2 || (cmdList[0].equals("balance") || cmdList[1].equals("balance")))
                && (cmdList.length != 3 || (cmdList[1].equals("balance") && cmdList[2].matches("open|close")));
    }
}
